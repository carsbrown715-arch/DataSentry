package com.touhouqing.datasentry.cleaning.batch;

import com.touhouqing.datasentry.cleaning.mapper.CleaningRollbackRunMapper;
import com.touhouqing.datasentry.cleaning.model.CleaningRollbackRun;
import com.touhouqing.datasentry.cleaning.service.CleaningRollbackService;
import com.touhouqing.datasentry.properties.DataSentryProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CleaningRollbackScheduler {

	private final CleaningRollbackRunMapper rollbackRunMapper;

	private final CleaningRollbackService rollbackService;

	private final DataSentryProperties dataSentryProperties;

	@Scheduled(fixedDelayString = "${spring.ai.alibaba.datasentry.cleaning.batch.poll-interval-ms:5000}")
	public void poll() {
		if (!dataSentryProperties.getCleaning().isEnabled()
				|| !dataSentryProperties.getCleaning().getBatch().isEnabled()) {
			return;
		}
		List<CleaningRollbackRun> runs = rollbackRunMapper.findRunnableRuns(2);
		for (CleaningRollbackRun run : runs) {
			try {
				LocalDateTime now = LocalDateTime.now();
				int started = rollbackRunMapper.startRun(run.getId(), now);
				if (started > 0) {
					CleaningRollbackRun locked = rollbackRunMapper.selectById(run.getId());
					rollbackService.processRun(locked);
				}
			}
			catch (Exception e) {
				log.warn("Failed to process cleaning rollback run {}", run.getId(), e);
			}
		}
	}

}
