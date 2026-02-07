package com.touhouqing.datasentry.cleaning.controller;

import com.touhouqing.datasentry.cleaning.dto.CleaningOptionMetaView;
import com.touhouqing.datasentry.cleaning.service.CleaningMetaService;
import com.touhouqing.datasentry.vo.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/datasentry/cleaning")
public class CleaningMetaController {

	private final CleaningMetaService cleaningMetaService;

	@GetMapping("/meta/options")
	public ResponseEntity<ApiResponse<CleaningOptionMetaView>> options() {
		return ResponseEntity.ok(ApiResponse.success("success", cleaningMetaService.getOptions()));
	}

}
