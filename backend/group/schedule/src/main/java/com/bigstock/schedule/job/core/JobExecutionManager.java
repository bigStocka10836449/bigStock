package com.bigstock.schedule.job.core;

import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import com.bigstock.schedule.job.model.JobRun;
import com.bigstock.schedule.job.redis.JobProgressRedis;
import com.bigstock.schedule.job.repo.JobRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class JobExecutionManager {
	private static final Logger log = LoggerFactory.getLogger(JobExecutionManager.class);
	private final JobRegistry registry;
	private final JobRepository repo;
	private final JobProgressRedis progress;
	private final RedissonClient redisson;
	private final ObjectMapper mapper;
	private final ThreadPoolTaskExecutor executor;
	private final String workerId = UUID.randomUUID() + "-" + ManagementFactory.getRuntimeMXBean().getName();
	@Value("${bstock.jobs.auto-recovery-enabled:false}")
	private boolean autoRecoveryEnabled;
	private final ConcurrentMap<UUID, JobContext> active = new ConcurrentHashMap<>();
	private final ConcurrentMap<UUID, Thread> threads = new ConcurrentHashMap<>();

	public JobExecutionManager(JobRegistry registry, JobRepository repo, JobProgressRedis progress,
			RedissonClient redisson, ObjectMapper mapper,
			@Qualifier("managedJobExecutor") ThreadPoolTaskExecutor executor) {
		this.registry = registry;
		this.repo = repo;
		this.progress = progress;
		this.redisson = redisson;
		this.mapper = mapper;
		this.executor = executor;
		for (ManagedJob job : registry.all())
			repo.register(job.code(), job.displayName());
	}

	public JobRun trigger(String code, String source, String key, Map<String, Object> params, UUID retryOf) {
		return create(code, source, key, params, retryOf, null, Map.of());
	}

	private JobRun create(String code, String source, String key, Map<String, Object> params, UUID retryOf,
			UUID resumeOf, Map<String, Object> checkpoint) {
		registry.required(code);
		if (!repo.enabled(code))
			throw new IllegalStateException("Job is disabled: " + code);
		if (key != null && !key.isBlank()) {
			Optional<JobRun> existing = repo.byKey(code, key);
			if (existing.isPresent())
				return existing.get();
		}
		UUID id = UUID.randomUUID();
		try {
			repo.insert(id, code, source, retryOf, resumeOf, key,
					mapper.writeValueAsString(params == null ? Map.of() : params),
					mapper.writeValueAsString(checkpoint));
		} catch (DataIntegrityViolationException ex) {
			Optional<JobRun> duplicate = repo.byKey(code, key);
			if (duplicate.isPresent())
				return duplicate.get();
			throw new IllegalStateException("Job is already active: " + code, ex);
		} catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
			throw new IllegalArgumentException("Invalid request JSON", ex);
		}
		dispatch(id);
		return repo.find(id).orElseThrow();
	}

	public JobRun retry(UUID original, String key) {
		JobRun old = find(original);
		if (!Set.of("FAILED", "PARTIAL_SUCCESS", "INTERRUPTED", "TIMED_OUT", "CANCELLED").contains(old.status()))
			throw new IllegalStateException("Execution not retryable: " + old.status());
		return create(old.jobCode(), "RETRY", key, originalParams(original), original, null, Map.of());
	}

	public JobRun resume(UUID original, String key) {
		JobRun old = find(original);
		if (!Set.of("FAILED", "PARTIAL_SUCCESS", "INTERRUPTED", "TIMED_OUT", "CANCELLED").contains(old.status()))
			throw new IllegalStateException("Execution not resumable: " + old.status());
		if (!(registry.required(old.jobCode()) instanceof ResumableJob))
			throw new IllegalStateException("Job has no safe resumable adapter: " + old.jobCode());
		Map<String, Object> cp = repo.checkpoint(original);
		if (cp.isEmpty())
			throw new IllegalStateException("No durable checkpoint");
		return create(old.jobCode(), "RESUME", key, originalParams(original), null, original, cp);
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> originalParams(UUID original) {
		try {
			return mapper.readValue((String) repo.runPayload(original).get("params"), Map.class);
		} catch (Exception e) {
			throw new IllegalStateException("Stored params invalid", e);
		}
	}

	public JobRun cancel(UUID id) {
		JobRun run = find(id);
		if (run.status().equals("PENDING"))
			repo.cancelPending(id);
		else if (run.status().equals("RUNNING")) {
			repo.requestCancel(id);
			// Intentional: no Thread.interrupt; original Yahoo/database operations finish
			// safely,
			// then JobContext.checkCancellation() throws at a known boundary.
		} else if (!run.status().equals("CANCEL_REQUESTED"))
			throw new IllegalStateException("Not cancellable: " + run.status());
		return find(id);
	}

	public JobRun find(UUID id) {
		return repo.find(id).orElseThrow(() -> new NoSuchElementException("Unknown execution " + id));
	}

	public List<JobRun> history(String code, int limit) {
		registry.required(code);
		return repo.history(code, Math.max(1, Math.min(limit, 200)));
	}

	public List<Map<String, Object>> jobs() {
		return repo.definitions();
	}

	@Scheduled(fixedDelayString = "${bstock.jobs.dispatch-scan-ms:10000}")
	public void dispatchPending() {
		for (JobRun run : repo.pending(20))
			dispatch(run.executionId());
	}

	private void dispatch(UUID id) {
		try {
			executor.execute(() -> execute(id));
		} catch (RuntimeException ex) {
			log.warn("Queue full; pending scanner will retry {}", id, ex);
		}
	}

	private void execute(UUID id) {
		JobRun run = repo.find(id).orElse(null);
		if (run == null)
			return;
		RLock lock = redisson.getLock("bstock:job:lock:" + run.jobCode());
		boolean acquired = false;
		try {
			acquired = lock.tryLock();
			if (!acquired)
				return;
			long epoch = repo.claim(id, workerId);
			if (epoch < 0)
				return;
			JobContext ctx = new JobContext(id, workerId, epoch, repo, progress);
			if ("RESUME".equals(run.triggerType())) {
				ctx.checkpoint(repo.checkpoint(id));
				Map<String, Object> cp = ctx.checkpoint();
				ctx.restoreCounts(0, asLong(cp.get("processed")), asLong(cp.get("success")), asLong(cp.get("failed")),
						asLong(cp.get("skipped")));
			}
			active.put(id, ctx);
			threads.put(id, Thread.currentThread());
			try {
				ctx.stage("RUNNING");
				ManagedJob job = registry.required(run.jobCode());
				if ("RESUME".equals(run.triggerType()))
					((ResumableJob) job).resume(ctx);
				else
					job.execute(ctx);
				ctx.checkCancellation();
				String status = ctx.failedCount() > 0 ? "PARTIAL_SUCCESS" : "SUCCESS";
				finish(ctx, status, "Completed");
			} catch (JobCancelledException ex) {
				finish(ctx, "CANCELLED", "Cancelled at safe checkpoint / lost execution ownership");
			} catch (Throwable ex) {
				log.error("Execution failed {}", id, ex);
				finish(ctx, "FAILED", ex.getClass().getSimpleName() + ": " + String.valueOf(ex.getMessage()));
			} finally {
				active.remove(id);
				threads.remove(id);
			}
		} catch (Exception e) {
			log.error("Job startup error {}", id, e);
		} finally {
			if (acquired && lock.isHeldByCurrentThread())
				lock.unlock();
		}
	}

	private static long asLong(Object val) {
		return val instanceof Number n ? n.longValue() : 0L;
	}

	private void finish(JobContext ctx, String status, String message) {
		boolean done = repo.finish(ctx.executionId(), ctx.owner(), ctx.epoch(), status, message, ctx.totalCount(),
				ctx.processedCount(), ctx.successCount(), ctx.failedCount(), ctx.skippedCount());
		if (done) {
			try {
				progress.put(ctx.executionId(), Map.of("status", status, "message", message));
			} catch (Exception e) {
				log.warn("Redis terminal progress failed", e);
			}
		} else
			log.error("Worker lost DB ownership, refusing terminal update: {}", ctx.executionId());
	}

	@Scheduled(fixedDelayString = "${bstock.jobs.heartbeat-ms:10000}")
	public void heartbeat() {
		for (JobContext ctx : active.values()) {
			try {
				ctx.heartbeat();
			} catch (Exception ex) {
				log.error("Lost heartbeat {}", ctx.executionId(), ex);
			}
		}
	}

	/**
	 * Conservative auto recovery: only after heartbeat timeout and lock is free.
	 * Requires a fenced/idempotent business writer; disabled by default.
	 */
	@Scheduled(fixedDelayString = "${bstock.jobs.recovery-scan-ms:60000}")
	public void recoverStale() {
		if (!autoRecoveryEnabled)
			return;
		for (JobRun run : repo.stale(5)) {
			RLock lock = redisson.getLock("bstock:job:lock:" + run.jobCode());
			boolean got = false;
			try {
				got = lock.tryLock();
				if (!got)
					continue;
				Map<String, Object> payload = repo.runPayload(run.executionId());
				String prev = (String) payload.get("worker_id");
				if (!repo.markLost(run.executionId(), prev, 5))
					continue;
				// Only resumable handlers may be replayed, and only with a durable checkpoint.
				if (registry.required(run.jobCode()) instanceof ResumableJob
						&& !repo.checkpoint(run.executionId()).isEmpty()) {
					try {
						resume(run.executionId(), "RECOVERY:" + run.executionId());
					} catch (Exception ex) {
						log.error("Recover requires manual intervention for {}", run.executionId(), ex);
					}
				}
			} catch (Exception ex) {
				log.error("Recovery check failed {}", run.executionId(), ex);
			} finally {
				if (got && lock.isHeldByCurrentThread())
					lock.unlock();
			}
		}
	}
}
