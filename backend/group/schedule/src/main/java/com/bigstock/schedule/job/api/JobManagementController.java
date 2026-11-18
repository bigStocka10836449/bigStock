package com.bigstock.schedule.job.api;

import com.bigstock.schedule.job.core.JobExecutionManager;
import com.bigstock.schedule.job.model.JobRun;
import com.bigstock.schedule.job.repo.JobRepository;
import com.bigstock.schedule.job.redis.JobProgressRedis;
import com.bigstock.schedule.job.trigger.DatabaseCronTrigger;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/schedule")
public class JobManagementController {
	private final JobExecutionManager manager;
	private final JobProgressRedis progress;
	private final DatabaseCronTrigger cron;
	private final JobRepository repo;

	public JobManagementController(JobExecutionManager manager, JobProgressRedis progress, DatabaseCronTrigger cron,
			JobRepository repo) {
		this.manager = manager;
		this.progress = progress;
		this.cron = cron;
		this.repo = repo;
	}

	@GetMapping("/jobs")
	public List<Map<String, Object>> jobs() {
		return manager.jobs();
	}

	@PostMapping("/jobs/{code}/executions")
	public ResponseEntity<JobRun> trigger(@PathVariable String code, @RequestHeader("Idempotency-Key") String key,
			@RequestBody(required = false) Map<String, Object> params) {
		requireKey(key);
		return ResponseEntity.accepted().body(manager.trigger(code, "MANUAL", key, params, null));
	}

	@GetMapping("/jobs/{code}/executions")
	public List<JobRun> history(@PathVariable String code, @RequestParam(defaultValue = "30") int limit) {
		return manager.history(code, limit);
	}

	@GetMapping("/executions/{id}")
	public JobRun get(@PathVariable UUID id) {
		return manager.find(id);
	}

	@GetMapping("/executions/{id}/progress")
	public Map<Object, Object> progress(@PathVariable UUID id) {
		manager.find(id);
		return progress.get(id);
	}

	@GetMapping("/executions/{id}/errors")
	public List<String> errors(@PathVariable UUID id) {
		manager.find(id);
		return progress.errors(id);
	}

	@GetMapping("/executions/{id}/checkpoint")
	public Map<String, Object> checkpoint(@PathVariable UUID id) {
		manager.find(id);
		return repo.checkpoint(id);
	}

	@PostMapping("/executions/{id}/retry")
	public ResponseEntity<JobRun> retry(@PathVariable UUID id, @RequestHeader("Idempotency-Key") String key) {
		requireKey(key);
		return ResponseEntity.accepted().body(manager.retry(id, key));
	}

	@PostMapping("/executions/{id}/resume")
	public ResponseEntity<JobRun> resume(@PathVariable UUID id, @RequestHeader("Idempotency-Key") String key) {
		requireKey(key);
		return ResponseEntity.accepted().body(manager.resume(id, key));
	}

	@PostMapping("/executions/{id}/cancel")
	public ResponseEntity<JobRun> cancel(@PathVariable UUID id) {
		return ResponseEntity.accepted().body(manager.cancel(id));
	}

	public record CronSettings(String cron, String timezone) {
	}

	@PutMapping("/jobs/{code}/schedule")
	public Map<String, Object> schedule(@PathVariable String code, @RequestBody CronSettings settings) {
		cron.update(code, settings.cron(), settings.timezone());
		return Map.of("jobCode", code, "cron", settings.cron(), "timezone", settings.timezone());
	}

	public record EnableRequest(boolean enabled) {
	}

	@PutMapping("/jobs/{code}/enabled")
	public Map<String, Object> enabled(@PathVariable String code, @RequestBody EnableRequest value) {
		repo.setEnabled(code, value.enabled());
		return Map.of("jobCode", code, "enabled", value.enabled());
	}

	private static void requireKey(String key) {
		if (key.isBlank() || key.length() > 200)
			throw new IllegalArgumentException("Idempotency-Key must have 1..200 chars");
	}
}
