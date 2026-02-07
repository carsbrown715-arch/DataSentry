package com.touhouqing.datasentry.cleaning.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CleaningPriceBootstrapRunner implements CommandLineRunner {

	private final CleaningPriceSyncService priceSyncService;

	@Override
	public void run(String... args) {
		try {
			priceSyncService.syncNow("startup");
			log.info("Initialized cleaning price catalog from configured source");
		}
		catch (Exception e) {
			log.warn("Skipped cleaning price bootstrap: {}", e.getMessage());
		}
	}

}
