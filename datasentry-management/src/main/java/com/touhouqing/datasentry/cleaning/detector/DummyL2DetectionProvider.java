package com.touhouqing.datasentry.cleaning.detector;

import com.touhouqing.datasentry.cleaning.model.CleaningPolicyConfig;
import com.touhouqing.datasentry.cleaning.model.CleaningRule;
import com.touhouqing.datasentry.cleaning.model.Finding;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DummyL2DetectionProvider implements L2DetectionProvider {

	@Override
	public String name() {
		return "DUMMY";
	}

	@Override
	public List<Finding> detect(String text, CleaningRule rule, CleaningPolicyConfig config) {
		if (text == null || text.isBlank()) {
			return List.of();
		}
		double threshold = config != null ? config.resolvedL2Threshold() : 0.6;
		double score = estimateRiskScore(text);
		if (score < threshold) {
			return List.of();
		}
		List<Finding> findings = new ArrayList<>();
		findings.add(Finding.builder()
			.type(rule.getCategory())
			.category(rule.getCategory())
			.severity(score)
			.start(0)
			.end(text.length())
			.detectorSource("L2_DUMMY")
			.build());
		return findings;
	}

	private double estimateRiskScore(String text) {
		String normalized = text.toLowerCase();
		double score = 0.35;
		if (normalized.contains("http") || normalized.contains("www.")) {
			score += 0.15;
		}
		if (normalized.contains("wx") || normalized.contains("telegram") || normalized.contains("qq")) {
			score += 0.2;
		}
		if (text.length() > 120) {
			score += 0.1;
		}
		if (normalized.contains("转账") || normalized.contains("兼职") || normalized.contains("点击")) {
			score += 0.2;
		}
		return Math.min(score, 0.95);
	}

}
