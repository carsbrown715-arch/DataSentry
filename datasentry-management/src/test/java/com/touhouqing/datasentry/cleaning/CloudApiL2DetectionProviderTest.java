package com.touhouqing.datasentry.cleaning;

import com.sun.net.httpserver.HttpServer;
import com.touhouqing.datasentry.cleaning.detector.CloudApiL2DetectionProvider;
import com.touhouqing.datasentry.cleaning.model.CleaningPolicyConfig;
import com.touhouqing.datasentry.cleaning.model.CleaningRule;
import com.touhouqing.datasentry.cleaning.model.Finding;
import com.touhouqing.datasentry.cleaning.service.CleaningOpsStateService;
import com.touhouqing.datasentry.properties.DataSentryProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class CloudApiL2DetectionProviderTest {

	private HttpServer server;

	@AfterEach
	public void tearDown() {
		if (server != null) {
			server.stop(0);
		}
	}

	@Test
	public void shouldUseInjectedHttpClientInstance() throws Exception {
		DataSentryProperties properties = new DataSentryProperties();
		CleaningOpsStateService opsStateService = new CleaningOpsStateService();
		HttpClient httpClient = HttpClient.newHttpClient();
		CloudApiL2DetectionProvider provider = new CloudApiL2DetectionProvider(properties, opsStateService, httpClient);

		Field field = CloudApiL2DetectionProvider.class.getDeclaredField("cleaningCloudHttpClient");
		field.setAccessible(true);
		Assertions.assertSame(httpClient, field.get(provider));
	}

	@Test
	public void shouldReturnFindingWhenCloudScoreExceedsThreshold() throws Exception {
		String endpoint = startServer(200, "{\"score\":0.92,\"label\":\"RISK\"}");
		DataSentryProperties properties = new DataSentryProperties();
		properties.getCleaning().getL2().getCloudApi().setUrl(endpoint);
		properties.getCleaning().getL2().getCloudApi().setTimeoutMs(1000);
		properties.getCleaning().getL2().setThreshold(0.6);
		CleaningOpsStateService opsStateService = new CleaningOpsStateService();
		CloudApiL2DetectionProvider provider = new CloudApiL2DetectionProvider(properties, opsStateService,
				HttpClient.newHttpClient());

		List<Finding> findings = provider.detect("点击链接领取返利", CleaningRule.builder().category("RISK").build(),
				CleaningPolicyConfig.builder().l2Threshold(0.6).build());

		Assertions.assertEquals(1, findings.size());
		Assertions.assertEquals("L2_CLOUD_API", findings.get(0).getDetectorSource());
		Assertions.assertEquals(1L, opsStateService.getCloudInferenceSuccessCount());
	}

	@Test
	public void shouldReturnEmptyWhenCloudScoreBelowThreshold() throws Exception {
		String endpoint = startServer(200, "{\"data\":{\"score\":0.12,\"label\":\"SAFE\"}}");
		DataSentryProperties properties = new DataSentryProperties();
		properties.getCleaning().getL2().getCloudApi().setUrl(endpoint);
		properties.getCleaning().getL2().setThreshold(0.6);
		CleaningOpsStateService opsStateService = new CleaningOpsStateService();
		CloudApiL2DetectionProvider provider = new CloudApiL2DetectionProvider(properties, opsStateService,
				HttpClient.newHttpClient());

		List<Finding> findings = provider.detect("正常内容", CleaningRule.builder().category("RISK").build(),
				CleaningPolicyConfig.builder().l2Threshold(0.6).build());

		Assertions.assertTrue(findings.isEmpty());
		Assertions.assertEquals(1L, opsStateService.getCloudInferenceSuccessCount());
	}

	@Test
	public void shouldThrowWhenCloudApiHttpFailure() throws Exception {
		String endpoint = startServer(500, "{\"error\":\"down\"}");
		DataSentryProperties properties = new DataSentryProperties();
		properties.getCleaning().getL2().getCloudApi().setUrl(endpoint);
		CleaningOpsStateService opsStateService = new CleaningOpsStateService();
		CloudApiL2DetectionProvider provider = new CloudApiL2DetectionProvider(properties, opsStateService,
				HttpClient.newHttpClient());

		Assertions.assertThrows(IllegalStateException.class,
				() -> provider.detect("test", CleaningRule.builder().category("RISK").build(),
						CleaningPolicyConfig.builder().l2Threshold(0.1).build()));
		Assertions.assertEquals(1L, opsStateService.getCloudInferenceFailureCount());
	}

	private String startServer(int status, String responseBody) throws Exception {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/score", exchange -> {
			byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			exchange.sendResponseHeaders(status, bytes.length);
			try (OutputStream outputStream = exchange.getResponseBody()) {
				outputStream.write(bytes);
			}
		});
		server.start();
		int port = server.getAddress().getPort();
		return "http://127.0.0.1:" + port + "/score";
	}

}
