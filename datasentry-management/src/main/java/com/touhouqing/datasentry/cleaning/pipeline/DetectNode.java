package com.touhouqing.datasentry.cleaning.pipeline;

import com.touhouqing.datasentry.cleaning.detector.LlmDetector;
import com.touhouqing.datasentry.cleaning.detector.L2Detector;
import com.touhouqing.datasentry.cleaning.detector.RegexDetector;
import com.touhouqing.datasentry.cleaning.enums.CleaningRuleType;
import com.touhouqing.datasentry.cleaning.model.CleaningAllowlist;
import com.touhouqing.datasentry.cleaning.model.CleaningContext;
import com.touhouqing.datasentry.cleaning.model.CleaningPolicyConfig;
import com.touhouqing.datasentry.cleaning.model.CleaningPolicySnapshot;
import com.touhouqing.datasentry.cleaning.model.CleaningRule;
import com.touhouqing.datasentry.cleaning.model.Finding;
import com.touhouqing.datasentry.cleaning.model.NodeResult;
import com.touhouqing.datasentry.cleaning.util.CleaningAllowlistMatcher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DetectNode implements PipelineNode {

	private final RegexDetector regexDetector;

	private final L2Detector l2Detector;

	private final LlmDetector llmDetector;

	@Override
	public NodeResult process(CleaningContext context) {
		String text = context.getNormalizedText() != null ? context.getNormalizedText() : context.getOriginalText();
		CleaningPolicySnapshot snapshot = context.getPolicySnapshot();
		CleaningPolicyConfig policyConfig = snapshot != null && snapshot.getConfig() != null ? snapshot.getConfig()
				: new CleaningPolicyConfig();
		boolean disableL3 = isDisableL3(context);
		List<CleaningRule> rules = snapshot != null ? snapshot.getRules() : List.of();
		List<Finding> l1Findings = new ArrayList<>();
		List<Finding> l2Findings = new ArrayList<>();
		List<Finding> findings = new ArrayList<>();
		for (CleaningRule rule : rules) {
			if (rule.getRuleType() == null) {
				continue;
			}
			if (CleaningRuleType.REGEX.name().equalsIgnoreCase(rule.getRuleType())) {
				l1Findings.addAll(regexDetector.detect(text, rule));
			}
			if (CleaningRuleType.L2_DUMMY.name().equalsIgnoreCase(rule.getRuleType())) {
				l2Findings.addAll(l2Detector.detect(text, rule, policyConfig));
			}
		}
		findings.addAll(l1Findings);
		findings.addAll(l2Findings);
		if (!disableL3 && snapshot != null && snapshot.getConfig() != null && snapshot.getConfig().resolvedLlmEnabled()
				&& shouldEscalateToL3(l1Findings, l2Findings, policyConfig)) {
			boolean hasLlmRule = rules.stream()
				.anyMatch(rule -> CleaningRuleType.LLM.name().equalsIgnoreCase(rule.getRuleType()));
			if (hasLlmRule) {
				findings.addAll(llmDetector.detect(text));
			}
		}
		List<CleaningAllowlist> allowlists = getAllowlists(context);
		context.setFindings(CleaningAllowlistMatcher.filterFindings(text, findings, allowlists));
		return NodeResult.ok();
	}

	private boolean shouldEscalateToL3(List<Finding> l1Findings, List<Finding> l2Findings,
			CleaningPolicyConfig config) {
		if (!l1Findings.isEmpty()) {
			return true;
		}
		if (l2Findings.isEmpty()) {
			return false;
		}
		double reviewThreshold = config != null ? config.resolvedReviewThreshold() : 0.4;
		for (Finding finding : l2Findings) {
			double severity = finding.getSeverity() != null ? finding.getSeverity() : 0.0;
			if (severity >= reviewThreshold) {
				return true;
			}
		}
		return false;
	}

	private boolean isDisableL3(CleaningContext context) {
		Object value = context.getMetadata().get("disableL3");
		return value instanceof Boolean && (Boolean) value;
	}

	@SuppressWarnings("unchecked")
	private List<CleaningAllowlist> getAllowlists(CleaningContext context) {
		Object value = context.getMetadata().get("allowlists");
		if (value instanceof List) {
			return (List<CleaningAllowlist>) value;
		}
		return List.of();
	}

}
