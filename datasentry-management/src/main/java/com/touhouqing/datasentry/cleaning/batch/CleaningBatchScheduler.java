package com.touhouqing.datasentry.cleaning.batch;

import com.touhouqing.datasentry.cleaning.mapper.CleaningJobRunMapper;
import com.touhouqing.datasentry.cleaning.model.CleaningJobRun;
import com.touhouqing.datasentry.cleaning.service.CleaningBatchProcessor;
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
public class CleaningBatchScheduler {

	private final CleaningJobRunMapper jobRunMapper;

	private final CleaningBatchProcessor batchProcessor;

	private final DataSentryProperties dataSentryProperties;

	private final String leaseOwner = resolveOwner();

	@Scheduled(fixedDelayString = "${spring.ai.alibaba.datasentry.cleaning.batch.poll-interval-ms:5000}")
	public void poll() {
		if (!dataSentryProperties.getCleaning().isEnabled()
				|| !dataSentryProperties.getCleaning().getBatch().isEnabled()) {
			return;
		}
		LocalDateTime now = LocalDateTime.now();
		List<CleaningJobRun> runs = jobRunMapper.findRunnableRuns(now, 3);
		for (CleaningJobRun run : runs) {
			try {
				LocalDateTime leaseUntil = now
					.plusSeconds(dataSentryProperties.getCleaning().getBatch().getLeaseSeconds());
				LocalDateTime startedTime = run.getStartedTime() != null ? run.getStartedTime() : now;
				int acquired = jobRunMapper.acquireLease(run.getId(), leaseOwner, leaseUntil, now, startedTime);
				if (acquired > 0) {
					CleaningJobRun locked = jobRunMapper.selectById(run.getId());
					if (locked != null) {
						batchProcessor.processRun(locked, leaseOwner);
					}
				}
			}
			catch (Exception e) {
				log.warn("Failed to process cleaning job run {}", run.getId(), e);
			}
		}
	}

	private String resolveOwner() {
		String host = System.getenv("HOSTNAME");
		if (host != null && !host.isBlank()) {
			return host;
		}
		return "local";
	}

}
