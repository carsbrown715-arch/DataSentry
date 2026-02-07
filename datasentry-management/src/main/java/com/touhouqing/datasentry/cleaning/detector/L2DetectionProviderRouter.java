package com.touhouqing.datasentry.cleaning.detector;

import com.touhouqing.datasentry.cleaning.model.CleaningPolicyConfig;
import com.touhouqing.datasentry.cleaning.model.CleaningRule;
import com.touhouqing.datasentry.cleaning.model.Finding;
import com.touhouqing.datasentry.cleaning.service.CleaningOpsStateService;
import com.touhouqing.datasentry.properties.DataSentryProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class L2DetectionProviderRouter {

	private final DummyL2DetectionProvider dummyProvider;

	private final OnnxL2DetectionProvider onnxProvider;

	private final CloudApiL2DetectionProvider cloudApiProvider;

	private final DataSentryProperties dataSentryProperties;

	private final CleaningOpsStateService opsStateService;

	@PostConstruct
	public void initStatus() {
		L2DetectionProvider provider = resolveProvider();
		if (!provider.isReady()) {
			opsStateService.setL2ProviderStatus(provider.name() + "/DEGRADED");
			return;
		}
		opsStateService.setL2ProviderStatus(provider.name() + "/OK");
	}

	public List<Finding> detect(String text, CleaningRule rule, CleaningPolicyConfig config) {
		L2DetectionProvider provider = resolveProvider();
		if (!provider.isReady()) {
			opsStateService.setL2ProviderStatus(provider.name() + "/DEGRADED");
			markFallback(provider);
			return dummyProvider.detect(text, rule, config);
		}
		try {
			List<Finding> result = provider.detect(text, rule, config);
			opsStateService.setL2ProviderStatus(provider.name() + "/OK");
			return result;
		}
		catch (Exception e) {
			log.warn("L2 provider failed: {}", provider.name(), e);
			opsStateService.setL2ProviderStatus(provider.name() + "/DEGRADED");
			if (provider == dummyProvider) {
				return List.of();
			}
			markFallback(provider);
			return dummyProvider.detect(text, rule, config);
		}
	}

	private void markFallback(L2DetectionProvider provider) {
		if (provider == onnxProvider) {
			opsStateService.markOnnxFallback();
		}
		if (provider == cloudApiProvider) {
			opsStateService.markCloudFallback();
		}
	}

	private L2DetectionProvider resolveProvider() {
		String provider = dataSentryProperties.getCleaning().getL2().getProvider();
		if ("ONNX".equalsIgnoreCase(provider)) {
			return onnxProvider;
		}
		if ("CLOUD_API".equalsIgnoreCase(provider) || "CLOUD".equalsIgnoreCase(provider)) {
			return cloudApiProvider;
		}
		return dummyProvider;
	}

}
