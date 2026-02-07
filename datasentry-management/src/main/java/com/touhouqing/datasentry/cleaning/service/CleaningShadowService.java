package com.touhouqing.datasentry.cleaning.service;

import com.touhouqing.datasentry.cleaning.model.CleaningContext;
import com.touhouqing.datasentry.cleaning.model.CleaningPolicyConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executor;

@Slf4j
@Service
public class CleaningShadowService {

	private final Executor shadowExecutor;

	public CleaningShadowService(@Qualifier("cleaningShadowExecutor") Executor shadowExecutor) {
		this.shadowExecutor = shadowExecutor;
	}

	public void submitIfEnabled(CleaningContext context, CleaningPolicyConfig config) {
		if (context == null || config == null || !config.resolvedShadowEnabled()) {
			return;
		}
		Object globalShadow = context.getMetadata().get("shadowEnabled");
		if (globalShadow instanceof Boolean && !((Boolean) globalShadow)) {
			return;
		}
		double ratio = config.resolvedShadowSampleRatio();
		if (ratio <= 0 || Math.random() > ratio) {
			return;
		}
		try {
			shadowExecutor
				.execute(() -> log.info("Shadow record traceId={} verdict={} findings={}", context.getTraceId(),
						context.getVerdict(), context.getFindings() != null ? context.getFindings().size() : 0));
		}
		catch (Exception e) {
			log.debug("Discard shadow task traceId={}", context.getTraceId(), e);
		}
	}

}
