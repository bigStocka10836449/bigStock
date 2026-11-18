package com.bigstock.schedule.job.trigger;

import com.bigstock.schedule.job.core.JobExecutionManager;
import com.bigstock.schedule.job.repo.JobRepository;
import java.time.*;
import java.util.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.slf4j.*;

/** Dynamic DB Cron, no @Scheduled on individual business jobs. */
@Component
public class DatabaseCronTrigger {
	private static final Logger log = LoggerFactory.getLogger(DatabaseCronTrigger.class);
	private final JobRepository repo;
	private final JobExecutionManager manager;
	private final TransactionTemplate tx;

	public DatabaseCronTrigger(JobRepository repo, JobExecutionManager manager, TransactionTemplate tx) {
		this.repo = repo;
		this.manager = manager;
		this.tx = tx;
	}

	@Scheduled(fixedDelayString = "${bstock.jobs.cron-scan-ms:15000}")
	public void tick() {
		for (Map<String, Object> row : repo.dueCrons()) {
			String code = (String) row.get("job_code"), expr = (String) row.get("cron_expression");
			String zone = (String) row.get("timezone");
			OffsetDateTime due = (OffsetDateTime) row.get("next_fire_at");
			try {
				ZoneId z = ZoneId.of(zone);
				// Skip missed intermediate fire times after downtime; create at most one
				// catch-up run.
				ZonedDateTime next = CronExpression.parse(expr).next(ZonedDateTime.now(z));
				if (next == null)
					throw new IllegalStateException("Cron has no future fire time");
				OffsetDateTime n = next.toOffsetDateTime();
				tx.executeWithoutResult(s -> {
					if (!repo.advanceCron(code, due, n))
						return;
					// Same database transaction: next_fire_at and PENDING row committed together.
					// Duplicate active run causes rollback; cron remains due until overlap ends.
					manager.trigger(code, "SCHEDULED", "CRON:" + code + ":" + due.toInstant(), Map.of(), null);
				});
			} catch (Exception ex) {
				log.warn("Cron delayed job={} fire={}", code, due, ex);
			}
		}
	}

	public void update(String code, String expr, String zone) {
		ZoneId z = ZoneId.of(zone);
		ZonedDateTime next = CronExpression.parse(expr).next(ZonedDateTime.now(z));
		if (next == null)
			throw new IllegalArgumentException("Invalid cron next fire");
		repo.updateCron(code, expr, zone, next.toOffsetDateTime());
	}
}
