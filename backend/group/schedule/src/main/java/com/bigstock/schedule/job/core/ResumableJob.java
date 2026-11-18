package com.bigstock.schedule.job.core;

/**
 * A resumable handler must honor the durable checkpoint and guarantee
 * idempotent replay.
 */
public interface ResumableJob extends ManagedJob {
	void resume(JobContext context) throws Exception;
}
