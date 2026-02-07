package com.touhouqing.datasentry.cleaning.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CleaningAlertEvent {

	private String level;

	private String code;

	private String message;

	private LocalDateTime createdTime;

	private Map<String, Object> payload;

}
