package com.touhouqing.datasentry.cleaning.service;

import com.touhouqing.datasentry.cleaning.dto.CleaningCheckRequest;
import com.touhouqing.datasentry.cleaning.dto.CleaningResponse;

public interface CleaningService {

	CleaningResponse check(Long agentId, CleaningCheckRequest request, String traceId);

	CleaningResponse sanitize(Long agentId, CleaningCheckRequest request, String traceId);

}
