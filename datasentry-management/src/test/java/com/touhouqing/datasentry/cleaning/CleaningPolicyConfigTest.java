package com.touhouqing.datasentry.cleaning;

import com.touhouqing.datasentry.cleaning.model.CleaningPolicyConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CleaningPolicyConfigTest {

	@Test
	public void shouldReturnDefaultL2AndShadowValues() {
		CleaningPolicyConfig config = new CleaningPolicyConfig();

		assertEquals(0.6, config.resolvedL2Threshold());
		assertFalse(config.resolvedShadowEnabled());
		assertEquals(0.0, config.resolvedShadowSampleRatio());
	}

	@Test
	public void shouldClampShadowSampleRatio() {
		CleaningPolicyConfig config = CleaningPolicyConfig.builder().shadowEnabled(true).shadowSampleRatio(1.5).build();

		assertTrue(config.resolvedShadowEnabled());
		assertEquals(1.0, config.resolvedShadowSampleRatio());
	}

}
