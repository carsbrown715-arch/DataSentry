package com.touhouqing.datasentry.cleaning.service;

import com.touhouqing.datasentry.cleaning.mapper.CleaningPriceCatalogMapper;
import com.touhouqing.datasentry.cleaning.model.CleaningPriceCatalog;
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

	private final Map<String, CacheEntry> pricingCache = new ConcurrentHashMap<>();

	public Pricing resolvePricing(String provider, String model) {
		String resolvedProvider = provider != null && !provider.isBlank() ? provider : DEFAULT_PROVIDER;
		String resolvedModel = model != null && !model.isBlank() ? model : DEFAULT_MODEL;
		String cacheKey = resolvedProvider + "::" + resolvedModel;
		CacheEntry cached = pricingCache.get(cacheKey);
		if (cached != null && !cached.expired()) {
			return cached.pricing();
		}
		CleaningPriceCatalog catalog = priceCatalogMapper.findLatestByProviderAndModel(resolvedProvider, resolvedModel);
		Pricing pricing;
		if (catalog != null) {
			pricing = new Pricing(catalog.getProvider(), catalog.getModel(), safePrice(catalog.getInputPricePer1k()),
					safePrice(catalog.getOutputPricePer1k()),
					catalog.getCurrency() != null && !catalog.getCurrency().isBlank() ? catalog.getCurrency()
							: DEFAULT_CURRENCY);
		}
		else {
			pricing = defaultPricing();
		}
		pricingCache.put(cacheKey, new CacheEntry(pricing, Instant.now().toEpochMilli() + CACHE_TTL_MILLIS));
		return pricing;
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
