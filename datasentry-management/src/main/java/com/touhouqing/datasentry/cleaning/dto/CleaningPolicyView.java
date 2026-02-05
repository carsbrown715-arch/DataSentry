package com.touhouqing.datasentry.cleaning.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CleaningPolicyView {

	private Long id;

	private String name;

	private String description;

	private Integer enabled;

	private String defaultAction;

	private String configJson;

	private LocalDateTime createdTime;

	private LocalDateTime updatedTime;

	private List<CleaningPolicyRuleItem> rules;

}
