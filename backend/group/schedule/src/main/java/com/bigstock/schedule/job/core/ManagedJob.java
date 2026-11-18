package com.bigstock.schedule.job.core;

public interface ManagedJob {
	String code();

	String displayName();

	void execute(JobContext context) throws Exception;
}
