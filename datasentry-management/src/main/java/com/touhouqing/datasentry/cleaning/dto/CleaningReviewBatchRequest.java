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
public class CleaningReviewBatchRequest {

	private List<Long> taskIds;

	private Long jobRunId;

	private String filter;

	private String reason;

	private String reviewer;

}
