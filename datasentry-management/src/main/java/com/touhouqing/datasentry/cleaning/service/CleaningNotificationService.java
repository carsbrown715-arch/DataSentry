package com.touhouqing.datasentry.cleaning.service;

import com.touhouqing.datasentry.cleaning.model.CleaningAlertEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.Executor;

@Slf4j
@Service
public class CleaningNotificationService {

	private final Executor notificationExecutor;

	private final CleaningWebhookNotifier webhookNotifier;

	public CleaningNotificationService(@Qualifier("cleaningNotificationExecutor") Executor notificationExecutor,
			CleaningWebhookNotifier webhookNotifier) {
		this.notificationExecutor = notificationExecutor;
		this.webhookNotifier = webhookNotifier;
	}

	public void notifyAsync(CleaningAlertEvent event) {
		if (event == null) {
			return;
		}
		try {
			notificationExecutor.execute(() -> webhookNotifier.notify(event));
		}
		catch (Exception e) {
			log.debug("Discard notify event code={}", event.getCode(), e);
		}
	}

	public void notifyAsync(String level, String code, String message, Map<String, Object> payload) {
		notifyAsync(CleaningAlertEvent.builder()
			.level(level)
			.code(code)
			.message(message)
			.payload(payload)
			.createdTime(LocalDateTime.now())
			.build());
	}

}
