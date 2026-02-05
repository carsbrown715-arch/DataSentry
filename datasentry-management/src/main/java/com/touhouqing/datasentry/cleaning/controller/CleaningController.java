package com.touhouqing.datasentry.cleaning.controller;

import com.touhouqing.datasentry.cleaning.dto.CleaningCheckRequest;
import com.touhouqing.datasentry.cleaning.dto.CleaningResponse;
import com.touhouqing.datasentry.cleaning.service.CleaningService;
import com.touhouqing.datasentry.entity.Agent;
import com.touhouqing.datasentry.properties.DataSentryProperties;
import com.touhouqing.datasentry.service.agent.AgentService;
import com.touhouqing.datasentry.vo.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/datasentry/cleaning")
public class CleaningController {

	private static final String TRACE_HEADER = "X-Trace-Id";

	private static final String API_KEY_HEADER = "X-API-KEY";

	private final CleaningService cleaningService;

	private final AgentService agentService;

	private final DataSentryProperties dataSentryProperties;

	@PostMapping("/{agentId}/check")
	public ResponseEntity<ApiResponse<CleaningResponse>> check(@PathVariable Long agentId,
			@RequestHeader(value = API_KEY_HEADER, required = false) String apiKey,
			@RequestHeader(value = TRACE_HEADER, required = false) String traceHeader,
			@RequestBody @Valid CleaningCheckRequest request) {
		return handle(agentId, apiKey, traceHeader, request, false);
	}

	@PostMapping("/{agentId}/sanitize")
	public ResponseEntity<ApiResponse<CleaningResponse>> sanitize(@PathVariable Long agentId,
			@RequestHeader(value = API_KEY_HEADER, required = false) String apiKey,
			@RequestHeader(value = TRACE_HEADER, required = false) String traceHeader,
			@RequestBody @Valid CleaningCheckRequest request) {
		return handle(agentId, apiKey, traceHeader, request, true);
	}

	private ResponseEntity<ApiResponse<CleaningResponse>> handle(Long agentId, String apiKey, String traceHeader,
			CleaningCheckRequest request, boolean sanitize) {
		if (!dataSentryProperties.getCleaning().isEnabled()) {
			return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
				.body(ApiResponse.<CleaningResponse>error("清理功能已关闭"));
		}
		Agent agent = agentService.findById(agentId);
		if (agent == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.<CleaningResponse>error("智能体不存在"));
		}
		if (!isAuthorized(agent, apiKey)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(ApiResponse.<CleaningResponse>error("API Key 无效或未启用"));
		}
		String traceId = StringUtils.hasText(traceHeader) ? traceHeader : UUID.randomUUID().toString();
		MDC.put("traceId", traceId);
		try {
			CleaningResponse response = sanitize ? cleaningService.sanitize(agentId, request, traceId)
					: cleaningService.check(agentId, request, traceId);
			return ResponseEntity.ok(ApiResponse.success("success", response));
		}
		finally {
			MDC.remove("traceId");
		}
	}

	private boolean isAuthorized(Agent agent, String apiKey) {
		if (agent.getApiKeyEnabled() == null || agent.getApiKeyEnabled() != 1) {
			return false;
		}
		return StringUtils.hasText(agent.getApiKey()) && agent.getApiKey().equals(apiKey);
	}

}
