package com.touhouqing.datasentry.cleaning.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.touhouqing.datasentry.cleaning.model.CleaningPriceCatalog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CleaningPriceCatalogMapper extends BaseMapper<CleaningPriceCatalog> {

	default CleaningPriceCatalog findLatestByProviderAndModel(String provider, String model) {
		return selectOne(new LambdaQueryWrapper<CleaningPriceCatalog>().eq(CleaningPriceCatalog::getProvider, provider)
			.eq(CleaningPriceCatalog::getModel, model)
			.orderByDesc(CleaningPriceCatalog::getId)
			.last("LIMIT 1"));
	}

}
