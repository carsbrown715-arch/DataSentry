package com.touhouqing.datasentry.cleaning.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.touhouqing.datasentry.cleaning.model.CleaningAllowlist;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface CleaningAllowlistMapper extends BaseMapper<CleaningAllowlist> {

	default List<CleaningAllowlist> findActive() {
		LocalDateTime now = LocalDateTime.now();
		return selectList(new LambdaQueryWrapper<CleaningAllowlist>().eq(CleaningAllowlist::getEnabled, 1)
			.and(wrapper -> wrapper.isNull(CleaningAllowlist::getExpireTime)
				.or()
				.gt(CleaningAllowlist::getExpireTime, now)));
	}

}
