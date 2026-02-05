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
@TableName("datasentry_cleaning_review_task")
public class CleaningReviewTask {

	@TableId(type = IdType.AUTO)
	private Long id;

	private Long jobRunId;

	private Long agentId;

	private Long datasourceId;

	private String tableName;

	private String pkJson;

	private String pkHash;

	private String columnName;

	private String verdict;

	private String categoriesJson;

	private String sanitizedPreview;

	private String actionSuggested;

	private String writebackPayloadJson;

	private String beforeRowJson;

	private String status;

	private String reviewer;

	private String reviewReason;

	private Integer version;

	private LocalDateTime createdTime;

	private LocalDateTime updatedTime;

}
