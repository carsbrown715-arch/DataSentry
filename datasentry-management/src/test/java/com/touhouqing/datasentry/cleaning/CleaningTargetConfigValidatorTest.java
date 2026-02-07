package com.touhouqing.datasentry.cleaning;

import com.touhouqing.datasentry.cleaning.service.CleaningTargetConfigValidator;
import com.touhouqing.datasentry.exception.InvalidInputException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CleaningTargetConfigValidatorTest {

	private final CleaningTargetConfigValidator validator = new CleaningTargetConfigValidator();

	@Test
	public void shouldResolveColumnsAsDefault() {
		assertEquals("COLUMNS", validator.resolveType(null));
	}

	@Test
	public void shouldValidateJsonPathMappings() {
		Map<String, String> result = validator.normalizeJsonPathMappings("JSONPATH", List.of("content"),
				Map.of("content", "$.profile.desc"));

		assertEquals("$.profile.desc", result.get("content"));
	}

	@Test
	public void shouldThrowWhenJsonPathMissing() {
		assertThrows(InvalidInputException.class,
				() -> validator.normalizeJsonPathMappings("JSONPATH", List.of("content"), Map.of()));
	}

}
