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
public class CleaningPolicyRequest {

	private String name;

	private String description;

	private Integer enabled;

	private String defaultAction;

	private Map<String, Object> config;

}
