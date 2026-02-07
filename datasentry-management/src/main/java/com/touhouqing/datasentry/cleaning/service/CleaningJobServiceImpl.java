package com.touhouqing.datasentry.cleaning.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.touhouqing.datasentry.cleaning.dto.CleaningBudgetView;
import com.touhouqing.datasentry.cleaning.dto.CleaningJobCreateRequest;
import com.touhouqing.datasentry.cleaning.enums.CleaningJobMode;
import com.touhouqing.datasentry.cleaning.enums.CleaningJobRunStatus;
import com.touhouqing.datasentry.cleaning.enums.CleaningReviewPolicy;
import com.touhouqing.datasentry.cleaning.enums.CleaningWritebackMode;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.touhouqing.datasentry.cleaning.model.CleaningCostLedger;
import com.touhouqing.datasentry.cleaning.mapper.CleaningJobMapper;
import com.touhouqing.datasentry.cleaning.mapper.CleaningJobRunMapper;
import com.touhouqing.datasentry.cleaning.model.CleaningJob;
import com.touhouqing.datasentry.cleaning.model.CleaningJobRun;
import com.touhouqing.datasentry.exception.InvalidInputException;
import com.touhouqing.datasentry.properties.DataSentryProperties;
import com.touhouqing.datasentry.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CleaningJobServiceImpl implements CleaningJobService {

	private final CleaningJobMapper jobMapper;

	private final CleaningJobRunMapper jobRunMapper;

	private final CleaningPolicyResolver policyResolver;

	private final CleaningTargetConfigValidator targetConfigValidator;

	private final CleaningCostLedgerService costLedgerService;

	private final DataSentryProperties dataSentryProperties;

	@Override
	public CleaningJob createJob(CleaningJobCreateRequest request) {
		String mode = resolveEnum(request.getMode(), CleaningJobMode.DRY_RUN.name(), CleaningJobMode.class);
		String writebackMode = resolveEnum(request.getWritebackMode(), CleaningWritebackMode.NONE.name(),
				CleaningWritebackMode.class);
		String reviewPolicy = resolveEnum(request.getReviewPolicy(), CleaningReviewPolicy.NEVER.name(),
				CleaningReviewPolicy.class);
		String targetConfigType = targetConfigValidator.resolveType(request.getTargetConfigType());
		java.util.Map<String, String> normalizedJsonPathMappings = targetConfigValidator
			.normalizeJsonPathMappings(targetConfigType, request.getTargetColumns(), request.getTargetConfig());
		LocalDateTime now = LocalDateTime.now();
		CleaningJob job = CleaningJob.builder()
			.agentId(request.getAgentId())
			.datasourceId(request.getDatasourceId())
			.tableName(request.getTableName())
			.targetConfigType(targetConfigType)
			.targetConfigJson(toJson(
					normalizedJsonPathMappings.isEmpty() ? request.getTargetConfig() : normalizedJsonPathMappings))
			.pkColumnsJson(toJson(request.getPkColumns()))
			.targetColumnsJson(toJson(request.getTargetColumns()))
			.whereSql(request.getWhereSql())
			.policyId(request.getPolicyId())
			.mode(mode)
			.writebackMode(writebackMode)
			.reviewPolicy(reviewPolicy)
			.writebackMappingJson(toJson(request.getWritebackMapping()))
			.batchSize(resolveBatchSize(request.getBatchSize()))
			.budgetEnabled(resolveBudgetEnabled(request.getBudgetEnabled()))
			.budgetSoftLimit(resolveSoftLimit(request.getBudgetSoftLimit()))
			.budgetHardLimit(resolveHardLimit(request.getBudgetHardLimit()))
			.budgetCurrency(resolveBudgetCurrency(request.getBudgetCurrency()))
			.onlineFailClosedEnabled(resolveFailClosedEnabled(request.getOnlineFailClosedEnabled()))
			.onlineRequestTokenLimit(resolveOnlineTokenLimit(request.getOnlineRequestTokenLimit()))
			.enabled(request.getEnabled() != null ? request.getEnabled() : 1)
			.createdTime(now)
			.updatedTime(now)
			.build();
		jobMapper.insert(job);
		return job;
	}

	@Override
	public CleaningJob getJob(Long jobId) {
		return jobMapper.selectById(jobId);
	}

	@Override
	public java.util.List<CleaningJob> listJobs(Long agentId, Long datasourceId, Integer enabled) {
		LambdaQueryWrapper<CleaningJob> wrapper = new LambdaQueryWrapper<>();
		if (agentId != null) {
			wrapper.eq(CleaningJob::getAgentId, agentId);
		}
		if (datasourceId != null) {
			wrapper.eq(CleaningJob::getDatasourceId, datasourceId);
		}
		if (enabled != null) {
			wrapper.eq(CleaningJob::getEnabled, enabled);
		}
		wrapper.orderByDesc(CleaningJob::getId);
		return jobMapper.selectList(wrapper);
	}

	@Override
	public CleaningJobRun createRun(Long jobId) {
		CleaningJob job = jobMapper.selectById(jobId);
		if (job == null) {
			throw new InvalidInputException("清理任务不存在");
		}
		String policySnapshotJson = toJson(policyResolver.resolveSnapshot(job.getPolicyId()));
		LocalDateTime now = LocalDateTime.now();
		CleaningJobRun run = CleaningJobRun.builder()
			.jobId(jobId)
			.status(CleaningJobRunStatus.QUEUED.name())
			.attempt(0)
			.policySnapshotJson(policySnapshotJson)
			.totalScanned(0L)
			.totalFlagged(0L)
			.totalWritten(0L)
			.totalFailed(0L)
			.estimatedCost(BigDecimal.ZERO)
			.actualCost(BigDecimal.ZERO)
			.budgetStatus("NORMAL")
			.budgetMessage(null)
			.createdTime(now)
			.updatedTime(now)
			.build();
		jobRunMapper.insert(run);
		return run;
	}

	@Override
	public CleaningJobRun getRun(Long runId) {
		return jobRunMapper.selectById(runId);
	}

	@Override
	public java.util.List<CleaningJobRun> listRuns(Long jobId, String status) {
		LambdaQueryWrapper<CleaningJobRun> wrapper = new LambdaQueryWrapper<>();
		if (jobId != null) {
			wrapper.eq(CleaningJobRun::getJobId, jobId);
		}
		if (status != null && !status.isBlank()) {
			wrapper.eq(CleaningJobRun::getStatus, status);
		}
		wrapper.orderByDesc(CleaningJobRun::getId);
		return jobRunMapper.selectList(wrapper);
	}

	@Override
	public CleaningJobRun pauseRun(Long runId) {
		jobRunMapper.updateStatusWithoutEnd(runId, CleaningJobRunStatus.PAUSED.name(), LocalDateTime.now());
		return jobRunMapper.selectById(runId);
	}

	@Override
	public CleaningJobRun resumeRun(Long runId) {
		CleaningJobRun existing = jobRunMapper.selectById(runId);
		if (existing == null) {
			throw new InvalidInputException("清理任务运行实例不存在");
		}
		if ("HARD_EXCEEDED".equals(existing.getBudgetStatus())) {
			throw new InvalidInputException("预算已超硬阈值，请调整预算后再恢复");
		}
		jobRunMapper.updateStatusWithoutEnd(runId, CleaningJobRunStatus.QUEUED.name(), LocalDateTime.now());
		return jobRunMapper.selectById(runId);
	}

	@Override
	public CleaningJobRun cancelRun(Long runId) {
		LocalDateTime now = LocalDateTime.now();
		jobRunMapper.updateStatus(runId, CleaningJobRunStatus.CANCELED.name(), now, now);
		return jobRunMapper.selectById(runId);
	}

	@Override
	public CleaningBudgetView getBudget(Long runId) {
		CleaningJobRun run = jobRunMapper.selectById(runId);
		if (run == null) {
			throw new InvalidInputException("清理任务运行实例不存在");
		}
		CleaningJob job = jobMapper.selectById(run.getJobId());
		if (job == null) {
			throw new InvalidInputException("清理任务不存在");
		}
		return CleaningBudgetView.builder()
			.runId(run.getId())
			.jobId(job.getId())
			.budgetEnabled(job.getBudgetEnabled())
			.budgetSoftLimit(job.getBudgetSoftLimit())
			.budgetHardLimit(job.getBudgetHardLimit())
			.budgetCurrency(job.getBudgetCurrency())
			.estimatedCost(run.getEstimatedCost())
			.actualCost(run.getActualCost())
			.budgetStatus(run.getBudgetStatus())
			.budgetMessage(run.getBudgetMessage())
			.build();
	}

	@Override
	public java.util.List<CleaningCostLedger> listCostLedger(Long jobRunId, String traceId, String channel) {
		return costLedgerService.list(jobRunId, traceId, channel);
	}

	private String resolveEnum(String value, String defaultValue, Class<? extends Enum<?>> enumType) {
		if (value == null || value.isBlank()) {
			return defaultValue;
		}
		for (Enum<?> enumValue : enumType.getEnumConstants()) {
			if (enumValue.name().equalsIgnoreCase(value)) {
				return enumValue.name();
			}
		}
		StringBuilder supportedValues = new StringBuilder();
		for (Enum<?> enumValue : enumType.getEnumConstants()) {
			if (supportedValues.length() > 0) {
				supportedValues.append(", ");
			}
			supportedValues.append(enumValue.name());
		}
		throw new InvalidInputException("枚举值非法：" + value + "，支持值：" + supportedValues);
	}

	private Integer resolveBatchSize(Integer batchSize) {
		if (batchSize != null && batchSize > 0) {
			return batchSize;
		}
		return dataSentryProperties.getCleaning().getBatch().getDefaultBatchSize();
	}

	private Integer resolveBudgetEnabled(Integer value) {
		if (value != null) {
			return value;
		}
		return 1;
	}

	private BigDecimal resolveSoftLimit(BigDecimal value) {
		if (value != null && value.signum() > 0) {
			return value;
		}
		return BigDecimal.valueOf(dataSentryProperties.getCleaning().getBudget().getDefaultSoftLimit());
	}

	private BigDecimal resolveHardLimit(BigDecimal value) {
		if (value != null && value.signum() > 0) {
			return value;
		}
		return BigDecimal.valueOf(dataSentryProperties.getCleaning().getBudget().getDefaultHardLimit());
	}

	private String resolveBudgetCurrency(String value) {
		if (value != null && !value.isBlank()) {
			return value;
		}
		return dataSentryProperties.getCleaning().getBudget().getDefaultCurrency();
	}

	private Integer resolveFailClosedEnabled(Integer value) {
		if (value != null) {
			return value;
		}
		return dataSentryProperties.getCleaning().getBudget().isFailClosedEnabled() ? 1 : 0;
	}

	private Integer resolveOnlineTokenLimit(Integer value) {
		if (value != null && value > 0) {
			return value;
		}
		return dataSentryProperties.getCleaning().getBudget().getOnlineRequestTokenLimit();
	}

	private String toJson(Object value) {
		if (value == null) {
			return null;
		}
		try {
			return JsonUtil.getObjectMapper().writeValueAsString(value);
		}
		catch (JsonProcessingException e) {
			throw new InvalidInputException("Invalid json payload");
		}
	}

}
