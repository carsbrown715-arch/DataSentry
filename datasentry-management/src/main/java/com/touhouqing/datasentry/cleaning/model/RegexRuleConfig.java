package com.touhouqing.datasentry.cleaning.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegexRuleConfig {

	public static final String MASK_MODE_PLACEHOLDER = "PLACEHOLDER";

	public static final String MASK_MODE_DELETE = "DELETE";

	private String pattern;

	private String flags;

	private String maskMode;

	private String maskText;

}
