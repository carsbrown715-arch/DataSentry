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
package com.touhouqing.datasentry.dto;

import com.touhouqing.datasentry.annotation.InEnum;
import com.touhouqing.datasentry.enums.ModelType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelConfigDTO {

	private Long id;

	@NotBlank(message = "provider must not be empty")
	private String provider; // e.g. "openai", "deepseek"

	private String apiKey; // e.g. "https://api.openai.com"

	@NotBlank(message = "baseUrl must not be empty")
	private String baseUrl;

	@NotBlank(message = "modelName must not be empty")
	private String modelName;

	private String modelVersion;

	@NotBlank(message = "modelType must not be empty")
	@InEnum(value = ModelType.class, message = "CHAT/EMBEDDING 之一")
	private String modelType;

	// 仅当厂商路径非标准时填写，例如 "/custom/chat"
	private String completionsPath;

	// 仅当厂商路径非标准时填写
	private String embeddingsPath;

	@Builder.Default
	private Double temperature = 0.0;

	@Builder.Default
	private Integer maxTokens = 2000;

	@Builder.Default
	private Boolean isActive = true;

	@DecimalMin(value = "0.000000", message = "inputPricePer1k must be >= 0")
	private BigDecimal inputPricePer1k;

	@DecimalMin(value = "0.000000", message = "outputPricePer1k must be >= 0")
	private BigDecimal outputPricePer1k;

	@Builder.Default
	private String currency = "CNY";

	private String pricingSource;

}
