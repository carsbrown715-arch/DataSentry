package com.touhouqing.datasentry.cleaning.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.touhouqing.datasentry.cleaning.mapper.CleaningPolicyMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.touhouqing.datasentry.cleaning.mapper.CleaningPolicyVersionMapper;
import com.touhouqing.datasentry.cleaning.mapper.CleaningPolicyRuleMapper;
import com.touhouqing.datasentry.cleaning.mapper.CleaningRuleMapper;
import com.touhouqing.datasentry.cleaning.model.CleaningPolicy;
import com.touhouqing.datasentry.cleaning.model.CleaningPolicyConfig;
import com.touhouqing.datasentry.cleaning.model.CleaningPolicySnapshot;
import com.touhouqing.datasentry.cleaning.model.CleaningPolicyRule;
import com.touhouqing.datasentry.cleaning.model.CleaningPolicyVersion;
import com.touhouqing.datasentry.cleaning.model.CleaningRule;
import com.touhouqing.datasentry.exception.InvalidInputException;
import com.touhouqing.datasentry.properties.DataSentryProperties;
import com.touhouqing.datasentry.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CleaningPolicyResolver {

	private final CleaningPolicyMapper policyMapper;

	private final CleaningPolicyRuleMapper policyRuleMapper;

	private final CleaningRuleMapper ruleMapper;

	private final CleaningPolicyVersionMapper policyVersionMapper;

	private final DataSentryProperties dataSentryProperties;

	public CleaningPolicyResolver(CleaningPolicyMapper policyMapper, CleaningPolicyRuleMapper policyRuleMapper,
			CleaningRuleMapper ruleMapper, CleaningPolicyVersionMapper policyVersionMapper,
			DataSentryProperties dataSentryProperties) {
		this.policyMapper = policyMapper;
		this.policyRuleMapper = policyRuleMapper;
		this.ruleMapper = ruleMapper;
		this.policyVersionMapper = policyVersionMapper;
		this.dataSentryProperties = dataSentryProperties;
	}

	public CleaningPolicyResolver(CleaningPolicyMapper policyMapper, CleaningPolicyRuleMapper policyRuleMapper,
			CleaningRuleMapper ruleMapper) {
		this(policyMapper, policyRuleMapper, ruleMapper, null, new DataSentryProperties());
	}

	public CleaningPolicySnapshot resolveSnapshot(Long policyId) {
		CleaningPolicyVersion publishedVersion = resolvePublishedVersion(policyId);
		if (publishedVersion != null) {
			return resolveSnapshotFromVersion(policyId, publishedVersion);
		}
		CleaningPolicy policy = policyMapper.selectById(policyId);
		if (policy == null || policy.getEnabled() == null || policy.getEnabled() != 1) {
			throw new InvalidInputException("清理策略不可用");
		}
		List<CleaningPolicyRule> policyRules = policyRuleMapper
			.selectList(new LambdaQueryWrapper<CleaningPolicyRule>().eq(CleaningPolicyRule::getPolicyId, policyId)
				.orderByDesc(CleaningPolicyRule::getPriority)
				.orderByAsc(CleaningPolicyRule::getRuleId));
		List<Long> ruleIds = policyRules.stream()
			.map(CleaningPolicyRule::getRuleId)
			.filter(Objects::nonNull)
			.collect(Collectors.toList());
		List<CleaningRule> rules = List.of();
		if (!ruleIds.isEmpty()) {
			List<CleaningRule> fetched = ruleMapper
				.selectList(new LambdaQueryWrapper<CleaningRule>().in(CleaningRule::getId, ruleIds)
					.eq(CleaningRule::getEnabled, 1));
			Map<Long, CleaningRule> ruleMap = fetched.stream()
				.collect(Collectors.toMap(CleaningRule::getId, rule -> rule, (first, second) -> first));
			rules = policyRules.stream()
				.map(rule -> ruleMap.get(rule.getRuleId()))
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
		}
		CleaningPolicyConfig config = parsePolicyConfig(policy.getConfigJson());
		return CleaningPolicySnapshot.builder()
			.policyId(policy.getId())
			.policyVersionId(null)
			.policyVersionNo(null)
			.policyName(policy.getName())
			.defaultAction(policy.getDefaultAction())
			.config(config)
			.rules(rules)
			.build();
	}

	private CleaningPolicySnapshot resolveSnapshotFromVersion(Long policyId, CleaningPolicyVersion version) {
		CleaningPolicy policy = policyMapper.selectById(policyId);
		if (policy == null || policy.getEnabled() == null || policy.getEnabled() != 1) {
			throw new InvalidInputException("清理策略不可用");
		}
		List<CleaningPolicyRule> policyRules = policyRuleMapper
			.selectList(new LambdaQueryWrapper<CleaningPolicyRule>().eq(CleaningPolicyRule::getPolicyId, policyId)
				.orderByDesc(CleaningPolicyRule::getPriority)
				.orderByAsc(CleaningPolicyRule::getRuleId));
		List<Long> ruleIds = policyRules.stream()
			.map(CleaningPolicyRule::getRuleId)
			.filter(Objects::nonNull)
			.collect(Collectors.toList());
		List<CleaningRule> rules = List.of();
		if (!ruleIds.isEmpty()) {
			List<CleaningRule> fetched = ruleMapper
				.selectList(new LambdaQueryWrapper<CleaningRule>().in(CleaningRule::getId, ruleIds)
					.eq(CleaningRule::getEnabled, 1));
			Map<Long, CleaningRule> ruleMap = fetched.stream()
				.collect(Collectors.toMap(CleaningRule::getId, rule -> rule, (first, second) -> first));
			rules = policyRules.stream().map(rule -> ruleMap.get(rule.getRuleId())).filter(Objects::nonNull).toList();
		}
		CleaningPolicyConfig config = parseVersionConfig(version.getConfigJson(), policy.getConfigJson());
		String defaultAction = version.getDefaultAction() != null ? version.getDefaultAction()
				: policy.getDefaultAction();
		return CleaningPolicySnapshot.builder()
			.policyId(policy.getId())
			.policyVersionId(version.getId())
			.policyVersionNo(version.getVersionNo())
			.policyName(policy.getName())
			.defaultAction(defaultAction)
			.config(config)
			.rules(rules)
			.build();
	}

	private CleaningPolicyVersion resolvePublishedVersion(Long policyId) {
		if (policyVersionMapper == null || dataSentryProperties == null) {
			return null;
		}
		if (!dataSentryProperties.getCleaning().isPolicyGovernanceEnabled()) {
			return null;
		}
		return policyVersionMapper.findPublished(policyId);
	}

	private CleaningPolicyConfig parseVersionConfig(String versionConfigJson, String fallbackConfigJson) {
		if (versionConfigJson != null && !versionConfigJson.isBlank()) {
			try {
				Map<String, Object> parsed = JsonUtil.getObjectMapper().readValue(versionConfigJson, Map.class);
				Object policyConfigJson = parsed.get("policyConfigJson");
				if (policyConfigJson instanceof String configText && !configText.isBlank()) {
					return parsePolicyConfig(configText);
				}
			}
			catch (Exception e) {
				log.warn("Failed to parse cleaning policy version config", e);
			}
		}
		return parsePolicyConfig(fallbackConfigJson);
	}

	private CleaningPolicyConfig parsePolicyConfig(String configJson) {
		if (configJson == null || configJson.isBlank()) {
			return new CleaningPolicyConfig();
		}
		try {
			CleaningPolicyConfig config = JsonUtil.getObjectMapper().readValue(configJson, CleaningPolicyConfig.class);
			return config != null ? config : new CleaningPolicyConfig();
		}
		catch (JsonProcessingException e) {
			log.warn("Failed to parse cleaning policy config", e);
			return new CleaningPolicyConfig();
		}
	}

}
