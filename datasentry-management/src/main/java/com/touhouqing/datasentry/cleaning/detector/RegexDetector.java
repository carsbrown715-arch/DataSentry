package com.touhouqing.datasentry.cleaning.detector;

import com.touhouqing.datasentry.cleaning.model.CleaningRule;
import com.touhouqing.datasentry.cleaning.model.Finding;
import com.touhouqing.datasentry.cleaning.model.RegexRuleConfig;
import com.touhouqing.datasentry.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class RegexDetector {

	private static final Map<String, Pattern> PATTERN_CACHE = new ConcurrentHashMap<>();

	public List<Finding> detect(String text, CleaningRule rule) {
		if (text == null || text.isEmpty() || rule == null || rule.getConfigJson() == null) {
			return List.of();
		}
		RegexRuleConfig config = parseConfig(rule.getConfigJson(), rule.getId());
		if (config == null || config.getPattern() == null || config.getPattern().isBlank()) {
			return List.of();
		}
		Pattern pattern = compilePattern(config);
		if (pattern == null) {
			return List.of();
		}
		Matcher matcher = pattern.matcher(text);
		List<Finding> findings = new ArrayList<>();
		while (matcher.find()) {
			findings.add(Finding.builder()
				.type(rule.getCategory())
				.category(rule.getCategory())
				.severity(rule.getSeverity() != null ? rule.getSeverity() : 0.8)
				.start(matcher.start())
				.end(matcher.end())
				.detectorSource("L1_REGEX")
				.build());
		}
		return findings;
	}

	private RegexRuleConfig parseConfig(String configJson, Long ruleId) {
		try {
			return JsonUtil.getObjectMapper().readValue(configJson, RegexRuleConfig.class);
		}
		catch (JsonProcessingException e) {
			log.warn("Failed to parse regex rule config for rule {}", ruleId, e);
			return null;
		}
	}

	private Pattern compilePattern(RegexRuleConfig config) {
		String key = config.getPattern() + "#" + config.getFlags();
		return PATTERN_CACHE.computeIfAbsent(key, k -> {
			try {
				int flags = parseFlags(config.getFlags());
				return Pattern.compile(config.getPattern(), flags);
			}
			catch (Exception e) {
				log.warn("Failed to compile regex pattern: {}", config.getPattern(), e);
				return null;
			}
		});
	}

	private int parseFlags(String flags) {
		if (flags == null || flags.isBlank()) {
			return 0;
		}
		int result = 0;
		String[] parts = flags.split(",");
		for (String part : parts) {
			String flag = part.trim().toUpperCase();
			switch (flag) {
				case "CASE_INSENSITIVE":
					result |= Pattern.CASE_INSENSITIVE;
					break;
				case "MULTILINE":
					result |= Pattern.MULTILINE;
					break;
				case "DOTALL":
					result |= Pattern.DOTALL;
					break;
				default:
					break;
			}
		}
		return result;
	}

}
