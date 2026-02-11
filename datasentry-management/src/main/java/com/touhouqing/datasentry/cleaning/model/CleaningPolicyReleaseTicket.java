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
@TableName("datasentry_cleaning_policy_release_ticket")
public class CleaningPolicyReleaseTicket {

	@TableId(type = IdType.AUTO)
	private Long id;

	private Long policyId;

	private Long versionId;

	private String action;

	private String note;

	private String operator;

	private LocalDateTime createdTime;

}
