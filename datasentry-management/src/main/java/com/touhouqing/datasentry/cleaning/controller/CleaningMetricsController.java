package com.touhouqing.datasentry.cleaning.controller;

import com.touhouqing.datasentry.cleaning.dto.CleaningAlertView;
import com.touhouqing.datasentry.cleaning.dto.CleaningMetricsView;
import com.touhouqing.datasentry.cleaning.service.CleaningMetricsService;
import com.touhouqing.datasentry.vo.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/datasentry/cleaning")
public class CleaningMetricsController {

	private final CleaningMetricsService metricsService;

	@GetMapping("/metrics/summary")
	public ResponseEntity<ApiResponse<CleaningMetricsView>> summary() {
		return ResponseEntity.ok(ApiResponse.success("success", metricsService.summary()));
	}

	@GetMapping("/alerts")
	public ResponseEntity<ApiResponse<List<CleaningAlertView>>> alerts() {
		return ResponseEntity.ok(ApiResponse.success("success", metricsService.alerts()));
	}

}
