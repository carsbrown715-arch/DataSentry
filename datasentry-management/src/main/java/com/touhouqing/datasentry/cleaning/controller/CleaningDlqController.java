package com.touhouqing.datasentry.cleaning.controller;

import com.touhouqing.datasentry.cleaning.model.CleaningDlqRecord;
import com.touhouqing.datasentry.cleaning.service.CleaningDlqService;
import com.touhouqing.datasentry.vo.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/datasentry/cleaning")
public class CleaningDlqController {

	private final CleaningDlqService dlqService;

	@GetMapping("/dlq")
	public ResponseEntity<ApiResponse<List<CleaningDlqRecord>>> list(@RequestParam(required = false) String status,
			@RequestParam(required = false) Long jobRunId) {
		return ResponseEntity.ok(ApiResponse.success("success", dlqService.list(status, jobRunId)));
	}

	@PostMapping("/dlq/{id}/retry")
	public ResponseEntity<ApiResponse<Void>> retry(@PathVariable Long id) {
		dlqService.retryOne(id);
		return ResponseEntity.ok(ApiResponse.success("success", null));
	}

}
