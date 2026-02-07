package com.touhouqing.datasentry.cleaning.service;

import com.touhouqing.datasentry.cleaning.model.CleaningAlertEvent;
import com.touhouqing.datasentry.properties.DataSentryProperties;
import com.touhouqing.datasentry.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class CleaningWebhookNotifier {

	private final DataSentryProperties dataSentryProperties;

	private final CleaningOpsStateService opsStateService;

	private final AtomicInteger currentMinuteCounter = new AtomicInteger(0);

	private volatile LocalDateTime currentMinute = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);

	public boolean notify(CleaningAlertEvent event) {
		DataSentryProperties.Cleaning.Notification.Webhook webhook = dataSentryProperties.getCleaning()
			.getNotification()
			.getWebhook();
		if (!webhook.isEnabled() || webhook.getUrl() == null || webhook.getUrl().isBlank() || event == null) {
			return false;
		}
		if (!allowByRateLimit(webhook.getRateLimitPerMinute())) {
			log.debug("Skip webhook notify by rate limit, code={}", event.getCode());
			return false;
		}
		String body = buildBody(event);
		boolean success = postWithRetry(webhook, body);
		if (success) {
			opsStateService.markWebhookPushSuccess();
		}
		else {
			opsStateService.markWebhookPushFailure();
		}
		return success;
	}

	private boolean postWithRetry(DataSentryProperties.Cleaning.Notification.Webhook webhook, String body) {
		HttpClient client = HttpClient.newBuilder()
			.connectTimeout(Duration.ofMillis(Math.max(webhook.getTimeoutMs(), 1000)))
			.build();
		int retryTimes = Math.max(0, webhook.getRetryTimes());
		for (int attempt = 0; attempt <= retryTimes; attempt++) {
			try {
				HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
					.uri(URI.create(webhook.getUrl()))
					.timeout(Duration.ofMillis(Math.max(webhook.getTimeoutMs(), 1000)))
					.header("Content-Type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
				String signature = signBody(webhook.getSecret(), body);
				if (signature != null) {
					requestBuilder.header("X-DataSentry-Signature", signature);
				}
				HttpResponse<String> response = client.send(requestBuilder.build(),
						HttpResponse.BodyHandlers.ofString());
				if (response.statusCode() >= 200 && response.statusCode() < 300) {
					return true;
				}
				log.warn("Webhook notify failed, status={} body={}", response.statusCode(), response.body());
			}
			catch (Exception e) {
				log.warn("Webhook notify exception on attempt {}", attempt + 1, e);
			}
		}
		return false;
	}

	private String buildBody(CleaningAlertEvent event) {
		Map<String, Object> payload = new HashMap<>();
		payload.put("level", event.getLevel());
		payload.put("code", event.getCode());
		payload.put("message", event.getMessage());
		payload.put("createdTime", event.getCreatedTime() != null ? event.getCreatedTime() : LocalDateTime.now());
		payload.put("payload", event.getPayload() != null ? event.getPayload() : Map.of());
		try {
			return JsonUtil.getObjectMapper().writeValueAsString(payload);
		}
		catch (Exception e) {
			return "{}";
		}
	}

	private String signBody(String secret, String body) {
		if (secret == null || secret.isBlank()) {
			return null;
		}
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hashed = digest.digest((secret + body).getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder();
			for (byte b : hashed) {
				sb.append(String.format("%02x", b));
			}
			return sb.toString();
		}
		catch (Exception e) {
			return null;
		}
	}

	private boolean allowByRateLimit(int limitPerMinute) {
		if (limitPerMinute <= 0) {
			return true;
		}
		LocalDateTime minute = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
		if (!minute.equals(currentMinute)) {
			synchronized (this) {
				if (!minute.equals(currentMinute)) {
					currentMinute = minute;
					currentMinuteCounter.set(0);
				}
			}
		}
		return currentMinuteCounter.incrementAndGet() <= limitPerMinute;
	}

}
