package com.touhouqing.datasentry.cleaning.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CleaningPolicyVersionView {

	private Long id;

	private Long policyId;

	private Integer versionNo;

	private String status;

	private String defaultAction;

	private LocalDateTime createdTime;

	private LocalDateTime updatedTime;

}
