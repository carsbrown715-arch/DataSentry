package com.touhouqing.datasentry.cleaning;

import com.touhouqing.datasentry.cleaning.util.CleaningJsonPathProcessor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class CleaningJsonPathProcessorTest {

	private final CleaningJsonPathProcessor processor = new CleaningJsonPathProcessor();

	@Test
	public void shouldExtractValueBySimplePath() {
		String json = "{\"profile\":{\"email\":\"a@b.com\"}}";

		String value = processor.extractText(json, "$.profile.email");

		assertEquals("a@b.com", value);
	}

	@Test
	public void shouldReplaceValueByArrayPath() {
		String json = "{\"contacts\":[{\"phone\":\"13800000000\"},{\"phone\":\"13900000000\"}]}";

		String replaced = processor.replaceText(json, "$.contacts[1].phone", "[REDACTED]");

		assertEquals("[REDACTED]", processor.extractText(replaced, "$.contacts[1].phone"));
		assertEquals("13800000000", processor.extractText(replaced, "$.contacts[0].phone"));
	}

	@Test
	public void shouldReturnNullForInvalidPath() {
		String json = "{\"k\":\"v\"}";

		assertNull(processor.extractText(json, "k"));
		assertNull(processor.replaceText(json, "$.x.y", "1"));
	}

}
