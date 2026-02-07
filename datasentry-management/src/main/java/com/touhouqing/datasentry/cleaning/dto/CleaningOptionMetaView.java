package com.touhouqing.datasentry.cleaning.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CleaningOptionMetaView {

	private List<CleaningOptionItemView> defaultActions;

	private List<CleaningOptionItemView> ruleTypes;

	private List<CleaningOptionItemView> ruleCategories;

	private List<CleaningOptionItemView> reviewPolicies;

	private List<CleaningOptionItemView> jobModes;

	private List<CleaningOptionItemView> writebackModes;

	private List<CleaningOptionItemView> runStatuses;

	private List<CleaningOptionItemView> verdicts;

	private List<CleaningOptionItemView> targetConfigTypes;

	private List<CleaningThresholdItemView> thresholdGuidance;

	private Map<String, CleaningRuleTypeSchemaView> ruleTypeSchemas;

	private Map<String, Map<String, Boolean>> ruleTypeUiBehavior;

	private List<CleaningSeverityGuidanceItemView> severityGuidance;

	private Map<String, String> riskConfirmations;

	private List<CleaningOptionItemView> regexTemplates;

	private Map<String, Object> jsonConfigTemplates;

	private Map<String, String> fieldHelp;

}
