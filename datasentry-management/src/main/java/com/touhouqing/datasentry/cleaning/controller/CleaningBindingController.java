package com.touhouqing.datasentry.cleaning.controller;

import com.touhouqing.datasentry.cleaning.dto.CleaningBindingRequest;
import com.touhouqing.datasentry.cleaning.model.CleaningBinding;
import com.touhouqing.datasentry.cleaning.service.CleaningBindingService;
import com.touhouqing.datasentry.vo.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/datasentry/cleaning")
public class CleaningBindingController {

	private final CleaningBindingService bindingService;

	@GetMapping("/bindings/online-default/{agentId}")
	public ResponseEntity<ApiResponse<CleaningBinding>> getOnlineDefaultBinding(@PathVariable Long agentId) {
		return ResponseEntity.ok(ApiResponse.success("success", bindingService.getOnlineDefaultBinding(agentId)));
	}

	@PutMapping("/bindings/online-default")
	public ResponseEntity<ApiResponse<CleaningBinding>> upsertOnlineDefaultBinding(
			@RequestBody CleaningBindingRequest request) {
		return ResponseEntity.ok(ApiResponse.success("success", bindingService.upsertOnlineDefaultBinding(request)));
	}

}
