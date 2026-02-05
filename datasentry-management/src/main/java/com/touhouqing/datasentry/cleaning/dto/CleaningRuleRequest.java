package com.touhouqing.datasentry.cleaning.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CleaningRuleRequest {

	private String name;

	private String ruleType;

	private String category;

	private Double severity;

	private Integer enabled;

	private Map<String, Object> config;

}
