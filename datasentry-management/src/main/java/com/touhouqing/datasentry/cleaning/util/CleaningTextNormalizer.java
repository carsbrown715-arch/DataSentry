package com.touhouqing.datasentry.cleaning.util;

public final class CleaningTextNormalizer {

	private CleaningTextNormalizer() {
	}

	public static String normalize(String text) {
		if (text == null) {
			return "";
		}
		String normalized = text.replace("\uFEFF", " ");
		normalized = normalized.replace("\u200B", " ")
			.replace("\u200C", " ")
			.replace("\u200D", " ")
			.replace("\u2060", " ");
		return normalized;
	}

}
