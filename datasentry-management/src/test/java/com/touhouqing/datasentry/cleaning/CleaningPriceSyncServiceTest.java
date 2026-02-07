package com.touhouqing.datasentry.cleaning;

import com.touhouqing.datasentry.cleaning.dto.CleaningPricingSyncResult;
import com.touhouqing.datasentry.cleaning.mapper.CleaningPriceCatalogMapper;
import com.touhouqing.datasentry.cleaning.model.CleaningPriceCatalog;
import com.touhouqing.datasentry.cleaning.service.CleaningNotificationService;
import com.touhouqing.datasentry.cleaning.service.CleaningOpsStateService;
import com.touhouqing.datasentry.cleaning.service.CleaningPriceSyncService;
import com.touhouqing.datasentry.cleaning.service.CleaningPricingService;
import com.touhouqing.datasentry.properties.DataSentryProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CleaningPriceSyncServiceTest {

	@Mock
	private CleaningPriceCatalogMapper priceCatalogMapper;

	@Mock
	private CleaningNotificationService notificationService;

	@Test
	public void shouldInsertCatalogWhenNoExistingRecord() {
		DataSentryProperties properties = new DataSentryProperties();
		DataSentryProperties.Cleaning.Pricing.PriceItem item = new DataSentryProperties.Cleaning.Pricing.PriceItem();
		item.setProvider("LOCAL_DEFAULT");
		item.setModel("L3_LLM");
		item.setVersion("default");
		item.setInputPricePer1k(new BigDecimal("0.002000"));
		item.setOutputPricePer1k(new BigDecimal("0.004000"));
		item.setCurrency("CNY");
		properties.getCleaning().getPricing().setSourceType("LOCAL_CONFIG");
		properties.getCleaning().getPricing().setLocalCatalog(List.of(item));
		CleaningOpsStateService opsStateService = new CleaningOpsStateService();
		CleaningPricingService pricingService = new CleaningPricingService(priceCatalogMapper);
		CleaningPriceSyncService syncService = new CleaningPriceSyncService(properties, priceCatalogMapper,
				opsStateService, notificationService, pricingService);

		when(priceCatalogMapper.findLatestByProviderAndModel("LOCAL_DEFAULT", "L3_LLM")).thenReturn(null);

		CleaningPricingSyncResult result = syncService.syncNow("test");

		assertTrue(result.isSuccess());
		assertEquals(1, result.getInserted());
		assertEquals(0, result.getUpdated());
		verify(priceCatalogMapper).insert(any(CleaningPriceCatalog.class));
	}

	@Test
	public void shouldUpdateCatalogWhenPriceChanged() {
		DataSentryProperties properties = new DataSentryProperties();
		DataSentryProperties.Cleaning.Pricing.PriceItem item = new DataSentryProperties.Cleaning.Pricing.PriceItem();
		item.setProvider("LOCAL_DEFAULT");
		item.setModel("L3_LLM");
		item.setVersion("default");
		item.setInputPricePer1k(new BigDecimal("0.003000"));
		item.setOutputPricePer1k(new BigDecimal("0.005000"));
		item.setCurrency("CNY");
		properties.getCleaning().getPricing().setSourceType("LOCAL_CONFIG");
		properties.getCleaning().getPricing().setLocalCatalog(List.of(item));
		CleaningOpsStateService opsStateService = new CleaningOpsStateService();
		CleaningPricingService pricingService = new CleaningPricingService(priceCatalogMapper);
		CleaningPriceSyncService syncService = new CleaningPriceSyncService(properties, priceCatalogMapper,
				opsStateService, notificationService, pricingService);

		CleaningPriceCatalog existed = CleaningPriceCatalog.builder()
			.id(1L)
			.provider("LOCAL_DEFAULT")
			.model("L3_LLM")
			.version("default")
			.inputPricePer1k(new BigDecimal("0.002000"))
			.outputPricePer1k(new BigDecimal("0.004000"))
			.currency("CNY")
			.build();
		when(priceCatalogMapper.findLatestByProviderAndModel("LOCAL_DEFAULT", "L3_LLM")).thenReturn(existed);

		CleaningPricingSyncResult result = syncService.syncNow("test");

		assertTrue(result.isSuccess());
		assertEquals(1, result.getUpdated());
		verify(priceCatalogMapper).updateById(any(CleaningPriceCatalog.class));
	}

	@Test
	public void shouldMarkFailureWhenHttpSourceBroken() {
		DataSentryProperties properties = new DataSentryProperties();
		properties.getCleaning().getPricing().setSourceType("HTTP_JSON");
		properties.getCleaning().getPricing().getHttp().setUrl("http://::invalid");
		CleaningOpsStateService opsStateService = new CleaningOpsStateService();
		CleaningPricingService pricingService = new CleaningPricingService(priceCatalogMapper);
		CleaningPriceSyncService syncService = new CleaningPriceSyncService(properties, priceCatalogMapper,
				opsStateService, notificationService, pricingService);

		CleaningPricingSyncResult result = syncService.syncNow("scheduled");

		assertFalse(result.isSuccess());
		verify(notificationService).notifyAsync(eq("WARN"), eq("PRICING_SYNC_FAILED"), any(String.class), anyMap());
	}

}
