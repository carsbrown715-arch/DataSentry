package com.touhouqing.datasentry.cleaning.service;

import org.springframework.stereotype.Component;

@Component
public class CleaningTokenEstimator {

	public long estimateTokens(String text) {
		if (text == null || text.isBlank()) {
			return 0L;
		}
		long chars = text.length();
		return (long) Math.ceil(chars / 4.0d);
	}

}
