package com.touhouqing.datasentry.cleaning.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CleaningThresholdItemView {

	private String code;

	private String labelZh;

	private Double defaultValue;

	private String description;

	private String recommendedRange;

}
