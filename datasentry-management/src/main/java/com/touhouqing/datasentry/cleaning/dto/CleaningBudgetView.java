package com.touhouqing.datasentry.cleaning.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CleaningBudgetView {

	private Long runId;

	private Long jobId;

	private Integer budgetEnabled;

	private BigDecimal budgetSoftLimit;

	private BigDecimal budgetHardLimit;

	private String budgetCurrency;

	private BigDecimal estimatedCost;

	private BigDecimal actualCost;

	private String budgetStatus;

	private String budgetMessage;

}
