package com.bigstock.schedule.job.trigger;

import com.bigstock.schedule.job.repo.JobRepository;
import java.time.*;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

/**
 * Initializes legacy stock schedules only once; later API edits are preserved.
 */
@Component
public class InitialCronSetup {
	private final org.springframework.jdbc.core.JdbcTemplate jdbc;

	public InitialCronSetup(org.springframework.jdbc.core.JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void seed() {
		putIfMissing("STOCK_INTRADAY_SYNC", "0 50 18 * * *");
		putIfMissing("STOCK_DAILY_SYNC", "0 30 15 * * *");
		putIfMissing("MARGIN_TRADING_SYNC", "0 50 21 * * *");
	}

	private void putIfMissing(String job, String cron) {
		ZoneId zone = ZoneId.of("Asia/Taipei");
		ZonedDateTime next = CronExpression.parse(cron).next(ZonedDateTime.now(zone));
		jdbc.update("""
				UPDATE bstock.job_definition SET cron_expression=?,timezone='Asia/Taipei',next_fire_at=?
				WHERE job_code=? AND cron_expression IS NULL
				""", cron, next.toOffsetDateTime(), job);
	}
}
