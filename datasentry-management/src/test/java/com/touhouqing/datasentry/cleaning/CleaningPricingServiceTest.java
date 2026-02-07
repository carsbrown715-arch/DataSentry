package com.touhouqing.datasentry.cleaning;

import com.touhouqing.datasentry.cleaning.mapper.CleaningPriceCatalogMapper;
import com.touhouqing.datasentry.cleaning.model.CleaningPriceCatalog;
import com.touhouqing.datasentry.cleaning.service.CleaningPricingService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CleaningPricingServiceTest {

	@Test
	public void shouldFallbackToDefaultWhenCatalogMissing() {
		CleaningPriceCatalogMapper mapper = mock(CleaningPriceCatalogMapper.class);
		when(mapper.findLatestByProviderAndModel("LOCAL_DEFAULT", "L3_LLM")).thenReturn(null);
		CleaningPricingService service = new CleaningPricingService(mapper);

		CleaningPricingService.Pricing pricing = service.resolvePricing(null, null);

		assertNotNull(pricing);
		assertEquals("LOCAL_DEFAULT", pricing.provider());
		assertEquals("L3_LLM", pricing.model());
		assertEquals(new BigDecimal("0.002000"), pricing.inputPricePer1k());
	}

	@Test
	public void shouldUseCatalogPricingWhenExists() {
		CleaningPriceCatalogMapper mapper = mock(CleaningPriceCatalogMapper.class);
		CleaningPriceCatalog catalog = CleaningPriceCatalog.builder()
			.provider("P")
			.model("M")
			.inputPricePer1k(new BigDecimal("0.123456"))
			.outputPricePer1k(new BigDecimal("0.654321"))
			.currency("CNY")
			.build();
		when(mapper.findLatestByProviderAndModel("P", "M")).thenReturn(catalog);
		CleaningPricingService service = new CleaningPricingService(mapper);

		CleaningPricingService.Pricing pricing = service.resolvePricing("P", "M");

		assertEquals(new BigDecimal("0.123456"), pricing.inputPricePer1k());
		assertEquals(new BigDecimal("0.654321"), pricing.outputPricePer1k());
	}

}
