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
package com.touhouqing.datasentry.service.aimodelconfig;

import com.touhouqing.datasentry.cleaning.service.CleaningPricingService;
import com.touhouqing.datasentry.converter.ModelConfigConverter;
import com.touhouqing.datasentry.dto.ModelConfigDTO;
import com.touhouqing.datasentry.entity.ModelConfig;
import com.touhouqing.datasentry.enums.ModelType;
import com.touhouqing.datasentry.mapper.ModelConfigMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.touhouqing.datasentry.converter.ModelConfigConverter.toDTO;
import static com.touhouqing.datasentry.converter.ModelConfigConverter.toEntity;

@Slf4j
@Service
@AllArgsConstructor
public class ModelConfigDataServiceImpl implements ModelConfigDataService {

	private static final String PRICING_SOURCE_MANUAL = "MANUAL";

	private final ModelConfigMapper modelConfigMapper;

	private final CleaningPricingService cleaningPricingService;

	@Override
	public ModelConfig findById(Long id) {
		return modelConfigMapper.findById(id);
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public void switchActiveStatus(Long id, ModelType type) {
		// 1. 禁用同类型其他配置
		modelConfigMapper.deactivateOthers(type.getCode(), id);

		// 2. 启用当前配置
		ModelConfig entity = modelConfigMapper.findById(id);
		if (entity != null) {
			entity.setIsActive(true);
			entity.setUpdatedTime(LocalDateTime.now());
			modelConfigMapper.updateById(entity);
		}
	}

	@Override
	public List<ModelConfigDTO> listConfigs() {
		return modelConfigMapper.findAll().stream().map(ModelConfigConverter::toDTO).collect(Collectors.toList());
	}

	@Override
	public void addConfig(ModelConfigDTO dto) {
		clean(dto);
		validatePricing(dto);
		ModelConfig entity = toEntity(dto);
		applyManualPricingIfPresent(entity);
		// 只存库，不切换
		modelConfigMapper.insert(entity);
		cleaningPricingService.clearCache();
	}

	private void clean(ModelConfigDTO dto) {
		dto.setModelName(trimToNull(dto.getModelName()));
		dto.setBaseUrl(trimToNull(dto.getBaseUrl()));
		dto.setApiKey(trimToNull(dto.getApiKey()));
		dto.setModelVersion(trimToNull(dto.getModelVersion()));
		dto.setCurrency(trimToNull(dto.getCurrency()));
		if (dto.getCompletionsPath() != null) {
			dto.setCompletionsPath(dto.getCompletionsPath().trim());
		}
		if (dto.getEmbeddingsPath() != null) {
			dto.setEmbeddingsPath(dto.getEmbeddingsPath().trim());
		}
	}

	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private void validatePricing(ModelConfigDTO dto) {
		if (dto.getInputPricePer1k() != null && dto.getInputPricePer1k().compareTo(BigDecimal.ZERO) < 0) {
			throw new RuntimeException("输入单价必须大于等于0");
		}
		if (dto.getOutputPricePer1k() != null && dto.getOutputPricePer1k().compareTo(BigDecimal.ZERO) < 0) {
			throw new RuntimeException("输出单价必须大于等于0");
		}
		if (dto.getCurrency() != null && dto.getCurrency().isBlank()) {
			throw new RuntimeException("货币不能为空");
		}
		boolean hasAnyPricingField = dto.getInputPricePer1k() != null || dto.getOutputPricePer1k() != null
				|| dto.getCurrency() != null;
		if (hasAnyPricingField) {
			if (dto.getInputPricePer1k() == null || dto.getOutputPricePer1k() == null) {
				throw new RuntimeException("输入单价和输出单价需同时填写");
			}
			if (dto.getCurrency() == null) {
				throw new RuntimeException("货币不能为空");
			}
		}
	}

	private void applyManualPricingIfPresent(ModelConfig entity) {
		if (entity.getInputPricePer1k() != null && entity.getOutputPricePer1k() != null) {
			entity.setPricingSource(PRICING_SOURCE_MANUAL);
			entity.setPricingUpdatedAt(LocalDateTime.now());
			if (entity.getCurrency() == null || entity.getCurrency().isBlank()) {
				entity.setCurrency("CNY");
			}
		}
	}

	/**
	 * 更新配置到数据库 (不处理热切换) 返回更新后的实体，以便上层业务判断是否需要刷新内存
	 */
	@Transactional(rollbackFor = Exception.class)
	@Override
	public ModelConfig updateConfigInDb(ModelConfigDTO dto) {
		clean(dto);
		validatePricing(dto);
		// 1. 查旧数据
		ModelConfig entity = modelConfigMapper.findById(dto.getId());
		if (entity == null) {
			throw new RuntimeException("配置不存在");
		}

		// 不准更改模型类型
		if (!entity.getModelType().getCode().equals(dto.getModelType())) {
			throw new RuntimeException("模型类型不允许修改");
		}

		// 2. 合并字段
		mergeDtoToEntity(dto, entity);
		entity.setUpdatedTime(LocalDateTime.now());

		// 3. 更新数据库
		modelConfigMapper.updateById(entity);
		cleaningPricingService.clearCache();

		return entity;
	}

	private static void mergeDtoToEntity(ModelConfigDTO dto, ModelConfig oldEntity) {
		BigDecimal oldInputPrice = oldEntity.getInputPricePer1k();
		BigDecimal oldOutputPrice = oldEntity.getOutputPricePer1k();
		String oldCurrency = oldEntity.getCurrency();
		String oldVersion = oldEntity.getModelVersion();

		oldEntity.setProvider(dto.getProvider());
		oldEntity.setBaseUrl(dto.getBaseUrl());
		oldEntity.setModelName(dto.getModelName());
		oldEntity.setModelVersion(dto.getModelVersion());
		oldEntity.setTemperature(dto.getTemperature());
		oldEntity.setMaxTokens(dto.getMaxTokens());
		oldEntity.setCompletionsPath(dto.getCompletionsPath());
		oldEntity.setEmbeddingsPath(dto.getEmbeddingsPath());
		oldEntity.setInputPricePer1k(dto.getInputPricePer1k());
		oldEntity.setOutputPricePer1k(dto.getOutputPricePer1k());
		oldEntity.setCurrency(dto.getCurrency());
		oldEntity.setUpdatedTime(LocalDateTime.now());

		boolean pricingChanged = !Objects.equals(oldInputPrice, oldEntity.getInputPricePer1k())
				|| !Objects.equals(oldOutputPrice, oldEntity.getOutputPricePer1k())
				|| !Objects.equals(oldCurrency, oldEntity.getCurrency())
				|| !Objects.equals(oldVersion, oldEntity.getModelVersion());
		if (pricingChanged && oldEntity.getInputPricePer1k() != null && oldEntity.getOutputPricePer1k() != null) {
			oldEntity.setPricingSource(PRICING_SOURCE_MANUAL);
			oldEntity.setPricingUpdatedAt(LocalDateTime.now());
			if (oldEntity.getCurrency() == null || oldEntity.getCurrency().isBlank()) {
				oldEntity.setCurrency("CNY");
			}
		}

		// 只有当前端传来的 Key 不包含 "****" 时，才说明用户真的改了 Key，否则保持原样
		if (dto.getApiKey() != null && !dto.getApiKey().contains("****")) {
			oldEntity.setApiKey(dto.getApiKey());
		}
	}

	@Override
	public void deleteConfig(Long id) {
		// 1. 先查询是否存在
		ModelConfig entity = modelConfigMapper.findById(id);
		if (entity == null) {
			throw new RuntimeException("配置不存在");
		}

		// 2. 如果是激活状态，禁止删除
		if (Boolean.TRUE.equals(entity.getIsActive())) {
			throw new RuntimeException("该配置当前正在使用中，无法删除！请先激活其他配置，再进行删除操作。");
		}

		// 3. 执行删除逻辑
		entity.setIsDeleted(1);
		entity.setUpdatedTime(LocalDateTime.now());
		int updated = modelConfigMapper.updateById(entity);
		if (updated == 0) {
			throw new RuntimeException("删除失败");
		}
	}

	@Override
	public ModelConfigDTO getActiveConfigByType(ModelType modelType) {
		ModelConfig entity = modelConfigMapper.selectActiveByType(modelType.getCode());
		if (entity == null) {
			log.warn("Activation model configuration of type [{}] not found, attempting to downgrade...", modelType);
			return null;
		}
		return toDTO(entity);
	}

}
