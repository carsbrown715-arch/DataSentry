package com.touhouqing.datasentry.cleaning.util;

import com.touhouqing.datasentry.cleaning.enums.CleaningAllowlistType;
import com.touhouqing.datasentry.cleaning.model.CleaningAllowlist;
import com.touhouqing.datasentry.cleaning.model.Finding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class CleaningAllowlistMatcher {

	private CleaningAllowlistMatcher() {
	}

	public static List<Finding> filterFindings(String text, List<Finding> findings,
			List<CleaningAllowlist> allowlists) {
		if (findings == null || findings.isEmpty() || allowlists == null || allowlists.isEmpty()) {
			return findings;
		}
		List<Finding> filtered = new ArrayList<>();
		for (Finding finding : findings) {
			if (!isAllowlisted(text, finding, allowlists)) {
				filtered.add(finding);
			}
		}
		return filtered;
	}

	private static boolean isAllowlisted(String text, Finding finding, List<CleaningAllowlist> allowlists) {
		String matchedText = extractMatchedText(text, finding);
		if (matchedText == null) {
			return false;
		}
		for (CleaningAllowlist allowlist : allowlists) {
			if (!categoryMatch(finding.getCategory(), allowlist.getCategory())) {
				continue;
			}
			if (matches(allowlist, matchedText)) {
				return true;
			}
		}
		return false;
	}

	private static boolean categoryMatch(String findingCategory, String allowlistCategory) {
		if (allowlistCategory == null || allowlistCategory.isBlank()) {
			return true;
		}
		return allowlistCategory.equalsIgnoreCase(findingCategory);
	}

	private static String extractMatchedText(String text, Finding finding) {
		if (text == null || finding.getStart() == null || finding.getEnd() == null) {
			return null;
		}
		int start = finding.getStart();
		int end = finding.getEnd();
		if (start < 0 || end <= start || end > text.length()) {
			return null;
		}
		return text.substring(start, end);
	}

	private static boolean matches(CleaningAllowlist allowlist, String value) {
		if (allowlist.getValue() == null) {
			return false;
		}
		String type = allowlist.getType();
		if (type == null) {
			return false;
		}
		try {
			CleaningAllowlistType allowlistType = CleaningAllowlistType.valueOf(type.toUpperCase(Locale.ROOT));
			switch (allowlistType) {
				case EXACT:
					return value.equals(allowlist.getValue());
				case CONTAINS:
					return value.contains(allowlist.getValue());
				case PREFIX:
					return value.startsWith(allowlist.getValue());
				case SUFFIX:
					return value.endsWith(allowlist.getValue());
				case REGEX:
					try {
						return Pattern.compile(allowlist.getValue()).matcher(value).find();
					}
					catch (Exception e) {
						return false;
					}
				default:
					return false;
			}
		}
		catch (IllegalArgumentException e) {
			return false;
		}
	}

}
