package com.touhouqing.datasentry.cleaning.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CleaningReviewDecisionRequest {

	@NotNull
	private Integer version;

	private String reason;

	private String reviewer;

}
