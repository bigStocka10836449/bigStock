package com.bigstock.schedule.job.handlers;

import com.bigstock.schedule.job.core.*;
import com.bigstock.schedule.service.GrabFromThirdParty;
import org.springframework.stereotype.Component;

/**
 * Enable ResumableJob ONLY after the INTEGRATION_V2 contract has been applied.
 * This adapter delegates without changing stock calculation logic.
 */
@Component
public class StockIntradayJob implements ResumableJob {
	private final GrabFromThirdParty service;

	public StockIntradayJob(GrabFromThirdParty service) {
		this.service = service;
	}

	public String code() {
		return "STOCK_INTRADAY_SYNC";
	}

	public String displayName() {
		return "盤中 K 線同步";
	}

	public void execute(JobContext ctx) throws Exception {
		run(ctx);
	}

	public void resume(JobContext ctx) throws Exception {
		run(ctx);
	}

	private void run(JobContext ctx) throws Exception {
		ctx.stage("TPEX");
		service.calculateIntradayManaged("0", ctx);
		ctx.checkCancellation();
		ctx.stage("TWSE");
		service.calculateIntradayManaged("1", ctx);
		ctx.checkCancellation();
		ctx.stage("OES");
		service.calculateIntradayManaged("2", ctx);
	}
}
