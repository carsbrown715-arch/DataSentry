package com.touhouqing.datasentry.cleaning.detector;

import com.touhouqing.datasentry.cleaning.model.CleaningPolicyConfig;
import com.touhouqing.datasentry.cleaning.model.CleaningRule;
import com.touhouqing.datasentry.cleaning.model.Finding;

import java.util.List;

public interface L2DetectionProvider {

	String name();

	default boolean isReady() {
		return true;
	}

	List<Finding> detect(String text, CleaningRule rule, CleaningPolicyConfig config);

}
