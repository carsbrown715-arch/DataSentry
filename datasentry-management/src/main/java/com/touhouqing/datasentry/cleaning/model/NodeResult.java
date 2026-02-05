package com.touhouqing.datasentry.cleaning.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NodeResult {

	private String status;

	@Builder.Default
	private List<String> errors = Collections.emptyList();

	public static NodeResult ok() {
		return NodeResult.builder().status("OK").build();
	}

	public static NodeResult skipped() {
		return NodeResult.builder().status("SKIPPED").build();
	}

	public static NodeResult failed(List<String> errors) {
		return NodeResult.builder().status("FAILED").errors(errors).build();
	}

}
