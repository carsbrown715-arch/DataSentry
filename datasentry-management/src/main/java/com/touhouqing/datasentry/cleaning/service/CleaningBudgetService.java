package com.touhouqing.datasentry.cleaning.service;

import com.touhouqing.datasentry.cleaning.enums.CleaningBudgetStatus;
import com.touhouqing.datasentry.cleaning.model.CleaningJob;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class CleaningBudgetService {

	public CleaningBudgetStatus evaluate(CleaningJob job, BigDecimal actualCost) {
		if (!isBudgetEnabled(job)) {
			return CleaningBudgetStatus.NORMAL;
		}
		BigDecimal normalized = actualCost != null ? actualCost : BigDecimal.ZERO;
		BigDecimal hard = safeLimit(job.getBudgetHardLimit());
		if (normalized.compareTo(hard) >= 0) {
			return CleaningBudgetStatus.HARD_EXCEEDED;
		}
		BigDecimal soft = safeLimit(job.getBudgetSoftLimit());
		if (normalized.compareTo(soft) >= 0) {
			return CleaningBudgetStatus.SOFT_EXCEEDED;
		}
		return CleaningBudgetStatus.NORMAL;
	}

	public boolean isBudgetEnabled(CleaningJob job) {
		return job != null && job.getBudgetEnabled() != null && job.getBudgetEnabled() == 1;
	}

	private BigDecimal safeLimit(BigDecimal value) {
		if (value == null || value.signum() <= 0) {
			return BigDecimal.valueOf(Double.MAX_VALUE);
		}
		return value;
	}

}
