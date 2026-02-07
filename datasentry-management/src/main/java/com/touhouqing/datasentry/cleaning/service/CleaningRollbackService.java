package com.touhouqing.datasentry.cleaning.service;

import com.touhouqing.datasentry.cleaning.enums.CleaningRollbackStatus;
import com.touhouqing.datasentry.cleaning.mapper.CleaningBackupRecordMapper;
import com.touhouqing.datasentry.cleaning.mapper.CleaningJobMapper;
import com.touhouqing.datasentry.cleaning.mapper.CleaningJobRunMapper;
import com.touhouqing.datasentry.cleaning.mapper.CleaningRollbackRunMapper;
import com.touhouqing.datasentry.cleaning.model.CleaningBackupRecord;
import com.touhouqing.datasentry.cleaning.model.CleaningJob;
import com.touhouqing.datasentry.cleaning.model.CleaningJobRun;
import com.touhouqing.datasentry.cleaning.model.CleaningRollbackRun;
import com.touhouqing.datasentry.connector.pool.DBConnectionPool;
import com.touhouqing.datasentry.connector.pool.DBConnectionPoolFactory;
import com.touhouqing.datasentry.entity.Datasource;
import com.touhouqing.datasentry.exception.InvalidInputException;
import com.touhouqing.datasentry.service.datasource.DatasourceService;
import com.touhouqing.datasentry.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CleaningRollbackService {

	private static final int BATCH_SIZE = 200;

	private final CleaningRollbackRunMapper rollbackRunMapper;

	private final CleaningBackupRecordMapper backupRecordMapper;

	private final CleaningJobRunMapper jobRunMapper;

	private final CleaningJobMapper jobMapper;

	private final DatasourceService datasourceService;

	private final DBConnectionPoolFactory connectionPoolFactory;

	private final CleaningBackupEncryptionService encryptionService;

	public CleaningRollbackRun createRollbackRun(Long runId) {
		CleaningJobRun jobRun = jobRunMapper.selectById(runId);
		if (jobRun == null) {
			throw new InvalidInputException("Job run not found");
		}
		if (encryptionService.isEncryptionEnabled() && !encryptionService.hasValidKey()) {
			throw new InvalidInputException(encryptionService.missingKeyHint());
		}
		LocalDateTime now = LocalDateTime.now();
		CleaningRollbackRun rollbackRun = CleaningRollbackRun.builder()
			.jobRunId(runId)
			.status(CleaningRollbackStatus.READY.name())
			.totalTarget(0L)
			.totalSuccess(0L)
			.totalFailed(0L)
			.createdTime(now)
			.updatedTime(now)
			.build();
		rollbackRunMapper.insert(rollbackRun);
		return rollbackRun;
	}

	public CleaningRollbackRun getRollbackRun(Long rollbackRunId) {
		return rollbackRunMapper.selectById(rollbackRunId);
	}

	public void processRun(CleaningRollbackRun run) {
		if (run == null) {
			return;
		}
		CleaningJobRun jobRun = jobRunMapper.selectById(run.getJobRunId());
		if (jobRun == null) {
			failRun(run.getId(), "Job run not found");
			return;
		}
		CleaningJob job = jobMapper.selectById(jobRun.getJobId());
		if (job == null) {
			failRun(run.getId(), "Job not found");
			return;
		}
		Datasource datasource = datasourceService.getDatasourceById(job.getDatasourceId());
		if (datasource == null) {
			failRun(run.getId(), "Datasource not found");
			return;
		}
		DBConnectionPool pool = connectionPoolFactory.getPoolByDbType(datasource.getType());
		Long checkpointId = run.getCheckpointId();
		Long totalTarget = defaultLong(run.getTotalTarget());
		Long totalSuccess = defaultLong(run.getTotalSuccess());
		Long totalFailed = defaultLong(run.getTotalFailed());
		try (Connection connection = pool.getConnection(datasourceService.getDbConfig(datasource))) {
			while (true) {
				List<CleaningBackupRecord> records = backupRecordMapper.findByRunAfterId(run.getJobRunId(),
						checkpointId, BATCH_SIZE);
				if (records.isEmpty()) {
					rollbackRunMapper.updateStatus(run.getId(), CleaningRollbackStatus.SUCCEEDED.name(),
							LocalDateTime.now(), LocalDateTime.now());
					return;
				}
				for (CleaningBackupRecord record : records) {
					totalTarget++;
					boolean ok = restoreRecord(connection, record);
					if (ok) {
						totalSuccess++;
					}
					else {
						totalFailed++;
					}
					checkpointId = record.getId();
				}
				rollbackRunMapper.updateProgress(run.getId(), checkpointId, totalTarget, totalSuccess, totalFailed,
						LocalDateTime.now());
			}
		}
		catch (Exception e) {
			log.warn("Failed to process rollback run {}", run.getId(), e);
			rollbackRunMapper.updateStatus(run.getId(), CleaningRollbackStatus.FAILED.name(), LocalDateTime.now(),
					LocalDateTime.now());
		}
	}

	private boolean restoreRecord(Connection connection, CleaningBackupRecord record) {
		try {
			Map<String, Object> beforeRow = loadBeforeRow(record);
			if (beforeRow.isEmpty()) {
				return false;
			}
			PkRef pkRef = resolvePk(record.getPkJson());
			if (pkRef == null) {
				return false;
			}
			executeUpdate(connection, record.getTableName(), beforeRow, pkRef);
			return true;
		}
		catch (Exception e) {
			log.warn("Failed to restore backup record {}", record.getId(), e);
			return false;
		}
	}

	private Map<String, Object> loadBeforeRow(CleaningBackupRecord record) {
		String json = record.getBeforeRowJson();
		if (json == null || json.isBlank()) {
			if (record.getBeforeRowCiphertext() == null || record.getBeforeRowCiphertext().isBlank()) {
				return new LinkedHashMap<>();
			}
			json = encryptionService.decrypt(record.getBeforeRowCiphertext());
		}
		return parseJsonMap(json);
	}

	private void executeUpdate(Connection connection, String tableName, Map<String, Object> updateValues, PkRef pkRef)
			throws Exception {
		String setClause = updateValues.keySet().stream().map(col -> col + " = ?").collect(Collectors.joining(", "));
		String sql = "UPDATE " + tableName + " SET " + setClause + " WHERE " + pkRef.column() + " = ?";
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			int index = 1;
			for (Object value : updateValues.values()) {
				statement.setObject(index++, value);
			}
			statement.setObject(index, pkRef.value());
			statement.executeUpdate();
		}
	}

	private Map<String, Object> parseJsonMap(String json) {
		if (json == null || json.isBlank()) {
			return new LinkedHashMap<>();
		}
		try {
			Map<String, Object> parsed = JsonUtil.getObjectMapper().readValue(json, Map.class);
			return parsed != null ? parsed : new LinkedHashMap<>();
		}
		catch (Exception e) {
			return new LinkedHashMap<>();
		}
	}

	private PkRef resolvePk(String pkJson) {
		Map<String, Object> pkMap = parseJsonMap(pkJson);
		if (pkMap.isEmpty()) {
			return null;
		}
		Map.Entry<String, Object> entry = pkMap.entrySet().iterator().next();
		return new PkRef(entry.getKey(), entry.getValue());
	}

	private void failRun(Long runId, String reason) {
		log.warn("Cleaning rollback run {} failed: {}", runId, reason);
		rollbackRunMapper.updateStatus(runId, CleaningRollbackStatus.FAILED.name(), LocalDateTime.now(),
				LocalDateTime.now());
	}

	private Long defaultLong(Long value) {
		return value != null ? value : 0L;
	}

	private record PkRef(String column, Object value) {
	}

}
