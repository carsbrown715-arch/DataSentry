package com.touhouqing.datasentry.cleaning.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.touhouqing.datasentry.cleaning.model.CleaningBinding;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CleaningBindingMapper extends BaseMapper<CleaningBinding> {

	default CleaningBinding findByAgentAndScene(Long agentId, String bindingType, String scene) {
		return selectOne(new LambdaQueryWrapper<CleaningBinding>().eq(CleaningBinding::getAgentId, agentId)
			.eq(CleaningBinding::getBindingType, bindingType)
			.eq(CleaningBinding::getEnabled, 1)
			.eq(CleaningBinding::getScene, scene)
			.orderByDesc(CleaningBinding::getId)
			.last("LIMIT 1"));
	}

	default CleaningBinding findDefaultByAgent(Long agentId, String bindingType) {
		return selectOne(new LambdaQueryWrapper<CleaningBinding>().eq(CleaningBinding::getAgentId, agentId)
			.eq(CleaningBinding::getBindingType, bindingType)
			.eq(CleaningBinding::getEnabled, 1)
			.isNull(CleaningBinding::getScene)
			.orderByDesc(CleaningBinding::getId)
			.last("LIMIT 1"));
	}

}
