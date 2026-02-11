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
@TableName("datasentry_cleaning_policy_version")
public class CleaningPolicyVersion {

	@TableId(type = IdType.AUTO)
	private Long id;

	private Long policyId;

	private Integer versionNo;

	private String status;

	private String configJson;

	private String defaultAction;

	private LocalDateTime createdTime;

	private LocalDateTime updatedTime;

}
