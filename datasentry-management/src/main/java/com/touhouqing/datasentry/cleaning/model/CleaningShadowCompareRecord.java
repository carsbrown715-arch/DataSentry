package com.touhouqing.datasentry.cleaning.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("datasentry_cleaning_shadow_compare_record")
public class CleaningShadowCompareRecord {

	@TableId(type = IdType.AUTO)
	private Long id;

	private String traceId;

	private Long jobRunId;

	private Long policyId;

	private Long policyVersionId;

	private String columnName;

	private String mainVerdict;

	private String shadowVerdict;

	private String diffLevel;

	private String diffJson;

	private LocalDateTime createdTime;

}
