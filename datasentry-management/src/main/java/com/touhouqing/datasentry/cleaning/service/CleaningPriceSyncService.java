package com.touhouqing.datasentry.cleaning.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.touhouqing.datasentry.cleaning.dto.CleaningPricingSyncResult;
import com.touhouqing.datasentry.cleaning.mapper.CleaningPriceCatalogMapper;
import com.touhouqing.datasentry.cleaning.model.CleaningPriceCatalog;
import com.touhouqing.datasentry.entity.ModelConfig;
import com.touhouqing.datasentry.mapper.ModelConfigMapper;
import com.touhouqing.datasentry.properties.DataSentryProperties;
import com.touhouqing.datasentry.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CleaningPriceSyncService {

	private static final String CODE_PRICING_SYNC_FAILED = "PRICING_SYNC_FAILED";

	private static final String PRICING_SOURCE_MANUAL = "MANUAL";

	private final DataSentryProperties dataSentryProperties;

	private final CleaningPriceCatalogMapper priceCatalogMapper;

	private final ModelConfigMapper modelConfigMapper;

	private final CleaningOpsStateService opsStateService;

	private final CleaningNotificationService notificationService;

	private final CleaningPricingService pricingService;

	public CleaningPricingSyncResult syncNow(String reason) {
		LocalDateTime started = LocalDateTime.now();
		DataSentryProperties.Cleaning.Pricing pricing = dataSentryProperties.getCleaning().getPricing();
		String sourceType = normalizeSourceType(pricing.getSourceType());
		int inserted = 0;
		int updated = 0;
		int skipped = 0;
		try {
			List<DataSentryProperties.Cleaning.Pricing.PriceItem> sourceItems = loadSourceItems(sourceType);
			for (DataSentryProperties.Cleaning.Pricing.PriceItem item : sourceItems) {
				if (item == null || isBlank(item.getProvider()) || isBlank(item.getModel())) {
					skipped++;
					continue;
				}
				if (isManualPricingProtected(item.getProvider(), item.getModel())) {
					log.info("Skip pricing sync for manual model config provider={} model={}", item.getProvider(),
							item.getModel());
					skipped++;
					continue;
				}
				CleaningPriceCatalog existed = priceCatalogMapper.findLatestByProviderAndModel(item.getProvider(),
						item.getModel());
				BigDecimal input = safePrice(item.getInputPricePer1k());
				BigDecimal output = safePrice(item.getOutputPricePer1k());
				String currency = !isBlank(item.getCurrency()) ? item.getCurrency() : "CNY";
				String version = !isBlank(item.getVersion()) ? item.getVersion() : "default";
				if (existed == null) {
					CleaningPriceCatalog catalog = CleaningPriceCatalog.builder()
						.provider(item.getProvider())
						.model(item.getModel())
						.version(version)
						.inputPricePer1k(input)
						.outputPricePer1k(output)
						.currency(currency)
						.createdTime(LocalDateTime.now())
						.updatedTime(LocalDateTime.now())
						.build();
					try {
						priceCatalogMapper.insert(catalog);
						inserted++;
						continue;
					}
					catch (DuplicateKeyException duplicateKeyException) {
						log.debug("Pricing already initialized provider={} model={} version={}", item.getProvider(),
								item.getModel(), version);
						existed = priceCatalogMapper.findLatestByProviderAndModel(item.getProvider(), item.getModel());
						if (existed == null) {
							throw duplicateKeyException;
						}
					}
				}
				boolean changed = !equalsPrice(existed.getInputPricePer1k(), input)
						|| !equalsPrice(existed.getOutputPricePer1k(), output)
						|| !safeString(existed.getCurrency()).equals(currency)
						|| !safeString(existed.getVersion()).equals(version);
				if (!changed) {
					skipped++;
					continue;
				}
				existed.setVersion(version);
				existed.setInputPricePer1k(input);
				existed.setOutputPricePer1k(output);
				existed.setCurrency(currency);
				existed.setUpdatedTime(LocalDateTime.now());
				priceCatalogMapper.updateById(existed);
				updated++;
			}
			LocalDateTime finished = LocalDateTime.now();
			pricingService.clearCache();
			opsStateService.markPricingSyncSuccess(finished);
			return CleaningPricingSyncResult.builder()
				.success(true)
				.sourceType(sourceType)
				.reason(reason)
				.total(sourceItems.size())
				.inserted(inserted)
				.updated(updated)
				.skipped(skipped)
				.message("pricing sync success")
				.startedTime(started)
				.finishedTime(finished)
				.build();
		}
		catch (Exception e) {
			opsStateService.markPricingSyncFailure();
			notificationService.notifyAsync("WARN", CODE_PRICING_SYNC_FAILED, "价格同步失败: " + e.getMessage(),
					Map.of("sourceType", sourceType, "reason", reason));
			return CleaningPricingSyncResult.builder()
				.success(false)
				.sourceType(sourceType)
				.reason(reason)
				.total(inserted + updated + skipped)
				.inserted(inserted)
				.updated(updated)
				.skipped(skipped)
				.message("pricing sync failed: " + e.getMessage())
				.startedTime(started)
				.finishedTime(LocalDateTime.now())
				.build();
		}
	}

	public List<CleaningPriceCatalog> listCatalog() {
		return priceCatalogMapper
			.selectList(new LambdaQueryWrapper<CleaningPriceCatalog>().orderByDesc(CleaningPriceCatalog::getUpdatedTime)
				.orderByDesc(CleaningPriceCatalog::getId));
	}

	public boolean isSyncEnabled() {
		return dataSentryProperties.getCleaning().getPricing().isSyncEnabled();
	}

	private boolean isManualPricingProtected(String provider, String model) {
		ModelConfig modelConfig = modelConfigMapper.findByProviderAndModelName(provider, model);
		if (modelConfig == null) {
			return false;
		}
		if (!PRICING_SOURCE_MANUAL.equalsIgnoreCase(safeString(modelConfig.getPricingSource()))) {
			return false;
		}
		return modelConfig.getInputPricePer1k() != null && modelConfig.getOutputPricePer1k() != null;
	}

	private List<DataSentryProperties.Cleaning.Pricing.PriceItem> loadSourceItems(String sourceType) throws Exception {
		if ("HTTP_JSON".equalsIgnoreCase(sourceType)) {
			List<DataSentryProperties.Cleaning.Pricing.PriceItem> httpItems = loadFromHttp();
			if (!httpItems.isEmpty()) {
				return httpItems;
			}
		}
		List<DataSentryProperties.Cleaning.Pricing.PriceItem> localItems = dataSentryProperties.getCleaning()
			.getPricing()
			.getLocalCatalog();
		if (localItems == null || localItems.isEmpty()) {
			DataSentryProperties.Cleaning.Pricing.PriceItem fallback = new DataSentryProperties.Cleaning.Pricing.PriceItem();
			fallback.setProvider(CleaningPricingService.DEFAULT_PROVIDER);
			fallback.setModel(CleaningPricingService.DEFAULT_MODEL);
			fallback.setVersion("default");
			fallback.setInputPricePer1k(new BigDecimal("0.002000"));
			fallback.setOutputPricePer1k(new BigDecimal("0.004000"));
			fallback.setCurrency("CNY");
			return List.of(fallback);
		}
		return localItems;
	}

	private List<DataSentryProperties.Cleaning.Pricing.PriceItem> loadFromHttp() throws Exception {
		DataSentryProperties.Cleaning.Pricing.Http http = dataSentryProperties.getCleaning().getPricing().getHttp();
		if (http == null || isBlank(http.getUrl())) {
			return List.of();
		}
		HttpClient client = HttpClient.newBuilder()
			.connectTimeout(Duration.ofMillis(Math.max(http.getTimeoutMs(), 1000)))
			.build();
		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create(http.getUrl()))
			.timeout(Duration.ofMillis(Math.max(http.getTimeoutMs(), 1000)))
			.GET()
			.build();
		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
		if (response.statusCode() < 200 || response.statusCode() >= 300) {
			throw new IllegalStateException("http status=" + response.statusCode());
		}
		var root = JsonUtil.getObjectMapper().readTree(response.body());
		List<DataSentryProperties.Cleaning.Pricing.PriceItem> items = new ArrayList<>();
		if (root.isArray()) {
			for (var node : root) {
				DataSentryProperties.Cleaning.Pricing.PriceItem parsed = parsePriceNode(node);
				if (parsed != null) {
					items.add(parsed);
				}
			}
		}
		else if (root.has("data") && root.get("data").isArray()) {
			for (var node : root.get("data")) {
				DataSentryProperties.Cleaning.Pricing.PriceItem parsed = parsePriceNode(node);
				if (parsed != null) {
					items.add(parsed);
				}
			}
		}
		return items;
	}

	private DataSentryProperties.Cleaning.Pricing.PriceItem parsePriceNode(
			com.fasterxml.jackson.databind.JsonNode node) {
		if (node == null || !node.isObject()) {
			return null;
		}
		String provider = text(node, "provider");
		String model = text(node, "model");
		if (isBlank(provider) || isBlank(model)) {
			return null;
		}
		DataSentryProperties.Cleaning.Pricing.PriceItem item = new DataSentryProperties.Cleaning.Pricing.PriceItem();
		item.setProvider(provider);
		item.setModel(model);
		item.setVersion(!isBlank(text(node, "version")) ? text(node, "version") : "default");
		item.setInputPricePer1k(decimal(node, "inputPricePer1k"));
		item.setOutputPricePer1k(decimal(node, "outputPricePer1k"));
		item.setCurrency(!isBlank(text(node, "currency")) ? text(node, "currency") : "CNY");
		return item;
	}

	private String text(com.fasterxml.jackson.databind.JsonNode node, String key) {
		return node.has(key) && !node.get(key).isNull() ? node.get(key).asText() : null;
	}

	private BigDecimal decimal(com.fasterxml.jackson.databind.JsonNode node, String key) {
		try {
			if (node.has(key) && !node.get(key).isNull()) {
				return new BigDecimal(node.get(key).asText());
			}
		}
		catch (Exception e) {
			return BigDecimal.ZERO;
		}
		return BigDecimal.ZERO;
	}

	private boolean equalsPrice(BigDecimal a, BigDecimal b) {
		return safePrice(a).compareTo(safePrice(b)) == 0;
	}

	private BigDecimal safePrice(BigDecimal value) {
		if (value == null || value.signum() < 0) {
			return BigDecimal.ZERO;
		}
		return value;
	}

	private String normalizeSourceType(String sourceType) {
		if ("HTTP_JSON".equalsIgnoreCase(sourceType)) {
			return "HTTP_JSON";
		}
		return "LOCAL_CONFIG";
	}

	private String safeString(String value) {
		return value == null ? "" : value;
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

}
