package com.touhouqing.datasentry.cleaning.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.touhouqing.datasentry.cleaning.mapper.CleaningDlqMapper;
import com.touhouqing.datasentry.cleaning.model.CleaningDlqRecord;
import com.touhouqing.datasentry.properties.DataSentryProperties;
import com.touhouqing.datasentry.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CleaningDlqService {

	private static final int MAX_RETRY = 3;

	private final CleaningDlqMapper dlqMapper;

	private final DataSentryProperties dataSentryProperties;

	private final CleaningNotificationService notificationService;

	public void push(Long jobId, Long runId, Long datasourceId, String tableName, String pkJson, Object payload,
			Exception error) {
		CleaningDlqRecord record = CleaningDlqRecord.builder()
			.jobId(jobId)
			.jobRunId(runId)
			.datasourceId(datasourceId)
			.tableName(tableName)
			.pkJson(pkJson)
			.payloadJson(toJson(payload))
			.errorMessage(error != null ? error.getMessage() : "unknown")
			.retryCount(0)
			.nextRetryTime(LocalDateTime.now())
			.status("READY")
			.createdTime(LocalDateTime.now())
			.updatedTime(LocalDateTime.now())
			.build();
		dlqMapper.insert(record);
		Long readyCount = dlqMapper
			.selectCount(new LambdaQueryWrapper<CleaningDlqRecord>().eq(CleaningDlqRecord::getStatus, "READY"));
		long threshold = dataSentryProperties.getCleaning().getNotification().getDlqBacklogThreshold();
		if (readyCount != null && threshold > 0 && readyCount >= threshold && readyCount % threshold == 0) {
			notificationService.notifyAsync("WARN", "DLQ_BACKLOG", "DLQ 待处理积压超过阈值：" + readyCount,
					Map.of("readyDlq", readyCount, "threshold", threshold));
		}
	}

	public List<CleaningDlqRecord> list(String status, Long jobRunId) {
		LambdaQueryWrapper<CleaningDlqRecord> wrapper = new LambdaQueryWrapper<>();
		if (status != null && !status.isBlank()) {
			wrapper.eq(CleaningDlqRecord::getStatus, status);
		}
		if (jobRunId != null) {
			wrapper.eq(CleaningDlqRecord::getJobRunId, jobRunId);
		}
		return dlqMapper.selectList(wrapper.orderByDesc(CleaningDlqRecord::getId));
	}

	public void retryBatch() {
		List<CleaningDlqRecord> records = dlqMapper.findRunnable(LocalDateTime.now(), 50);
		for (CleaningDlqRecord record : records) {
			try {
				if (record.getRetryCount() != null && record.getRetryCount() >= MAX_RETRY) {
					dlqMapper.markDead(record.getId());
					continue;
				}
				log.info("Replaying DLQ id={} jobRun={} table={} pk={}", record.getId(), record.getJobRunId(),
						record.getTableName(), record.getPkJson());
				dlqMapper.markDone(record.getId());
			}
			catch (Exception e) {
				int nextRetry = record.getRetryCount() != null ? record.getRetryCount() + 1 : 1;
				if (nextRetry >= MAX_RETRY) {
					dlqMapper.markDead(record.getId());
				}
				else {
					dlqMapper.increaseRetry(record.getId(), nextRetry, LocalDateTime.now().plusMinutes(nextRetry * 5L));
				}
			}
		}
	}

	public void retryOne(Long id) {
		CleaningDlqRecord record = dlqMapper.selectById(id);
		if (record == null) {
			return;
		}
		dlqMapper.increaseRetry(id, record.getRetryCount() != null ? record.getRetryCount() + 1 : 1,
				LocalDateTime.now());
	}

	private String toJson(Object value) {
		if (value == null) {
			return null;
		}
		try {
			return JsonUtil.getObjectMapper().writeValueAsString(value);
		}
		catch (Exception e) {
			return toJson(Map.of("value", String.valueOf(value)));
		}
	}

}
