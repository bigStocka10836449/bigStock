package com.bigstock.schedule.job.handlers;

import com.bigstock.schedule.job.core.*;
import com.bigstock.schedule.service.GrabFromThirdParty;
import org.springframework.stereotype.Component;

@Component
public class StockDailyJob implements ManagedJob {
	private final GrabFromThirdParty service;

	public StockDailyJob(GrabFromThirdParty service) {
		this.service = service;
	}

	public String code() {
		return "STOCK_DAILY_SYNC";
	}

	public String displayName() {
		return "日週月 K 線與排名同步";
	}

	public void execute(JobContext ctx) throws Exception {
		ctx.stage("DAILY_SYNC");
		service.updateStockDayPriceByThirdParty();
		ctx.stage("DAILY_SYNC_DONE");
	}
}
