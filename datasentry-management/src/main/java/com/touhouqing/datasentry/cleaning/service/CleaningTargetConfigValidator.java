package com.touhouqing.datasentry.cleaning.service;

import com.touhouqing.datasentry.exception.InvalidInputException;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class CleaningTargetConfigValidator {

	public static final String TYPE_COLUMNS = "COLUMNS";

	public static final String TYPE_JSONPATH = "JSONPATH";

	public String resolveType(String targetConfigType) {
		if (targetConfigType == null || targetConfigType.isBlank()) {
			return TYPE_COLUMNS;
		}
		String normalized = targetConfigType.trim().toUpperCase();
		if (!TYPE_COLUMNS.equals(normalized) && !TYPE_JSONPATH.equals(normalized)) {
			throw new InvalidInputException("targetConfigType 仅支持 COLUMNS 或 JSONPATH");
		}
		return normalized;
	}

	public Map<String, String> normalizeJsonPathMappings(String resolvedType, List<String> targetColumns,
			Map<String, Object> targetConfig) {
		if (!TYPE_JSONPATH.equals(resolvedType)) {
			return Map.of();
		}
		if (targetConfig == null || targetConfig.isEmpty()) {
			throw new InvalidInputException("JSONPATH 模式必须提供 targetConfig");
		}
		Map<String, String> normalized = new LinkedHashMap<>();
		for (String column : targetColumns) {
			Object value = targetConfig.get(column);
			if (!(value instanceof String path) || path.isBlank()) {
				throw new InvalidInputException("JSONPATH 模式下每个目标列都必须配置 jsonPath: " + column);
			}
			String trimmed = path.trim();
			if (!trimmed.startsWith("$.")) {
				throw new InvalidInputException("非法 jsonPath（必须以 $. 开头）: " + column);
			}
			normalized.put(column, trimmed);
		}
		return normalized;
	}

}
