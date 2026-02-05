package com.touhouqing.datasentry.cleaning.pipeline;

import com.touhouqing.datasentry.cleaning.detector.LlmDetector;
import com.touhouqing.datasentry.cleaning.detector.RegexDetector;
import com.touhouqing.datasentry.cleaning.enums.CleaningRuleType;
import com.touhouqing.datasentry.cleaning.model.CleaningAllowlist;
import com.touhouqing.datasentry.cleaning.model.CleaningContext;
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

	private final LlmDetector llmDetector;

	@Override
	public NodeResult process(CleaningContext context) {
		String text = context.getNormalizedText() != null ? context.getNormalizedText() : context.getOriginalText();
		CleaningPolicySnapshot snapshot = context.getPolicySnapshot();
		List<CleaningRule> rules = snapshot != null ? snapshot.getRules() : List.of();
		List<Finding> findings = new ArrayList<>();
		for (CleaningRule rule : rules) {
			if (rule.getRuleType() == null) {
				continue;
			}
			if (CleaningRuleType.REGEX.name().equalsIgnoreCase(rule.getRuleType())) {
				findings.addAll(regexDetector.detect(text, rule));
			}
		}
		if (snapshot != null && snapshot.getConfig() != null && snapshot.getConfig().resolvedLlmEnabled()) {
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

	@SuppressWarnings("unchecked")
	private List<CleaningAllowlist> getAllowlists(CleaningContext context) {
		Object value = context.getMetadata().get("allowlists");
		if (value instanceof List) {
			return (List<CleaningAllowlist>) value;
		}
		return List.of();
	}

}
