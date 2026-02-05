package com.touhouqing.datasentry.cleaning.controller;

import com.touhouqing.datasentry.cleaning.model.CleaningRollbackRun;
import com.touhouqing.datasentry.cleaning.service.CleaningRollbackService;
import com.touhouqing.datasentry.vo.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/datasentry/cleaning")
public class CleaningRollbackController {

	private final CleaningRollbackService rollbackService;

	@PostMapping("/job-runs/{runId}/rollback")
	public ResponseEntity<ApiResponse<CleaningRollbackRun>> createRollback(@PathVariable Long runId) {
		return ResponseEntity.ok(ApiResponse.success("success", rollbackService.createRollbackRun(runId)));
	}

	@GetMapping("/rollbacks/{rollbackRunId}")
	public ResponseEntity<ApiResponse<CleaningRollbackRun>> getRollback(@PathVariable Long rollbackRunId) {
		return ResponseEntity.ok(ApiResponse.success("success", rollbackService.getRollbackRun(rollbackRunId)));
	}

}
