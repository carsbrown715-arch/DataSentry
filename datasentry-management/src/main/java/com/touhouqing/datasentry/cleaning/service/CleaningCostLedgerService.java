package com.touhouqing.datasentry.cleaning.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.touhouqing.datasentry.cleaning.enums.CleaningCostChannel;
import com.touhouqing.datasentry.cleaning.mapper.CleaningCostLedgerMapper;
import com.touhouqing.datasentry.cleaning.model.CleaningCostLedger;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CleaningCostLedgerService {

	private final CleaningCostLedgerMapper costLedgerMapper;

	public BigDecimal recordCost(CostEntry entry) {
		long inputTokens = Math.max(entry.inputTokensEst(), 0L);
		long outputTokens = Math.max(entry.outputTokensEst(), 0L);
		BigDecimal inCost = entry.unitPriceIn()
			.multiply(BigDecimal.valueOf(inputTokens))
			.divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP);
		BigDecimal outCost = entry.unitPriceOut()
			.multiply(BigDecimal.valueOf(outputTokens))
			.divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP);
		BigDecimal total = inCost.add(outCost).setScale(6, RoundingMode.HALF_UP);

		CleaningCostLedger ledger = CleaningCostLedger.builder()
			.jobId(entry.jobId())
			.jobRunId(entry.jobRunId())
			.agentId(entry.agentId())
			.traceId(entry.traceId())
			.channel(entry.channel().name())
			.detectorLevel(entry.detectorLevel())
			.provider(entry.provider())
			.model(entry.model())
			.inputTokensEst(inputTokens)
			.outputTokensEst(outputTokens)
			.unitPriceIn(entry.unitPriceIn())
			.unitPriceOut(entry.unitPriceOut())
			.costAmount(total)
			.currency(entry.currency())
			.createdTime(LocalDateTime.now())
			.build();
		costLedgerMapper.insert(ledger);
		return total;
	}

	public List<CleaningCostLedger> list(Long jobRunId, String traceId, String channel) {
		LambdaQueryWrapper<CleaningCostLedger> wrapper = new LambdaQueryWrapper<>();
		if (jobRunId != null) {
			wrapper.eq(CleaningCostLedger::getJobRunId, jobRunId);
		}
		if (traceId != null && !traceId.isBlank()) {
			wrapper.eq(CleaningCostLedger::getTraceId, traceId);
		}
		if (channel != null && !channel.isBlank()) {
			wrapper.eq(CleaningCostLedger::getChannel, channel.toUpperCase());
		}
		return costLedgerMapper.selectList(wrapper.orderByDesc(CleaningCostLedger::getId));
	}

	public record CostEntry(Long jobId, Long jobRunId, Long agentId, String traceId, CleaningCostChannel channel,
			String detectorLevel, String provider, String model, long inputTokensEst, long outputTokensEst,
			BigDecimal unitPriceIn, BigDecimal unitPriceOut, String currency) {
	}

}
