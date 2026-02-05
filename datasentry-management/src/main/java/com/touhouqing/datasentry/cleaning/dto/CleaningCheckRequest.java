package com.touhouqing.datasentry.cleaning.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CleaningCheckRequest {

	@NotBlank(message = "text不能为空")
	private String text;

	private String scene;

	private Long policyId;

}
