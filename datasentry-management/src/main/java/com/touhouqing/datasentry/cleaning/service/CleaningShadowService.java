package com.touhouqing.datasentry.cleaning.service;

import com.touhouqing.datasentry.cleaning.mapper.CleaningShadowCompareRecordMapper;
import com.touhouqing.datasentry.cleaning.model.CleaningContext;
import com.touhouqing.datasentry.cleaning.model.CleaningPolicyConfig;
import com.touhouqing.datasentry.cleaning.model.CleaningPolicySnapshot;
import com.touhouqing.datasentry.cleaning.model.CleaningShadowCompareRecord;
import com.touhouqing.datasentry.cleaning.model.Finding;
import com.touhouqing.datasentry.properties.DataSentryProperties;
import com.touhouqing.datasentry.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

@Slf4j
@Service
public class CleaningShadowService {

	private final Executor shadowExecutor;

	private final CleaningShadowCompareRecordMapper shadowCompareRecordMapper;

	private final DataSentryProperties dataSentryProperties;

	public CleaningShadowService(@Qualifier("cleaningShadowExecutor") Executor shadowExecutor,
			CleaningShadowCompareRecordMapper shadowCompareRecordMapper, DataSentryProperties dataSentryProperties) {
		this.shadowExecutor = shadowExecutor;
		this.shadowCompareRecordMapper = shadowCompareRecordMapper;
		this.dataSentryProperties = dataSentryProperties;
	}

	public void submitIfEnabled(CleaningContext context, CleaningPolicyConfig config) {
		if (!shouldRunShadow(context, config, true)) {
			return;
		}
		try {
			shadowExecutor
				.execute(() -> log.info("Shadow record traceId={} verdict={} findings={}", context.getTraceId(),
						context.getVerdict(), context.getFindings() != null ? context.getFindings().size() : 0));
		}
		catch (Exception e) {
			log.debug("Discard shadow task traceId={}", context.getTraceId(), e);
		}
	}

	public ShadowCompareOutcome compareAndRecordIfEnabled(CleaningContext mainResult, CleaningPolicySnapshot snapshot,
			Supplier<CleaningContext> shadowSupplier) {
		if (!dataSentryProperties.getCleaning().isShadowDualTrackEnabled()) {
			return ShadowCompareOutcome.none();
		}
		if (mainResult == null || snapshot == null || shadowSupplier == null) {
			return ShadowCompareOutcome.none();
		}
		if (!shouldRunShadow(mainResult, snapshot.getConfig(), true)) {
			return ShadowCompareOutcome.none();
		}
		try {
			CleaningContext shadowResult = shadowSupplier.get();
			if (shadowResult == null) {
				return ShadowCompareOutcome.none();
			}
			String mainVerdict = mainResult.getVerdict() != null ? mainResult.getVerdict().name() : null;
			String shadowVerdict = shadowResult.getVerdict() != null ? shadowResult.getVerdict().name() : null;
			List<String> mainCategories = resolveCategories(mainResult.getFindings());
			List<String> shadowCategories = resolveCategories(shadowResult.getFindings());
			String diffLevel = resolveDiffLevel(mainVerdict, shadowVerdict, mainCategories, shadowCategories,
					mainResult.getSanitizedText(), shadowResult.getSanitizedText());
			String diffJson = buildDiffJson(mainVerdict, shadowVerdict, mainCategories, shadowCategories,
					mainResult.getSanitizedText(), shadowResult.getSanitizedText());
			shadowCompareRecordMapper.insert(CleaningShadowCompareRecord.builder()
				.traceId(mainResult.getTraceId())
				.jobRunId(mainResult.getJobRunId())
				.policyId(snapshot.getPolicyId())
				.policyVersionId(snapshot.getPolicyVersionId())
				.columnName(mainResult.getColumnName())
				.mainVerdict(mainVerdict)
				.shadowVerdict(shadowVerdict)
				.diffLevel(diffLevel)
				.diffJson(diffJson)
				.createdTime(LocalDateTime.now())
				.build());
			return new ShadowCompareOutcome(true, diffLevel, diffJson);
		}
		catch (Exception e) {
			log.warn("Failed to compare shadow result traceId={}", mainResult.getTraceId(), e);
			return ShadowCompareOutcome.none();
		}
	}

	private boolean shouldRunShadow(CleaningContext context, CleaningPolicyConfig config, boolean withSampling) {
		if (context == null || config == null || !config.resolvedShadowEnabled()) {
			return false;
		}
		Object globalShadow = context.getMetadata().get("shadowEnabled");
		if (globalShadow instanceof Boolean && !((Boolean) globalShadow)) {
			return false;
		}
		if (!withSampling) {
			return true;
		}
		double ratio = config.resolvedShadowSampleRatio();
		return ratio > 0 && Math.random() <= ratio;
	}

	private List<String> resolveCategories(List<Finding> findings) {
		if (findings == null || findings.isEmpty()) {
			return List.of();
		}
		Set<String> categories = new LinkedHashSet<>();
		for (Finding finding : findings) {
			if (finding != null && finding.getCategory() != null && !finding.getCategory().isBlank()) {
				categories.add(finding.getCategory());
			}
		}
		return categories.stream().toList();
	}

	private String resolveDiffLevel(String mainVerdict, String shadowVerdict, List<String> mainCategories,
			List<String> shadowCategories, String mainSanitizedText, String shadowSanitizedText) {
		boolean verdictSame = Objects.equals(mainVerdict, shadowVerdict);
		boolean categoriesSame = Objects.equals(mainCategories, shadowCategories);
		boolean sanitizeSame = Objects.equals(mainSanitizedText, shadowSanitizedText);
		if (verdictSame && categoriesSame && sanitizeSame) {
			return "NONE";
		}
		if (verdictSame) {
			return "LOW";
		}
		return "HIGH";
	}

	private String buildDiffJson(String mainVerdict, String shadowVerdict, List<String> mainCategories,
			List<String> shadowCategories, String mainSanitizedText, String shadowSanitizedText) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("mainVerdict", mainVerdict);
		payload.put("shadowVerdict", shadowVerdict);
		payload.put("mainCategories", mainCategories);
		payload.put("shadowCategories", shadowCategories);
		payload.put("sanitizedDifferent", !Objects.equals(mainSanitizedText, shadowSanitizedText));
		try {
			return JsonUtil.getObjectMapper().writeValueAsString(payload);
		}
		catch (Exception e) {
			return null;
		}
	}

	public record ShadowCompareOutcome(boolean compared, String diffLevel, String diffJson) {

		public static ShadowCompareOutcome none() {
			return new ShadowCompareOutcome(false, null, null);
		}

	}

}
