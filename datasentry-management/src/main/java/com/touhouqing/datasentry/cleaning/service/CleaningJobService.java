package com.touhouqing.datasentry.cleaning.service;

import com.touhouqing.datasentry.cleaning.dto.CleaningJobCreateRequest;
import com.touhouqing.datasentry.cleaning.model.CleaningJob;
import com.touhouqing.datasentry.cleaning.model.CleaningJobRun;

public interface CleaningJobService {

	CleaningJob createJob(CleaningJobCreateRequest request);

	CleaningJob getJob(Long jobId);

	CleaningJobRun createRun(Long jobId);

	CleaningJobRun getRun(Long runId);

	CleaningJobRun pauseRun(Long runId);

	CleaningJobRun resumeRun(Long runId);

	CleaningJobRun cancelRun(Long runId);

}
