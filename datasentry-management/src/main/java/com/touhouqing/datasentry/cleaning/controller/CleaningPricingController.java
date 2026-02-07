package com.touhouqing.datasentry.cleaning.controller;

import com.touhouqing.datasentry.cleaning.dto.CleaningPricingSyncResult;
import com.touhouqing.datasentry.cleaning.model.CleaningPriceCatalog;
import com.touhouqing.datasentry.cleaning.service.CleaningPriceSyncService;
import com.touhouqing.datasentry.vo.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/datasentry/cleaning")
public class CleaningPricingController {

	private final CleaningPriceSyncService priceSyncService;

	@PostMapping("/pricing/sync")
	public ResponseEntity<ApiResponse<CleaningPricingSyncResult>> sync(
			@RequestParam(name = "reason", required = false, defaultValue = "manual") String reason) {
		CleaningPricingSyncResult result = priceSyncService.syncNow(reason);
		String message = result.isSuccess() ? "pricing sync success" : "pricing sync failed";
		return ResponseEntity.ok(ApiResponse.success(message, result));
	}

	@GetMapping("/pricing/catalog")
	public ResponseEntity<ApiResponse<List<CleaningPriceCatalog>>> catalog() {
		return ResponseEntity.ok(ApiResponse.success("success", priceSyncService.listCatalog()));
	}

}
