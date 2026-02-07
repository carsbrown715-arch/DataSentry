package com.touhouqing.datasentry.cleaning.service;

import com.touhouqing.datasentry.cleaning.mapper.CleaningPriceCatalogMapper;
import com.touhouqing.datasentry.cleaning.model.CleaningPriceCatalog;
import com.touhouqing.datasentry.entity.ModelConfig;
import com.touhouqing.datasentry.enums.ModelType;
import com.touhouqing.datasentry.mapper.ModelConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class CleaningPricingService {

	public static final String DEFAULT_PROVIDER = "LOCAL_DEFAULT";

	public static final String DEFAULT_MODEL = "L3_LLM";

	private static final BigDecimal DEFAULT_INPUT_PRICE = new BigDecimal("0.002000");

	private static final BigDecimal DEFAULT_OUTPUT_PRICE = new BigDecimal("0.004000");

	private static final String DEFAULT_CURRENCY = "CNY";

	private static final long CACHE_TTL_MILLIS = 60000;

	private final CleaningPriceCatalogMapper priceCatalogMapper;

	private final ModelConfigMapper modelConfigMapper;

	private final Map<String, CacheEntry> pricingCache = new ConcurrentHashMap<>();

	public Pricing resolvePricing(String provider, String model) {
		String resolvedProvider = provider != null && !provider.isBlank() ? provider : DEFAULT_PROVIDER;
		String resolvedModel = model != null && !model.isBlank() ? model : DEFAULT_MODEL;
		String cacheKey = resolvedProvider + "::" + resolvedModel;
		CacheEntry cached = pricingCache.get(cacheKey);
		if (cached != null && !cached.expired()) {
			return cached.pricing();
		}
		Pricing pricing = resolvePricingFromModelConfig(resolvedProvider, resolvedModel);
		if (pricing == null) {
			pricing = resolvePricingFromCatalog(resolvedProvider, resolvedModel);
		}
		if (pricing == null) {
			pricing = defaultPricing();
		}
		pricingCache.put(cacheKey, new CacheEntry(pricing, Instant.now().toEpochMilli() + CACHE_TTL_MILLIS));
		return pricing;
	}

	private Pricing resolvePricingFromModelConfig(String provider, String model) {
		ModelConfig modelConfig = modelConfigMapper.findByProviderAndModelName(provider, model);
		if (modelConfig == null && DEFAULT_PROVIDER.equals(provider) && DEFAULT_MODEL.equals(model)) {
			modelConfig = modelConfigMapper.selectActiveByType(ModelType.CHAT.getCode());
		}
		if (modelConfig == null || modelConfig.getInputPricePer1k() == null
				|| modelConfig.getOutputPricePer1k() == null) {
			return null;
		}
		String currency = modelConfig.getCurrency() != null && !modelConfig.getCurrency().isBlank()
				? modelConfig.getCurrency() : DEFAULT_CURRENCY;
		String resolvedProvider = modelConfig.getProvider() != null && !modelConfig.getProvider().isBlank()
				? modelConfig.getProvider() : provider;
		String resolvedModel = modelConfig.getModelName() != null && !modelConfig.getModelName().isBlank()
				? modelConfig.getModelName() : model;
		return new Pricing(resolvedProvider, resolvedModel, safePrice(modelConfig.getInputPricePer1k()),
				safePrice(modelConfig.getOutputPricePer1k()), currency);
	}
	private Pricing resolvePricingFromCatalog(String provider, String model) {
		CleaningPriceCatalog catalog = priceCatalogMapper.findLatestByProviderAndModel(provider, model);
		if (catalog == null) {
			return null;
		}
		return new Pricing(catalog.getProvider(), catalog.getModel(), safePrice(catalog.getInputPricePer1k()),
				safePrice(catalog.getOutputPricePer1k()),
				catalog.getCurrency() != null && !catalog.getCurrency().isBlank() ? catalog.getCurrency()
						: DEFAULT_CURRENCY);
	}

	public Pricing defaultPricing() {
		return new Pricing(DEFAULT_PROVIDER, DEFAULT_MODEL, DEFAULT_INPUT_PRICE, DEFAULT_OUTPUT_PRICE,
				DEFAULT_CURRENCY);
	}

	public void clearCache() {
		pricingCache.clear();
	}

	private BigDecimal safePrice(BigDecimal value) {
		if (value == null || value.signum() < 0) {
			return BigDecimal.ZERO;
		}
		return value;
	}

	public record Pricing(String provider, String model, BigDecimal inputPricePer1k, BigDecimal outputPricePer1k,
			String currency) {
	}

	private record CacheEntry(Pricing pricing, long expiresAt) {

		boolean expired() {
			return Instant.now().toEpochMilli() > expiresAt;
		}

	}

}
