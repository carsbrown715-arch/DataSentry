package com.touhouqing.datasentry.cleaning.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.touhouqing.datasentry.cleaning.model.CleaningJobRun;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface CleaningJobRunMapper extends BaseMapper<CleaningJobRun> {

	default List<CleaningJobRun> findRunnableRuns(LocalDateTime now, int limit) {
		LambdaQueryWrapper<CleaningJobRun> wrapper = new LambdaQueryWrapper<CleaningJobRun>()
			.in(CleaningJobRun::getStatus, "QUEUED", "RUNNING")
			.and(condition -> condition.isNull(CleaningJobRun::getLeaseUntil)
				.or()
				.lt(CleaningJobRun::getLeaseUntil, now))
			.orderByAsc(CleaningJobRun::getId);
		wrapper.last("LIMIT " + limit);
		return selectList(wrapper);
	}

	default int acquireLease(Long id, String owner, LocalDateTime leaseUntil, LocalDateTime now,
			LocalDateTime startedTime) {
		LambdaUpdateWrapper<CleaningJobRun> wrapper = new LambdaUpdateWrapper<CleaningJobRun>()
			.eq(CleaningJobRun::getId, id)
			.in(CleaningJobRun::getStatus, "QUEUED", "RUNNING")
			.and(condition -> condition.isNull(CleaningJobRun::getLeaseUntil)
				.or()
				.lt(CleaningJobRun::getLeaseUntil, now))
			.set(CleaningJobRun::getLeaseOwner, owner)
			.set(CleaningJobRun::getLeaseUntil, leaseUntil)
			.set(CleaningJobRun::getHeartbeatTime, now)
			.set(CleaningJobRun::getStatus, "RUNNING")
			.set(CleaningJobRun::getUpdatedTime, now);
		if (startedTime != null) {
			wrapper.set(CleaningJobRun::getStartedTime, startedTime);
		}
		return update(null, wrapper);
	}

	default int heartbeat(Long id, String owner, LocalDateTime leaseUntil, LocalDateTime now) {
		LambdaUpdateWrapper<CleaningJobRun> wrapper = new LambdaUpdateWrapper<CleaningJobRun>()
			.eq(CleaningJobRun::getId, id)
			.eq(CleaningJobRun::getStatus, "RUNNING")
			.eq(CleaningJobRun::getLeaseOwner, owner)
			.set(CleaningJobRun::getLeaseUntil, leaseUntil)
			.set(CleaningJobRun::getHeartbeatTime, now)
			.set(CleaningJobRun::getUpdatedTime, now);
		return update(null, wrapper);
	}

	default int updateProgress(Long id, String checkpointJson, Long totalScanned, Long totalFlagged, Long totalWritten,
			Long totalFailed, LocalDateTime now, LocalDateTime leaseUntil) {
		LambdaUpdateWrapper<CleaningJobRun> wrapper = new LambdaUpdateWrapper<CleaningJobRun>()
			.eq(CleaningJobRun::getId, id)
			.set(CleaningJobRun::getCheckpointJson, checkpointJson)
			.set(CleaningJobRun::getTotalScanned, totalScanned)
			.set(CleaningJobRun::getTotalFlagged, totalFlagged)
			.set(CleaningJobRun::getTotalWritten, totalWritten)
			.set(CleaningJobRun::getTotalFailed, totalFailed)
			.set(CleaningJobRun::getHeartbeatTime, now)
			.set(CleaningJobRun::getLeaseUntil, leaseUntil)
			.set(CleaningJobRun::getUpdatedTime, now);
		return update(null, wrapper);
	}

	default int updateStatus(Long id, String status, LocalDateTime endedTime, LocalDateTime now) {
		LambdaUpdateWrapper<CleaningJobRun> wrapper = new LambdaUpdateWrapper<CleaningJobRun>()
			.eq(CleaningJobRun::getId, id)
			.set(CleaningJobRun::getStatus, status)
			.set(CleaningJobRun::getEndedTime, endedTime)
			.set(CleaningJobRun::getLeaseOwner, null)
			.set(CleaningJobRun::getLeaseUntil, null)
			.set(CleaningJobRun::getUpdatedTime, now);
		return update(null, wrapper);
	}

	default int updateStatusWithoutEnd(Long id, String status, LocalDateTime now) {
		LambdaUpdateWrapper<CleaningJobRun> wrapper = new LambdaUpdateWrapper<CleaningJobRun>()
			.eq(CleaningJobRun::getId, id)
			.set(CleaningJobRun::getStatus, status)
			.set(CleaningJobRun::getLeaseOwner, null)
			.set(CleaningJobRun::getLeaseUntil, null)
			.set(CleaningJobRun::getUpdatedTime, now);
		return update(null, wrapper);
	}

}
