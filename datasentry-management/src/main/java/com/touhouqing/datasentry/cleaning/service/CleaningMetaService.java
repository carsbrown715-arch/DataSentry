package com.touhouqing.datasentry.cleaning.service;

import com.touhouqing.datasentry.cleaning.dto.CleaningOptionItemView;
import com.touhouqing.datasentry.cleaning.dto.CleaningOptionMetaView;
import com.touhouqing.datasentry.cleaning.dto.CleaningThresholdItemView;
import com.touhouqing.datasentry.cleaning.model.CleaningPolicyConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CleaningMetaService {

	public CleaningOptionMetaView getOptions() {
		CleaningPolicyConfig defaultPolicyConfig = new CleaningPolicyConfig();

		return CleaningOptionMetaView.builder()
			.defaultActions(defaultActions())
			.ruleTypes(ruleTypes())
			.ruleCategories(ruleCategories())
			.reviewPolicies(reviewPolicies())
			.jobModes(jobModes())
			.writebackModes(writebackModes())
			.runStatuses(runStatuses())
			.verdicts(verdicts())
			.targetConfigTypes(targetConfigTypes())
			.thresholdGuidance(thresholdGuidance(defaultPolicyConfig))
			.jsonConfigTemplates(jsonConfigTemplates())
			.fieldHelp(fieldHelp())
			.build();
	}

	private List<CleaningOptionItemView> defaultActions() {
		return List.of(option("DETECT_ONLY", "仅检测", "只返回风险结果，不做脱敏和写回", "上线初期最稳妥", "低", "新手默认"),
				option("SANITIZE_RETURN", "检测并返回脱敏文本", "返回脱敏后的文本，不写回数据库", "适合在线接口返回", "中", "在线清理"),
				option("SANITIZE_WRITEBACK", "脱敏后写回", "将脱敏结果写回目标表", "批处理治理常用", "较高", "存量治理"),
				option("REVIEW_THEN_WRITEBACK", "先人审再写回", "命中风险先进入人审，通过后再写回", "误杀风险可控", "中", "高敏业务"),
				option("DELETE", "删除", "命中后删除或软删除数据", "需严格权限控制", "高", "合规删除场景"));
	}

	private List<CleaningOptionItemView> ruleTypes() {
		return List.of(
				CleaningOptionItemView.builder()
					.code("REGEX")
					.labelZh("正则规则（L1）")
					.description("通过正则表达式快速识别手机号、邮箱等模式")
					.configSchemaHint("{\"pattern\":\"...\",\"flags\":\"CASE_INSENSITIVE\"}")
					.sampleConfig(regexConfigTemplate())
					.build(),
				CleaningOptionItemView.builder()
					.code("L2_DUMMY")
					.labelZh("轻量模型规则（L2）")
					.description("使用轻量模型打分，适合灰度验证和成本控制")
					.configSchemaHint("{}")
					.sampleConfig(Map.of())
					.build(),
				CleaningOptionItemView.builder()
					.code("LLM")
					.labelZh("大模型规则（L3）")
					.description("复杂语义场景使用大模型检测，成本较高")
					.configSchemaHint("{}")
					.sampleConfig(Map.of())
					.build());
	}

	private List<CleaningOptionItemView> ruleCategories() {
		return List.of(option("PII", "个人隐私", "手机号、身份证、邮箱等个人信息"), option("SPAM", "垃圾营销", "广告、拉群、导流、欺诈等"),
				option("SECURITY", "安全风险", "木马链接、钓鱼、注入等攻击内容"), option("COMPLIANCE", "合规风险", "违规用语、监管敏感内容"));
	}

	private List<CleaningOptionItemView> reviewPolicies() {
		return List.of(option("NEVER", "不进入人审", "自动执行策略动作"), option("ON_RISK", "有风险时人审", "命中风险再进入人审队列"),
				option("ALWAYS", "总是人审", "每条都需人工确认，最稳妥"));
	}

	private List<CleaningOptionItemView> jobModes() {
		return List.of(option("DRY_RUN", "试运行", "只检测与评估，不落库写回"), option("WRITEBACK", "正式写回", "按照写回模式执行更新或软删"));
	}

	private List<CleaningOptionItemView> writebackModes() {
		return List.of(optionWithCaution("NONE", "不写回", "仅生成检测结果", null),
				optionWithCaution("UPDATE", "更新写回", "将脱敏后的字段更新回原表", "建议先在试运行验证"),
				optionWithCaution("SOFT_DELETE", "软删除", "将记录标记为已删除状态", "需确认业务有软删字段"));
	}

	private List<CleaningOptionItemView> runStatuses() {
		return List.of(option("QUEUED", "排队中", "等待调度执行"), option("RUNNING", "运行中", "任务正在处理"),
				option("PAUSED", "已暂停", "任务暂停，可恢复"), option("SUCCEEDED", "已成功", "任务执行完成"),
				option("FAILED", "失败", "任务执行失败"), option("CANCELED", "已取消", "任务被取消"));
	}

	private List<CleaningOptionItemView> verdicts() {
		return List.of(option("ALLOW", "放行", "未命中风险规则"), option("BLOCK", "拦截", "风险分超过拦截阈值"),
				option("REVIEW", "待人审", "风险分处于人审区间"), option("REDACTED", "已脱敏", "内容已按策略脱敏"));
	}

	private List<CleaningOptionItemView> targetConfigTypes() {
		return List.of(option("COLUMNS", "列模式", "直接对目标列整列清理"), option("JSONPATH", "JSONPath 模式", "针对 JSON 字段内部路径定点清理"));
	}

	private List<CleaningThresholdItemView> thresholdGuidance(CleaningPolicyConfig defaultPolicyConfig) {
		return List.of(
				threshold("blockThreshold", "Block 阈值", defaultPolicyConfig.resolvedBlockThreshold(), "风险分大于等于该值时直接拦截",
						"0.70 - 0.90"),
				threshold("reviewThreshold", "Review 阈值", defaultPolicyConfig.resolvedReviewThreshold(),
						"风险分处于 review 与 block 之间时进入人审", "0.35 - 0.65"),
				threshold("l2Threshold", "L2 阈值", defaultPolicyConfig.resolvedL2Threshold(), "轻量模型判定为风险的最小分数",
						"0.50 - 0.75"),
				threshold("shadowSampleRatio", "Shadow 采样率", defaultPolicyConfig.resolvedShadowSampleRatio(),
						"影子流量比例，只做观测不影响主流程", "0.00 - 0.20"));
	}

	private Map<String, Object> jsonConfigTemplates() {
		Map<String, Object> templates = new LinkedHashMap<>();
		templates.put("REGEX", regexConfigTemplate());
		templates.put("L2_DUMMY", Map.of());
		templates.put("LLM", Map.of());
		return templates;
	}

	private Map<String, Object> regexConfigTemplate() {
		Map<String, Object> template = new LinkedHashMap<>();
		template.put("pattern", "\\\\d{11}");
		template.put("flags", "CASE_INSENSITIVE");
		return template;
	}

	private Map<String, String> fieldHelp() {
		Map<String, String> help = new LinkedHashMap<>();
		help.put("policy.defaultAction", "默认动作是策略兜底行为，规则命中后按该动作执行");
		help.put("policy.ruleType", "规则类型决定检测层级：REGEX 快、L2 成本低、LLM 语义强");
		help.put("policy.configJson", "配置 JSON 用于给规则传入参数，如正则 pattern 与 flags");
		help.put("policy.threshold", "阈值越低越严格，命中率更高但误杀风险也会提升");
		help.put("policy.shadow", "Shadow 为影子模式，只观测不影响主决策");
		help.put("job.targetConfigType", "列模式直接清理列值；JSONPath 模式只清理 JSON 子字段");
		help.put("job.reviewPolicy", "人审策略决定何时进入人工审核队列");
		help.put("job.mode", "试运行仅评估，正式写回会修改业务数据");
		help.put("job.whereSql", "建议仅写筛选条件，避免更新/删除等高风险语句");
		help.put("job.budget", "预算用于控制成本，超硬阈值后任务会暂停或拒绝继续");
		help.put("realtime.action", "check 只检测；sanitize 会返回脱敏文本");
		help.put("realtime.scene", "业务场景可用于策略细分和效果观测");
		help.put("realtime.policyId", "可指定某个策略验证，不填则走默认策略");
		help.put("realtime.verdict", "判定结果：ALLOW 放行，REVIEW 人审，BLOCK 拦截");
		return help;
	}

	private CleaningOptionItemView option(String code, String labelZh, String description) {
		return CleaningOptionItemView.builder().code(code).labelZh(labelZh).description(description).build();
	}

	private CleaningOptionItemView option(String code, String labelZh, String description, String effect,
			String riskLevel, String recommendedFor) {
		return CleaningOptionItemView.builder()
			.code(code)
			.labelZh(labelZh)
			.description(description)
			.effect(effect)
			.riskLevel(riskLevel)
			.recommendedFor(recommendedFor)
			.build();
	}

	private CleaningOptionItemView optionWithCaution(String code, String labelZh, String description, String caution) {
		return CleaningOptionItemView.builder()
			.code(code)
			.labelZh(labelZh)
			.description(description)
			.caution(caution)
			.build();
	}

	private CleaningThresholdItemView threshold(String code, String labelZh, double defaultValue, String description,
			String recommendedRange) {
		return CleaningThresholdItemView.builder()
			.code(code)
			.labelZh(labelZh)
			.defaultValue(defaultValue)
			.description(description)
			.recommendedRange(recommendedRange)
			.build();
	}

}
