package com.touhouqing.datasentry.cleaning.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CleaningOptionItemView {

	private String code;

	private String labelZh;

	private String description;

	private String effect;

	private String riskLevel;

	private String recommendedFor;

	private String caution;

	private String configSchemaHint;

	private Object sampleConfig;

}
