package com.touhouqing.datasentry.cleaning.config;

import com.touhouqing.datasentry.properties.DataSentryProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class CleaningCloudHttpClientConfig {

	@Bean
	public HttpClient cleaningCloudHttpClient(DataSentryProperties dataSentryProperties) {
		int connectTimeoutMs = dataSentryProperties.getCleaning().getL2().getCloudApi().getConnectTimeoutMs();
		return HttpClient.newBuilder()
			.connectTimeout(Duration.ofMillis(Math.max(connectTimeoutMs, 100)))
			.version(HttpClient.Version.HTTP_1_1)
			.build();
	}

}
