package com.bigstock.schedule.job.repo;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.bigstock.schedule.job.model.JobRun;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class JobRepository {
	private final JdbcTemplate jdbc;
	private final ObjectMapper mapper;
	private final JdbcTemplate jdbcTemplate;

	/**
	 * 初始化預設 Cron。
	 *
	 * 只有 cron_expression 為 NULL 時才更新。
	 *
	 * PostgreSQL 單一 UPDATE 具有原子性； 多個 Spring Boot 實例同時啟動時， 不會覆蓋已經完成初始化的 Cron。
	 */
	public int initDefaultCron(String jobCode, String cron, String timezone, Instant nextFireAt) {

		String sql = """
				UPDATE bstock.job_definition
				SET
				    cron_expression = ?,
				    timezone = ?,
				    next_fire_at = ?,
				    updated_at = NOW()
				WHERE job_code = ?
				  AND cron_expression IS NULL
				""";

		return jdbcTemplate.update(sql, cron, timezone, Timestamp.from(nextFireAt), jobCode);
	}

	public void register(String code, String name) {
		jdbc.update("INSERT INTO bstock.job_definition(job_code,job_name) VALUES(?,?) ON CONFLICT(job_code) DO NOTHING",
				code, name);
	}

	public List<Map<String, Object>> definitions() {
		return jdbc.queryForList(
				"SELECT job_code,job_name,description,enabled,config,cron_expression,timezone,next_fire_at FROM bstock.job_definition ORDER BY job_code");
	}

	public boolean enabled(String code) {
		return Boolean.TRUE.equals(jdbc
				.query("SELECT enabled FROM bstock.job_definition WHERE job_code=?", (rs, n) -> rs.getBoolean(1), code)
				.stream().findFirst().orElse(false));
	}

	public Optional<JobRun> find(UUID id) {
		return jdbc.query("SELECT * FROM bstock.job_execution WHERE execution_id=?", (rs, n) -> map(rs), id).stream()
				.findFirst();
	}

	public Optional<JobRun> byKey(String code, String key) {
		if (key == null)
			return Optional.empty();
		return jdbc.query("SELECT * FROM bstock.job_execution WHERE job_code=? AND idempotency_key=?",
				(rs, n) -> map(rs), code, key).stream().findFirst();
	}

	public List<JobRun> history(String code, int limit) {
		return jdbc.query("SELECT * FROM bstock.job_execution WHERE job_code=? ORDER BY created_at DESC LIMIT ?",
				(rs, n) -> map(rs), code, limit);
	}

	public List<JobRun> pending(int limit) {
		return jdbc.query("SELECT * FROM bstock.job_execution WHERE status='PENDING' ORDER BY created_at LIMIT ?",
				(rs, n) -> map(rs), limit);
	}

	public List<JobRun> stale(int minutes) {
		return jdbc.query(
				"SELECT * FROM bstock.job_execution WHERE status IN ('RUNNING','CANCEL_REQUESTED') AND heartbeat_at < now() - (? * interval '1 minute')",
				(rs, n) -> map(rs), minutes);
	}

	public void insert(UUID id, String code, String trigger, UUID retryOf, UUID resumeOf, String key, String params,
			String checkpoint) {
		jdbc.update(
				"""
						INSERT INTO bstock.job_execution(execution_id,job_code,status,trigger_type,retry_of,resume_of,idempotency_key,params,checkpoint)
						VALUES(?,?,'PENDING',?,?,?,?,cast(? as jsonb),cast(? as jsonb))
						""",
				id, code, trigger, retryOf, resumeOf, key, params, checkpoint);
	}

	public long claim(UUID id, String worker) {
		List<Long> r = jdbc.query("""
				UPDATE bstock.job_execution SET status='RUNNING',worker_id=?,owner_epoch=owner_epoch+1,
				started_at=coalesce(started_at,now()),heartbeat_at=now(),cancel_requested=false
				WHERE execution_id=? AND status='PENDING' RETURNING owner_epoch
				""", (rs, n) -> rs.getLong(1), worker, id);
		return r.isEmpty() ? -1 : r.get(0);
	}

	public boolean isOwnerAndActive(UUID id, String worker, long epoch) {
		return !jdbc.query(
				"SELECT 1 FROM bstock.job_execution WHERE execution_id=? AND worker_id=? AND owner_epoch=? AND status IN ('RUNNING','CANCEL_REQUESTED')",
				(rs, n) -> 1, id, worker, epoch).isEmpty();
	}

	public boolean cancelRequested(UUID id) {
		return Boolean.TRUE.equals(jdbc.query("SELECT cancel_requested FROM bstock.job_execution WHERE execution_id=?",
				(rs, n) -> rs.getBoolean(1), id).stream().findFirst().orElse(false));
	}

	public boolean requestCancel(UUID id) {
		return jdbc.update(
				"UPDATE bstock.job_execution SET cancel_requested=true,status='CANCEL_REQUESTED' WHERE execution_id=? AND status='RUNNING'",
				id) == 1;
	}

	public boolean cancelPending(UUID id) {
		return jdbc.update(
				"UPDATE bstock.job_execution SET cancel_requested=true,status='CANCELLED',finished_at=now() WHERE execution_id=? AND status='PENDING'",
				id) == 1;
	}

	public boolean heartbeat(UUID id, String worker, long epoch) {
		return jdbc.update(
				"UPDATE bstock.job_execution SET heartbeat_at=now() WHERE execution_id=? AND worker_id=? AND owner_epoch=? AND status IN ('RUNNING','CANCEL_REQUESTED')",
				id, worker, epoch) == 1;
	}

	public boolean progress(UUID id, String worker, long epoch, long total, long processed, long ok, long failed,
			long skipped, String stage, String message) {
		return jdbc.update(
				"""
						UPDATE bstock.job_execution SET total_items=?,processed_items=?,success_items=?,failed_items=?,skipped_items=?,
						progress=CASE WHEN ?=0 THEN 0 ELSE LEAST(99.99,ROUND((?::numeric*100)/?,2)) END,
						current_stage=?,message=? WHERE execution_id=? AND worker_id=? AND owner_epoch=? AND status='RUNNING'
						""",
				total, processed, ok, failed, skipped, total, processed, total, stage, message, id, worker, epoch) == 1;
	}

	public boolean finish(UUID id, String worker, long epoch, String status, String message, long total, long processed,
			long ok, long failed, long skipped) {
		return jdbc.update("""
				UPDATE bstock.job_execution SET status=?,message=?,finished_at=now(),heartbeat_at=now(),
				total_items=?,processed_items=?,success_items=?,failed_items=?,skipped_items=?,
				progress=CASE WHEN ? IN ('SUCCESS','PARTIAL_SUCCESS') THEN 100 ELSE progress END
				WHERE execution_id=? AND worker_id=? AND owner_epoch=? AND status IN ('RUNNING','CANCEL_REQUESTED')
				""", status, message, total, processed, ok, failed, skipped, status, id, worker, epoch) == 1;
	}

	public void terminalPending(UUID id, String status, String reason) {
		jdbc.update(
				"UPDATE bstock.job_execution SET status=?,message=?,finished_at=now() WHERE execution_id=? AND status='PENDING'",
				status, reason, id);
	}

	// Recovery: before changing row, manager MUST obtain the Redis lock; heartbeat
	// is only a candidate signal.
	public boolean markLost(UUID id, String priorWorker, int minutes) {
		return jdbc.update(
				"""
						UPDATE bstock.job_execution SET status='INTERRUPTED',message='Owner unreachable after stale heartbeat',finished_at=now(),owner_epoch=owner_epoch+1
						WHERE execution_id=? AND worker_id=? AND status IN ('RUNNING','CANCEL_REQUESTED')
						AND heartbeat_at < now() - (? * interval '1 minute')
						""",
				id, priorWorker, minutes) == 1;
	}

	public Map<String, Object> checkpoint(UUID id) {
		String json = jdbc.queryForObject("SELECT checkpoint::text FROM bstock.job_execution WHERE execution_id=?",
				String.class, id);
		try {
			return json == null ? Map.of() : mapper.readValue(json, new TypeReference<Map<String, Object>>() {
			});
		} catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	public boolean saveCheckpoint(UUID id, String worker, long epoch, String json) {
		return jdbc.update("""
				UPDATE bstock.job_execution SET checkpoint=cast(? as jsonb)
				WHERE execution_id=? AND worker_id=? AND owner_epoch=? AND status='RUNNING'
				""", json, id, worker, epoch) == 1;
	}

	public void updateCron(String code, String expr, String zone, OffsetDateTime next) {
		int n = jdbc.update(
				"UPDATE bstock.job_definition SET cron_expression=?,timezone=?,next_fire_at=?,updated_at=now() WHERE job_code=?",
				expr, zone, next, code);
		if (n != 1)
			throw new NoSuchElementException("Job not found: " + code);
	}

	public void setEnabled(String code, boolean enabled) {
		int n = jdbc.update("UPDATE bstock.job_definition SET enabled=?,updated_at=now() WHERE job_code=?", enabled,
				code);
		if (n != 1)
			throw new NoSuchElementException(code);
	}

	public List<Map<String, Object>> dueCrons() {
		return jdbc.query(
				"SELECT job_code,cron_expression,timezone,next_fire_at FROM bstock.job_definition WHERE enabled=true AND cron_expression IS NOT NULL AND next_fire_at<=now() ORDER BY next_fire_at LIMIT 20",
				(rs, i) -> {
					Map<String, Object> m = new HashMap<>();
					m.put("job_code", rs.getString("job_code"));
					m.put("cron_expression", rs.getString("cron_expression"));
					m.put("timezone", rs.getString("timezone"));
					m.put("next_fire_at", rs.getObject("next_fire_at", OffsetDateTime.class));
					return m;
				});
	}

	public boolean advanceCron(String code, OffsetDateTime oldTime, OffsetDateTime next) {
		return jdbc.update("UPDATE bstock.job_definition SET next_fire_at=? WHERE job_code=? AND next_fire_at=?", next,
				code, oldTime) == 1;
	}

	public Map<String, Object> runPayload(UUID id) {
		return jdbc.queryForMap(
				"SELECT params::text AS params,checkpoint::text AS checkpoint,resume_of,worker_id,owner_epoch,status,total_items,processed_items,success_items,failed_items,skipped_items FROM bstock.job_execution WHERE execution_id=?",
				id);
	}

	private static JobRun map(ResultSet rs) throws SQLException {
		return new JobRun((UUID) rs.getObject("execution_id"), rs.getString("job_code"), rs.getString("status"),
				rs.getString("trigger_type"), (UUID) rs.getObject("retry_of"), rs.getDouble("progress"),
				rs.getLong("total_items"), rs.getLong("processed_items"), rs.getLong("success_items"),
				rs.getLong("failed_items"), rs.getLong("skipped_items"), rs.getString("current_stage"),
				rs.getString("message"), rs.getObject("created_at", OffsetDateTime.class),
				rs.getObject("started_at", OffsetDateTime.class), rs.getObject("finished_at", OffsetDateTime.class),
				rs.getObject("heartbeat_at", OffsetDateTime.class));
	}
}
