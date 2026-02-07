package com.touhouqing.datasentry.cleaning;

import com.touhouqing.datasentry.cleaning.dto.CleaningRuleRequest;
import com.touhouqing.datasentry.cleaning.mapper.CleaningPolicyMapper;
import com.touhouqing.datasentry.cleaning.mapper.CleaningPolicyRuleMapper;
import com.touhouqing.datasentry.cleaning.mapper.CleaningRuleMapper;
import com.touhouqing.datasentry.cleaning.model.CleaningRule;
import com.touhouqing.datasentry.cleaning.service.CleaningPolicyService;
import com.touhouqing.datasentry.exception.InvalidInputException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class CleaningPolicyServiceValidationTest {

	@Mock
	private CleaningPolicyMapper policyMapper;

	@Mock
	private CleaningRuleMapper ruleMapper;

	@Mock
	private CleaningPolicyRuleMapper policyRuleMapper;

	private CleaningPolicyService cleaningPolicyService;

	@BeforeEach
	public void setUp() {
		cleaningPolicyService = new CleaningPolicyService(policyMapper, ruleMapper, policyRuleMapper);
	}

	@Test
	public void shouldRejectUnsupportedRuleType() {
		CleaningRuleRequest request = CleaningRuleRequest.builder()
			.name("rule-a")
			.ruleType("UNKNOWN")
			.category("PII")
			.severity(0.8)
			.enabled(1)
			.config(Map.of())
			.build();

		assertThrows(InvalidInputException.class, () -> cleaningPolicyService.createRule(request));
	}

	@Test
	public void shouldRejectRegexWithoutPattern() {
		CleaningRuleRequest request = CleaningRuleRequest.builder()
			.name("rule-regex")
			.ruleType("REGEX")
			.category("PII")
			.severity(0.8)
			.enabled(1)
			.config(Map.of("flags", "CASE_INSENSITIVE"))
			.build();

		assertThrows(InvalidInputException.class, () -> cleaningPolicyService.createRule(request));
	}

	@Test
	public void shouldRejectRegexUnsupportedFlags() {
		CleaningRuleRequest request = CleaningRuleRequest.builder()
			.name("rule-regex")
			.ruleType("REGEX")
			.category("PII")
			.severity(0.8)
			.enabled(1)
			.config(Map.of("pattern", "\\\\d{11}", "flags", "CASE_INSENSITIVE,INVALID"))
			.build();

		assertThrows(InvalidInputException.class, () -> cleaningPolicyService.createRule(request));
	}

	@Test
	public void shouldRejectSeverityOutOfRange() {
		CleaningRuleRequest request = CleaningRuleRequest.builder()
			.name("rule-regex")
			.ruleType("REGEX")
			.category("PII")
			.severity(1.2)
			.enabled(1)
			.config(Map.of("pattern", "\\\\d{11}", "flags", "CASE_INSENSITIVE"))
			.build();

		assertThrows(InvalidInputException.class, () -> cleaningPolicyService.createRule(request));
	}

	@Test
	public void shouldAllowRegexFlagsAsArray() {
		CleaningRuleRequest request = CleaningRuleRequest.builder()
			.name("rule-regex")
			.ruleType("REGEX")
			.category("PII")
			.severity(0.8)
			.enabled(1)
			.config(Map.of("pattern", "\\\\d{11}", "flags", List.of("CASE_INSENSITIVE", "MULTILINE")))
			.build();

		assertDoesNotThrow(() -> cleaningPolicyService.createRule(request));
		verify(ruleMapper).insert(org.mockito.ArgumentMatchers.any(CleaningRule.class));
	}

	@Test
	public void shouldRejectUnsupportedRegexMaskMode() {
		CleaningRuleRequest request = CleaningRuleRequest.builder()
			.name("rule-regex")
			.ruleType("REGEX")
			.category("PII")
			.severity(0.8)
			.enabled(1)
			.config(Map.of("pattern", "\\\\d{11}", "flags", "CASE_INSENSITIVE", "maskMode", "RANDOM"))
			.build();

		assertThrows(InvalidInputException.class, () -> cleaningPolicyService.createRule(request));
	}

	@Test
	public void shouldRejectMaskTextWhenDeleteMode() {
		CleaningRuleRequest request = CleaningRuleRequest.builder()
			.name("rule-regex")
			.ruleType("REGEX")
			.category("PII")
			.severity(0.8)
			.enabled(1)
			.config(Map.of("pattern", "\\\\d{11}", "flags", "CASE_INSENSITIVE", "maskMode", "DELETE", "maskText",
					"***"))
			.build();

		assertThrows(InvalidInputException.class, () -> cleaningPolicyService.createRule(request));
	}

	@Test
	public void shouldAllowRegexMaskModeAndMaskText() {
		CleaningRuleRequest request = CleaningRuleRequest.builder()
			.name("rule-regex")
			.ruleType("REGEX")
			.category("PII")
			.severity(0.8)
			.enabled(1)
			.config(Map.of("pattern", "\\\\d{11}", "flags", "CASE_INSENSITIVE", "maskMode", "PLACEHOLDER", "maskText",
					"***"))
			.build();

		assertDoesNotThrow(() -> cleaningPolicyService.createRule(request));
		verify(ruleMapper).insert(org.mockito.ArgumentMatchers.any(CleaningRule.class));
	}

}
