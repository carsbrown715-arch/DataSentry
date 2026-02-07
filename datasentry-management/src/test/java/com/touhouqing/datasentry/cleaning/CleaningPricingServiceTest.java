package com.touhouqing.datasentry.cleaning;

import com.touhouqing.datasentry.cleaning.mapper.CleaningPriceCatalogMapper;
import com.touhouqing.datasentry.cleaning.model.CleaningPriceCatalog;
import com.touhouqing.datasentry.cleaning.service.CleaningPricingService;
import com.touhouqing.datasentry.entity.ModelConfig;
import com.touhouqing.datasentry.mapper.ModelConfigMapper;
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
		ModelConfigMapper modelConfigMapper = mock(ModelConfigMapper.class);
		when(modelConfigMapper.findByProviderAndModelName("LOCAL_DEFAULT", "L3_LLM")).thenReturn(null);
		when(mapper.findLatestByProviderAndModel("LOCAL_DEFAULT", "L3_LLM")).thenReturn(null);
		CleaningPricingService service = new CleaningPricingService(mapper, modelConfigMapper);

		CleaningPricingService.Pricing pricing = service.resolvePricing(null, null);

		assertNotNull(pricing);
		assertEquals("LOCAL_DEFAULT", pricing.provider());
		assertEquals("L3_LLM", pricing.model());
		assertEquals(new BigDecimal("0.002000"), pricing.inputPricePer1k());
	}

	@Test
	public void shouldUseCatalogPricingWhenExists() {
		CleaningPriceCatalogMapper mapper = mock(CleaningPriceCatalogMapper.class);
		ModelConfigMapper modelConfigMapper = mock(ModelConfigMapper.class);
		CleaningPriceCatalog catalog = CleaningPriceCatalog.builder()
			.provider("P")
			.model("M")
			.inputPricePer1k(new BigDecimal("0.123456"))
			.outputPricePer1k(new BigDecimal("0.654321"))
			.currency("CNY")
			.build();
		when(modelConfigMapper.findByProviderAndModelName("P", "M")).thenReturn(null);
		when(mapper.findLatestByProviderAndModel("P", "M")).thenReturn(catalog);
		CleaningPricingService service = new CleaningPricingService(mapper, modelConfigMapper);

		CleaningPricingService.Pricing pricing = service.resolvePricing("P", "M");

		assertEquals(new BigDecimal("0.123456"), pricing.inputPricePer1k());
		assertEquals(new BigDecimal("0.654321"), pricing.outputPricePer1k());
	}

	@Test
	public void shouldUseModelConfigPricingBeforeCatalog() {
		CleaningPriceCatalogMapper mapper = mock(CleaningPriceCatalogMapper.class);
		ModelConfigMapper modelConfigMapper = mock(ModelConfigMapper.class);
		ModelConfig modelConfig = new ModelConfig();
		modelConfig.setProvider("P");
		modelConfig.setModelName("M");
		modelConfig.setInputPricePer1k(new BigDecimal("0.200000"));
		modelConfig.setOutputPricePer1k(new BigDecimal("0.300000"));
		modelConfig.setCurrency("USD");
		when(modelConfigMapper.findByProviderAndModelName("P", "M")).thenReturn(modelConfig);
		CleaningPricingService service = new CleaningPricingService(mapper, modelConfigMapper);

		CleaningPricingService.Pricing pricing = service.resolvePricing("P", "M");

		assertEquals(new BigDecimal("0.200000"), pricing.inputPricePer1k());
		assertEquals(new BigDecimal("0.300000"), pricing.outputPricePer1k());
		assertEquals("USD", pricing.currency());
	}

	@Test
	public void shouldUseActiveChatModelPricingWhenDefaultKeyRequested() {
		CleaningPriceCatalogMapper mapper = mock(CleaningPriceCatalogMapper.class);
		ModelConfigMapper modelConfigMapper = mock(ModelConfigMapper.class);
		ModelConfig activeChatConfig = new ModelConfig();
		activeChatConfig.setProvider("deepseek");
		activeChatConfig.setModelName("deepseek-chat");
		activeChatConfig.setInputPricePer1k(new BigDecimal("0.111111"));
		activeChatConfig.setOutputPricePer1k(new BigDecimal("0.222222"));
		activeChatConfig.setCurrency("CNY");
		when(modelConfigMapper.findByProviderAndModelName("LOCAL_DEFAULT", "L3_LLM")).thenReturn(null);
		when(modelConfigMapper.selectActiveByType("CHAT")).thenReturn(activeChatConfig);
		CleaningPricingService service = new CleaningPricingService(mapper, modelConfigMapper);

		CleaningPricingService.Pricing pricing = service.resolvePricing(CleaningPricingService.DEFAULT_PROVIDER,
				CleaningPricingService.DEFAULT_MODEL);

		assertEquals("deepseek", pricing.provider());
		assertEquals("deepseek-chat", pricing.model());
		assertEquals(new BigDecimal("0.111111"), pricing.inputPricePer1k());
		assertEquals(new BigDecimal("0.222222"), pricing.outputPricePer1k());
	}

}
