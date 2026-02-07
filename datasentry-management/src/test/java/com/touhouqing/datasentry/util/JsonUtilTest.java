/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.touhouqing.datasentry.util;

import com.touhouqing.datasentry.cleaning.model.CleaningRule;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class JsonUtilTest {

	@Test
	public void shouldSerializeLocalDateTimeFields() throws Exception {
		CleaningRule rule = CleaningRule.builder()
			.id(1L)
			.name("test")
			.createdTime(LocalDateTime.of(2026, 2, 7, 21, 0))
			.updatedTime(LocalDateTime.of(2026, 2, 7, 21, 1))
			.build();

		String json = JsonUtil.getObjectMapper().writeValueAsString(rule);

		assertTrue(json.contains("\"createdTime\""));
		assertTrue(json.contains("2026-02-07 21:00:00"));
	}

}
