package com.touhouqing.datasentry.cleaning.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.touhouqing.datasentry.cleaning.dto.CleaningBindingRequest;
import com.touhouqing.datasentry.cleaning.enums.CleaningBindingType;
import com.touhouqing.datasentry.cleaning.mapper.CleaningBindingMapper;
import com.touhouqing.datasentry.cleaning.mapper.CleaningPolicyMapper;
import com.touhouqing.datasentry.cleaning.model.CleaningBinding;
import com.touhouqing.datasentry.cleaning.model.CleaningPolicy;
import com.touhouqing.datasentry.exception.InvalidInputException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CleaningBindingService {

	private final CleaningBindingMapper bindingMapper;

	private final CleaningPolicyMapper policyMapper;

	public CleaningBinding getOnlineDefaultBinding(Long agentId) {
		if (agentId == null) {
			throw new InvalidInputException("agentId 不能为空");
		}
		return bindingMapper.findDefaultByAgent(agentId, CleaningBindingType.ONLINE_TEXT.name());
	}

	public CleaningBinding upsertOnlineDefaultBinding(CleaningBindingRequest request) {
		if (request == null || request.getAgentId() == null) {
			throw new InvalidInputException("agentId 不能为空");
		}
		if (request.getPolicyId() == null) {
			throw new InvalidInputException("policyId 不能为空");
		}
		CleaningPolicy policy = policyMapper.selectById(request.getPolicyId());
		if (policy == null || policy.getEnabled() == null || policy.getEnabled() != 1) {
			throw new InvalidInputException("策略不存在或未启用");
		}
		CleaningBinding existing = bindingMapper.findDefaultByAgent(request.getAgentId(),
				CleaningBindingType.ONLINE_TEXT.name());
		LocalDateTime now = LocalDateTime.now();
		int enabled = request.getEnabled() != null ? request.getEnabled() : 1;
		if (existing == null) {
			CleaningBinding binding = CleaningBinding.builder()
				.agentId(request.getAgentId())
				.bindingType(CleaningBindingType.ONLINE_TEXT.name())
				.scene(null)
				.policyId(request.getPolicyId())
				.enabled(enabled)
				.createdTime(now)
				.updatedTime(now)
				.build();
			bindingMapper.insert(binding);
			return binding;
		}
		bindingMapper.update(null,
				new LambdaUpdateWrapper<CleaningBinding>().eq(CleaningBinding::getId, existing.getId())
					.set(CleaningBinding::getPolicyId, request.getPolicyId())
					.set(CleaningBinding::getEnabled, enabled)
					.set(CleaningBinding::getUpdatedTime, now));
		return bindingMapper
			.selectOne(new LambdaQueryWrapper<CleaningBinding>().eq(CleaningBinding::getId, existing.getId()));
	}

}
