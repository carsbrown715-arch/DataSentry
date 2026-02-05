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
	}

}
