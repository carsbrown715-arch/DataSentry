package com.touhouqing.datasentry.cleaning.batch;

import com.touhouqing.datasentry.cleaning.service.CleaningDlqService;
import com.touhouqing.datasentry.properties.DataSentryProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CleaningDlqScheduler {

	private final CleaningDlqService dlqService;

	private final DataSentryProperties dataSentryProperties;

	@Scheduled(fixedDelayString = "${spring.ai.alibaba.datasentry.cleaning.batch.poll-interval-ms:5000}")
	public void poll() {
		if (!dataSentryProperties.getCleaning().isEnabled()) {
			return;
		}
		dlqService.retryBatch();
	}

}
