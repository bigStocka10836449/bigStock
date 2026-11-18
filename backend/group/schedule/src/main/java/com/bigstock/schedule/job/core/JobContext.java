package com.bigstock.schedule.job.core;

import com.bigstock.schedule.job.repo.JobRepository;
import com.bigstock.schedule.job.redis.JobProgressRedis;
import java.util.*;
import org.slf4j.*;

/**
 * Thread-safe observations; checkpoint must be committed in the business
 * transaction, not here.
 */
public final class JobContext {
	private static final Logger log = LoggerFactory.getLogger(JobContext.class);
	private final UUID id;
	private final String worker;
	private final long epoch;
	private final JobRepository repo;
	private final JobProgressRedis redis;
	private long total, processed, success, failed, skipped;
	private String stage = "STARTING", message = "";
	private Map<String, Object> checkpoint = Map.of();

	public JobContext(UUID id, String worker, long epoch, JobRepository repo, JobProgressRedis redis) {
		this.id = id;
		this.worker = worker;
		this.epoch = epoch;
		this.repo = repo;
		this.redis = redis;
	}

	public UUID executionId() {
		return id;
	}

	public String owner() {
		return worker;
	}

	public long epoch() {
		return epoch;
	}

	public synchronized void stage(String value) {
		stage = value;
		publish();
	}

	public synchronized void total(long value) {
		total = Math.max(value, 0);
		publish();
	}

	public synchronized void addTotal(long value) {
		total += Math.max(0, value);
		publish();
	}

	public synchronized void batchResult(long items, long succeeded, long failures, long ignored, String description) {
		checkCancellation();
		processed += items;
		success += succeeded;
		failed += failures;
		skipped += ignored;
		message = description;
		publish();
	}

	public void checkCancellation() {
		if (Thread.currentThread().isInterrupted())
			throw new JobCancelledException();
		if (!repo.isOwnerAndActive(id, worker, epoch))
			throw new JobCancelledException();
		if (repo.cancelRequested(id))
			throw new JobCancelledException();
	}

	public void recordError(String item, String description) {
		try {
			redis.appendError(id, item,
					description == null ? "unknown" : description.substring(0, Math.min(description.length(), 300)));
		} catch (Exception e) {
			log.warn("Could not write job error {}", id, e);
		}
	}

	public void heartbeat() {
		if (!repo.heartbeat(id, worker, epoch))
			throw new JobCancelledException();
	}

	public synchronized void publish() {
		if (!repo.progress(id, worker, epoch, total, processed, success, failed, skipped, stage, message))
			throw new JobCancelledException();
		try {
			redis.put(id,
					Map.of("status", "RUNNING", "stage", stage, "total", Long.toString(total), "processed",
							Long.toString(processed), "success", Long.toString(success), "failed",
							Long.toString(failed), "skipped", Long.toString(skipped), "message", message));
		} catch (Exception e) {
			log.warn("Redis progress unavailable: {}", id, e);
		}
	}

	public synchronized void restoreCounts(long total, long processed, long success, long failed, long skipped) {
		this.total = total;
		this.processed = processed;
		this.success = success;
		this.failed = failed;
		this.skipped = skipped;
	}

	public synchronized void checkpoint(Map<String, Object> cp) {
		this.checkpoint = Map.copyOf(cp);
	}

	public synchronized Map<String, Object> checkpoint() {
		return checkpoint;
	}

	/**
	 * Stock-specific convenience for integration adapter, not required by other
	 * jobs.
	 */
	public synchronized boolean shouldSkipStockBatch(String stockType, int start) {
		Object cpType = checkpoint.get("stockType"), cpNext = checkpoint.get("nextStart");
		if (cpType == null || !(cpNext instanceof Number))
			return false;
		int previous = Integer.parseInt(cpType.toString()), current = Integer.parseInt(stockType);
		return current < previous || (current == previous && start < ((Number) cpNext).intValue());
	}

	public synchronized long totalCount() {
		return total;
	}

	public synchronized long processedCount() {
		return processed;
	}

	public synchronized long successCount() {
		return success;
	}

	public synchronized long failedCount() {
		return failed;
	}

	public synchronized long skippedCount() {
		return skipped;
	}
}
