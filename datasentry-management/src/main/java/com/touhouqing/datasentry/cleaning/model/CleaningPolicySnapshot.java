package com.touhouqing.datasentry.cleaning.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CleaningPolicySnapshot {

	private Long policyId;

	private String policyName;

	private String defaultAction;

	private CleaningPolicyConfig config;

	private List<CleaningRule> rules;

}
