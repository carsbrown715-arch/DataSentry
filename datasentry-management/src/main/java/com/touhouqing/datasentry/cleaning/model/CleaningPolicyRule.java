package com.touhouqing.datasentry.cleaning.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("datasentry_cleaning_policy_rule")
public class CleaningPolicyRule {

	private Long policyId;

	private Long ruleId;

	private Integer priority;

}
