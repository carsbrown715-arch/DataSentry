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

	private String pattern;

	private String flags;

}
