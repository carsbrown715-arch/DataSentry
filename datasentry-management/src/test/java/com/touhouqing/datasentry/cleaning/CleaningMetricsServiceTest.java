package com.touhouqing.datasentry.cleaning;

import com.touhouqing.datasentry.cleaning.dto.CleaningAlertView;
import com.touhouqing.datasentry.cleaning.mapper.CleaningCostLedgerMapper;
import com.touhouqing.datasentry.cleaning.mapper.CleaningDlqMapper;
import com.touhouqing.datasentry.cleaning.mapper.CleaningJobRunMapper;
import com.touhouqing.datasentry.cleaning.service.CleaningMetricsService;
import com.touhouqing.datasentry.cleaning.service.CleaningOpsStateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CleaningMetricsServiceTest {

	@Mock
	private CleaningJobRunMapper jobRunMapper;

	@Mock
	private CleaningDlqMapper dlqMapper;

	@Mock
	private CleaningCostLedgerMapper costLedgerMapper;

	private CleaningOpsStateService opsStateService;

	private CleaningMetricsService metricsService;

	@BeforeEach
	public void setUp() {
		opsStateService = new CleaningOpsStateService();
		metricsService = new CleaningMetricsService(jobRunMapper, dlqMapper, costLedgerMapper, opsStateService);
		when(jobRunMapper.selectCount(any())).thenReturn(0L);
		when(dlqMapper.selectCount(any())).thenReturn(0L);
	}

	@Test
	public void shouldEmitOnnxDegradedAlertWhenProviderDegraded() {
		opsStateService.setL2ProviderStatus("ONNX/DEGRADED");

		List<CleaningAlertView> alerts = metricsService.alerts();

		assertTrue(alerts.stream().anyMatch(item -> "L2_ONNX_DEGRADED".equals(item.getCode())));
	}

	@Test
	public void shouldEmitCloudDegradedAlertWhenProviderDegraded() {
		opsStateService.setL2ProviderStatus("CLOUD_API/DEGRADED");

		List<CleaningAlertView> alerts = metricsService.alerts();

		assertTrue(alerts.stream().anyMatch(item -> "L2_CLOUD_API_DEGRADED".equals(item.getCode())));
	}

}
