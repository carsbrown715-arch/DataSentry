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
public class CleaningPolicyRollbackVersionRequest {

	@NotNull(message = "versionId 不能为空")
	private Long versionId;

	private String note;

	private String operator;

}
