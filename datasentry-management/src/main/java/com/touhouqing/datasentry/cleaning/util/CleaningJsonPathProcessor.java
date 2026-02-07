package com.touhouqing.datasentry.cleaning.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.touhouqing.datasentry.util.JsonUtil;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CleaningJsonPathProcessor {

	public String extractText(String json, String jsonPath) {
		if (json == null || json.isBlank() || jsonPath == null || jsonPath.isBlank()) {
			return null;
		}
		try {
			JsonNode root = JsonUtil.getObjectMapper().readTree(json);
			List<PathStep> steps = parseSteps(jsonPath);
			if (steps.isEmpty()) {
				return null;
			}
			JsonNode current = root;
			for (PathStep step : steps) {
				if (step.fieldName() != null) {
					if (!(current instanceof ObjectNode)) {
						return null;
					}
					current = current.get(step.fieldName());
				}
				if (current == null) {
					return null;
				}
				if (step.arrayIndex() != null) {
					if (!(current instanceof ArrayNode arrayNode) || arrayNode.isEmpty()) {
						return null;
					}
					int index = step.arrayIndex();
					if (index < 0 || index >= arrayNode.size()) {
						return null;
					}
					current = arrayNode.get(index);
				}
			}
			if (current == null || current.isNull() || current.isContainerNode()) {
				return null;
			}
			return current.asText();
		}
		catch (Exception e) {
			return null;
		}
	}

	public String replaceText(String json, String jsonPath, String replacement) {
		if (json == null || json.isBlank() || jsonPath == null || jsonPath.isBlank()) {
			return null;
		}
		try {
			JsonNode root = JsonUtil.getObjectMapper().readTree(json);
			List<PathStep> steps = parseSteps(jsonPath);
			if (steps.isEmpty()) {
				return null;
			}
			JsonNode current = root;
			for (int index = 0; index < steps.size() - 1; index++) {
				PathStep step = steps.get(index);
				if (step.fieldName() != null) {
					if (!(current instanceof ObjectNode)) {
						return null;
					}
					current = current.get(step.fieldName());
				}
				if (current == null) {
					return null;
				}
				if (step.arrayIndex() != null) {
					if (!(current instanceof ArrayNode arrayNode) || arrayNode.isEmpty()) {
						return null;
					}
					int position = step.arrayIndex();
					if (position < 0 || position >= arrayNode.size()) {
						return null;
					}
					current = arrayNode.get(position);
				}
			}
			PathStep last = steps.get(steps.size() - 1);
			if (last.fieldName() != null) {
				if (!(current instanceof ObjectNode objectNode)) {
					return null;
				}
				JsonNode target = objectNode.get(last.fieldName());
				if (target == null) {
					return null;
				}
				if (last.arrayIndex() == null) {
					objectNode.set(last.fieldName(), TextNode.valueOf(replacement));
				}
				else {
					if (!(target instanceof ArrayNode arrayNode) || arrayNode.isEmpty()) {
						return null;
					}
					int position = last.arrayIndex();
					if (position < 0 || position >= arrayNode.size()) {
						return null;
					}
					arrayNode.set(position, TextNode.valueOf(replacement));
				}
			}
			else {
				if (!(current instanceof ArrayNode arrayNode) || arrayNode.isEmpty() || last.arrayIndex() == null) {
					return null;
				}
				int position = last.arrayIndex();
				if (position < 0 || position >= arrayNode.size()) {
					return null;
				}
				arrayNode.set(position, TextNode.valueOf(replacement));
			}
			return JsonUtil.getObjectMapper().writeValueAsString(root);
		}
		catch (Exception e) {
			return null;
		}
	}

	private List<PathStep> parseSteps(String jsonPath) {
		String path = jsonPath.trim();
		if (!path.startsWith("$.")) {
			return List.of();
		}
		String expr = path.substring(2);
		if (expr.isBlank()) {
			return List.of();
		}
		String[] segments = expr.split("\\.");
		List<PathStep> steps = new ArrayList<>();
		for (String rawSegment : segments) {
			if (rawSegment == null || rawSegment.isBlank()) {
				return List.of();
			}
			String segment = rawSegment.trim();
			String fieldName = segment;
			Integer arrayIndex = null;
			int bracketStart = segment.indexOf('[');
			if (bracketStart >= 0) {
				int bracketEnd = segment.indexOf(']', bracketStart);
				if (bracketEnd <= bracketStart) {
					return List.of();
				}
				fieldName = bracketStart > 0 ? segment.substring(0, bracketStart) : null;
				String indexPart = segment.substring(bracketStart + 1, bracketEnd);
				if ("*".equals(indexPart)) {
					arrayIndex = 0;
				}
				else {
					try {
						arrayIndex = Integer.parseInt(indexPart);
					}
					catch (NumberFormatException e) {
						return List.of();
					}
				}
			}
			steps.add(new PathStep(fieldName, arrayIndex));
		}
		return steps;
	}

	private record PathStep(String fieldName, Integer arrayIndex) {
	}

}
