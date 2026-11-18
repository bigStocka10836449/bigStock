package com.bigstock.schedule.job.core;

public class JobCancelledException extends RuntimeException {
	public JobCancelledException() {
		super("Cancellation requested");
	}
}
