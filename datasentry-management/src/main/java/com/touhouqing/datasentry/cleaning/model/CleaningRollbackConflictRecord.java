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
@TableName("datasentry_cleaning_rollback_conflict_record")
public class CleaningRollbackConflictRecord {

	@TableId(type = IdType.AUTO)
	private Long id;

	private Long rollbackRunId;

	private Long backupRecordId;

	private String level;

	private String reason;

	private Integer resolved;

	private LocalDateTime createdTime;

}
