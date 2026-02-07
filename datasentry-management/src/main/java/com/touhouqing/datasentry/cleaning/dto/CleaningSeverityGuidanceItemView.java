package com.touhouqing.datasentry.cleaning.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CleaningSeverityGuidanceItemView {

	private String level;

	private double min;

	private double max;

	private String labelZh;

	private String description;

}
