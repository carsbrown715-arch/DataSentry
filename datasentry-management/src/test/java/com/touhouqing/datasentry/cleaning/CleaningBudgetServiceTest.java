package com.touhouqing.datasentry.cleaning;

import com.touhouqing.datasentry.cleaning.enums.CleaningBudgetStatus;
import com.touhouqing.datasentry.cleaning.model.CleaningJob;
import com.touhouqing.datasentry.cleaning.service.CleaningBudgetService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CleaningBudgetServiceTest {

	private final CleaningBudgetService budgetService = new CleaningBudgetService();

	@Test
	public void shouldReturnNormalWhenBudgetDisabled() {
		CleaningJob job = CleaningJob.builder().budgetEnabled(0).build();
		assertEquals(CleaningBudgetStatus.NORMAL, budgetService.evaluate(job, BigDecimal.valueOf(999)));
	}

	@Test
	public void shouldReturnSoftExceededWhenCrossSoftLimit() {
		CleaningJob job = CleaningJob.builder()
			.budgetEnabled(1)
			.budgetSoftLimit(BigDecimal.valueOf(10))
			.budgetHardLimit(BigDecimal.valueOf(50))
			.build();

		assertEquals(CleaningBudgetStatus.SOFT_EXCEEDED, budgetService.evaluate(job, BigDecimal.valueOf(10)));
	}

	@Test
	public void shouldReturnHardExceededWhenCrossHardLimit() {
		CleaningJob job = CleaningJob.builder()
			.budgetEnabled(1)
			.budgetSoftLimit(BigDecimal.valueOf(10))
			.budgetHardLimit(BigDecimal.valueOf(50))
			.build();

		assertEquals(CleaningBudgetStatus.HARD_EXCEEDED, budgetService.evaluate(job, BigDecimal.valueOf(50)));
	}

}
