package com.touhouqing.datasentry.cleaning.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CleaningRuleTypeSchemaView {

	private String ruleType;

	private String title;

	private String description;

	private List<CleaningRuleTypeFieldView> fields;

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class CleaningRuleTypeFieldView {

		private String name;

		private String labelZh;

		private String type;

		private Boolean required;

		private Object defaultValue;

		private String placeholder;

		private String help;

		private List<String> options;

	}

}
