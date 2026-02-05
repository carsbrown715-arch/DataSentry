package com.touhouqing.datasentry.cleaning.util;

import com.touhouqing.datasentry.DataSentryApplication;
import com.touhouqing.datasentry.cleaning.dto.CleaningCheckRequest;
import com.touhouqing.datasentry.cleaning.dto.CleaningResponse;
import com.touhouqing.datasentry.cleaning.service.CleaningService;
import com.touhouqing.datasentry.util.JsonUtil;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.UUID;

public class CleaningLocalRunner {

	public static void main(String[] args) throws Exception {
		if (args.length == 0) {
			System.out.println("Usage: CleaningLocalRunner <text> [policyId] [agentId]");
			return;
		}
		String text = args[0];
		Long policyId = args.length > 1 ? Long.parseLong(args[1]) : null;
		Long agentId = args.length > 2 ? Long.parseLong(args[2]) : 1L;
		SpringApplication application = new SpringApplication(DataSentryApplication.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		ConfigurableApplicationContext context = application.run();
		try {
			CleaningService cleaningService = context.getBean(CleaningService.class);
			CleaningCheckRequest request = CleaningCheckRequest.builder().text(text).policyId(policyId).build();
			CleaningResponse response = cleaningService.check(agentId, request, UUID.randomUUID().toString());
			String output = JsonUtil.getObjectMapper().writeValueAsString(response);
			System.out.println(output);
		}
		finally {
			context.close();
		}
	}

}
