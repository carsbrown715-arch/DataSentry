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
public class CleaningPricingSyncResult {

	private boolean success;

	private String sourceType;

	private String reason;

	private Integer total;

	private Integer inserted;

	private Integer updated;

	private Integer skipped;

	private String message;

	private LocalDateTime startedTime;

	private LocalDateTime finishedTime;

}
