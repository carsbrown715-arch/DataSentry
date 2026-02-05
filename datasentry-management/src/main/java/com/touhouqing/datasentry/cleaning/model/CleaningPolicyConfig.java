package com.touhouqing.datasentry.cleaning.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CleaningPolicyConfig {

	private Double blockThreshold;

	private Double reviewThreshold;

	private Boolean llmEnabled;

	public double resolvedBlockThreshold() {
		return blockThreshold != null ? blockThreshold : 0.7;
	}

	public double resolvedReviewThreshold() {
		return reviewThreshold != null ? reviewThreshold : 0.4;
	}

	public boolean resolvedLlmEnabled() {
		return llmEnabled == null || llmEnabled;
	}

}
