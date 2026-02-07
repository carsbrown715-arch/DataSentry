package com.touhouqing.datasentry.cleaning.detector;

import com.touhouqing.datasentry.cleaning.model.CleaningPolicyConfig;
import com.touhouqing.datasentry.cleaning.model.CleaningRule;
import com.touhouqing.datasentry.cleaning.model.Finding;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class L2Detector {

	private final L2DetectionProviderRouter providerRouter;

	public List<Finding> detect(String text, CleaningRule rule, CleaningPolicyConfig config) {
		return providerRouter.detect(text, rule, config);
	}

}
