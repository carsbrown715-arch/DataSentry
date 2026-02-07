package com.touhouqing.datasentry.cleaning;

import com.touhouqing.datasentry.cleaning.detector.LlmDetector;
import com.touhouqing.datasentry.cleaning.detector.L2Detector;
import com.touhouqing.datasentry.cleaning.detector.RegexDetector;
import com.touhouqing.datasentry.cleaning.model.CleaningContext;
import com.touhouqing.datasentry.cleaning.model.CleaningPolicyConfig;
import com.touhouqing.datasentry.cleaning.model.CleaningPolicySnapshot;
import com.touhouqing.datasentry.cleaning.model.CleaningRule;
import com.touhouqing.datasentry.cleaning.model.Finding;
import com.touhouqing.datasentry.cleaning.pipeline.DetectNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DetectNodeFailClosedTest {

	@Test
	public void shouldSkipL3WhenDisableL3FlagTrue() {
		RegexDetector regexDetector = mock(RegexDetector.class);
		L2Detector l2Detector = mock(L2Detector.class);
		LlmDetector llmDetector = mock(LlmDetector.class);
		DetectNode node = new DetectNode(regexDetector, l2Detector, llmDetector);
		when(regexDetector.detect(anyString(), any())).thenReturn(List.of());
		when(l2Detector.detect(anyString(), any(), any())).thenReturn(List.of());

		CleaningPolicySnapshot snapshot = CleaningPolicySnapshot.builder()
			.config(CleaningPolicyConfig.builder().llmEnabled(true).build())
			.rules(List.of(CleaningRule.builder().ruleType("LLM").build(),
					CleaningRule.builder().ruleType("REGEX").build()))
			.build();
		CleaningContext context = CleaningContext.builder().originalText("abc").policySnapshot(snapshot).build();
		context.getMetadata().put("disableL3", true);

		node.process(context);

		verify(llmDetector, never()).detect(anyString());
		assertEquals(List.of(), context.getFindings());
	}

	@Test
	public void shouldCallL3WhenDisableL3FlagFalse() {
		RegexDetector regexDetector = mock(RegexDetector.class);
		L2Detector l2Detector = mock(L2Detector.class);
		LlmDetector llmDetector = mock(LlmDetector.class);
		DetectNode node = new DetectNode(regexDetector, l2Detector, llmDetector);
		when(regexDetector.detect(anyString(), any())).thenReturn(List.of());
		when(l2Detector.detect(anyString(), any(), any())).thenReturn(List.of(Finding.builder().severity(0.6).build()));
		when(llmDetector.detect(anyString())).thenReturn(List.of(Finding.builder().category("X").build()));

		CleaningPolicySnapshot snapshot = CleaningPolicySnapshot.builder()
			.config(CleaningPolicyConfig.builder().llmEnabled(true).build())
			.rules(List.of(CleaningRule.builder().ruleType("L2_DUMMY").build(),
					CleaningRule.builder().ruleType("LLM").build()))
			.build();
		CleaningContext context = CleaningContext.builder().originalText("abc").policySnapshot(snapshot).build();

		node.process(context);

		verify(llmDetector).detect(anyString());
		assertEquals(2, context.getFindings().size());
	}

}
