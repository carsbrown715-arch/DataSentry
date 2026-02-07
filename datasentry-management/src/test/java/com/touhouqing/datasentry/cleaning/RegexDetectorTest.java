package com.touhouqing.datasentry.cleaning;

import com.touhouqing.datasentry.cleaning.detector.RegexDetector;
import com.touhouqing.datasentry.cleaning.model.CleaningRule;
import com.touhouqing.datasentry.cleaning.model.Finding;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RegexDetectorTest {

	@Test
	public void detectsPhoneNumberWithSpan() {
		RegexDetector detector = new RegexDetector();
		CleaningRule rule = CleaningRule.builder()
			.id(1L)
			.name("手机号检测")
			.ruleType("REGEX")
			.category("PII_PHONE")
			.severity(0.9)
			.enabled(1)
			.configJson("{\"pattern\":\"1[3-9]\\\\d{9}\",\"flags\":\"NONE\"}")
			.build();
		String text = "请联系 13800138000 处理";

		List<Finding> findings = detector.detect(text, rule);

		assertEquals(1, findings.size());
		Finding finding = findings.get(0);
		assertEquals("PII_PHONE", finding.getCategory());
		int expectedStart = text.indexOf("13800138000");
		assertEquals(expectedStart, finding.getStart());
		assertEquals(expectedStart + 11, finding.getEnd());
		assertEquals("[REDACTED]", finding.getReplacement());
	}

	@Test
	public void detectsPhoneNumberWhenPatternIsOverEscaped() {
		RegexDetector detector = new RegexDetector();
		CleaningRule rule = CleaningRule.builder()
			.id(2L)
			.name("手机号检测-过度转义")
			.ruleType("REGEX")
			.category("PII_PHONE")
			.severity(0.9)
			.enabled(1)
			.configJson("{\"pattern\":\"1[3-9]\\\\\\\\d{9}\",\"flags\":\"CASE_INSENSITIVE\"}")
			.build();
		String text = "13800001002";

		List<Finding> findings = detector.detect(text, rule);

		assertEquals(1, findings.size());
		assertEquals(0, findings.get(0).getStart());
		assertEquals(11, findings.get(0).getEnd());
		assertEquals("[REDACTED]", findings.get(0).getReplacement());
	}

	@Test
	public void detectsWithCustomMaskReplacement() {
		RegexDetector detector = new RegexDetector();
		CleaningRule rule = CleaningRule.builder()
			.id(3L)
			.name("手机号检测-自定义替换")
			.ruleType("REGEX")
			.category("PII_PHONE")
			.severity(0.9)
			.enabled(1)
			.configJson(
					"{\"pattern\":\"1[3-9]\\\\d{9}\",\"flags\":\"CASE_INSENSITIVE\",\"maskMode\":\"PLACEHOLDER\",\"maskText\":\"***\"}")
			.build();
		String text = "13800001002";

		List<Finding> findings = detector.detect(text, rule);

		assertEquals(1, findings.size());
		assertEquals("***", findings.get(0).getReplacement());
	}

	@Test
	public void detectsWithDeleteMaskMode() {
		RegexDetector detector = new RegexDetector();
		CleaningRule rule = CleaningRule.builder()
			.id(4L)
			.name("手机号检测-删除命中")
			.ruleType("REGEX")
			.category("PII_PHONE")
			.severity(0.9)
			.enabled(1)
			.configJson("{\"pattern\":\"1[3-9]\\\\d{9}\",\"flags\":\"CASE_INSENSITIVE\",\"maskMode\":\"DELETE\"}")
			.build();
		String text = "13800001002";

		List<Finding> findings = detector.detect(text, rule);

		assertEquals(1, findings.size());
		assertEquals("", findings.get(0).getReplacement());
	}

}
