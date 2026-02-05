package com.touhouqing.datasentry.cleaning.controller;

import com.touhouqing.datasentry.cleaning.dto.CleaningJobCreateRequest;
import com.touhouqing.datasentry.cleaning.model.CleaningJob;
import com.touhouqing.datasentry.cleaning.model.CleaningJobRun;
import com.touhouqing.datasentry.cleaning.service.CleaningJobService;
import com.touhouqing.datasentry.vo.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/datasentry/cleaning")
public class CleaningJobController {

	private final CleaningJobService jobService;

	@PostMapping("/jobs")
	public ResponseEntity<ApiResponse<CleaningJob>> createJob(@RequestBody @Valid CleaningJobCreateRequest request) {
		CleaningJob job = jobService.createJob(request);
		return ResponseEntity.ok(ApiResponse.success("success", job));
	}

	@GetMapping("/jobs/{jobId}")
	public ResponseEntity<ApiResponse<CleaningJob>> getJob(@PathVariable Long jobId) {
		CleaningJob job = jobService.getJob(jobId);
		return ResponseEntity.ok(ApiResponse.success("success", job));
	}

	@PostMapping("/jobs/{jobId}/runs")
	public ResponseEntity<ApiResponse<CleaningJobRun>> createRun(@PathVariable Long jobId) {
		CleaningJobRun run = jobService.createRun(jobId);
		return ResponseEntity.ok(ApiResponse.success("success", run));
	}

	@GetMapping("/job-runs/{runId}")
	public ResponseEntity<ApiResponse<CleaningJobRun>> getRun(@PathVariable Long runId) {
		CleaningJobRun run = jobService.getRun(runId);
		return ResponseEntity.ok(ApiResponse.success("success", run));
	}

	@PostMapping("/job-runs/{runId}/pause")
	public ResponseEntity<ApiResponse<CleaningJobRun>> pauseRun(@PathVariable Long runId) {
		CleaningJobRun run = jobService.pauseRun(runId);
		return ResponseEntity.ok(ApiResponse.success("success", run));
	}

	@PostMapping("/job-runs/{runId}/resume")
	public ResponseEntity<ApiResponse<CleaningJobRun>> resumeRun(@PathVariable Long runId) {
		CleaningJobRun run = jobService.resumeRun(runId);
		return ResponseEntity.ok(ApiResponse.success("success", run));
	}

	@PostMapping("/job-runs/{runId}/cancel")
	public ResponseEntity<ApiResponse<CleaningJobRun>> cancelRun(@PathVariable Long runId) {
		CleaningJobRun run = jobService.cancelRun(runId);
		return ResponseEntity.ok(ApiResponse.success("success", run));
	}

}
