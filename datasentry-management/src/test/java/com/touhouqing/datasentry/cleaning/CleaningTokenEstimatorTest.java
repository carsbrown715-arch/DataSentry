package com.touhouqing.datasentry.cleaning;

import com.touhouqing.datasentry.cleaning.service.CleaningTokenEstimator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CleaningTokenEstimatorTest {

	private final CleaningTokenEstimator estimator = new CleaningTokenEstimator();

	@Test
	public void shouldReturnZeroForBlankText() {
		assertEquals(0L, estimator.estimateTokens(""));
		assertEquals(0L, estimator.estimateTokens("   "));
		assertEquals(0L, estimator.estimateTokens(null));
	}

	@Test
	public void shouldEstimateByCharsDividedByFour() {
		assertEquals(1L, estimator.estimateTokens("abcd"));
		assertEquals(2L, estimator.estimateTokens("abcde"));
		assertEquals(3L, estimator.estimateTokens("123456789"));
	}

}
