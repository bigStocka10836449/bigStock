package com.bigstock.schedule.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.bigstock.sharedComponent.client.TpexQuarterlyFinancialClient;
import com.bigstock.sharedComponent.client.TwseQuarterlyFinancialClient;
import com.bigstock.sharedComponent.dto.QuarterlyFinancialVo;
import com.bigstock.sharedComponent.service.QuarterlyFinancialRedisService;
import com.bigstock.sharedComponent.service.StockFinancialRedisService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class GrabQuarterlyFinancial {

	private final TpexQuarterlyFinancialClient tpexClient;
	private final TwseQuarterlyFinancialClient twseClient;
	private final QuarterlyFinancialRedisService quarterlyFinancialRedisService;
	private final StockFinancialRedisService stockFinancialRedisService;

//	@PostConstruct
	@Scheduled(cron = "0 30 19 * * ?", zone = "Asia/Taipei")
	public void syncToDb() throws InterruptedException {

		List<YearQuarter> yearQuarters = GrabQuarterlyFinancial.resolveLastTwoReportQuarters();

		for (YearQuarter yearQuarter : yearQuarters) {

			int year = yearQuarter.year();
			int quarter = yearQuarter.quarter();

			// ==========================================
			// 1. TWSE
			// ==========================================

			List<QuarterlyFinancialVo> twseList = new ArrayList<>();

			try {

				twseList = twseClient.fetch(year, quarter);

			} catch (Exception e) {

				log.error("[FINANCIAL SYNC] TWSE fetch failed: year={}, quarter={}", year, quarter, e);
			}

			// ==========================================
			// 2. TPEX
			// ==========================================

			List<QuarterlyFinancialVo> tpexList = new ArrayList<>();

			try {

				tpexList = tpexClient.fetch(year, quarter);

			} catch (Exception e) {

				log.error("[FINANCIAL SYNC] TPEX fetch failed: year={}, quarter={}", year, quarter, e);
			}

			// ==========================================
			// 3. TWSE Redis 更新
			// ==========================================

			if (twseList != null && !twseList.isEmpty()) {

				try {

					// 原本：市場季度 Snapshot
					quarterlyFinancialRedisService.write(year, quarter, "TWSE", twseList);

					// 新增：個股完整歷史財報
					stockFinancialRedisService.mergeQuarter(year, quarter, "TWSE", twseList);

					log.info("[FINANCIAL SYNC] TWSE completed: year={}, quarter={}, size={}", year, quarter,
							twseList.size());

				} catch (Exception e) {

					log.error("[FINANCIAL SYNC] TWSE Redis update failed: year={}, quarter={}", year, quarter, e);
				}
			}

			// ==========================================
			// 4. TPEX Redis 更新
			// ==========================================

			if (tpexList != null && !tpexList.isEmpty()) {

				try {

					// 原本：市場季度 Snapshot
					quarterlyFinancialRedisService.write(year, quarter, "TPEX", tpexList);

					// 新增：個股完整歷史財報
					stockFinancialRedisService.mergeQuarter(year, quarter, "TPEX", tpexList);

					log.info("[FINANCIAL SYNC] TPEX completed: year={}, quarter={}, size={}", year, quarter,
							tpexList.size());

				} catch (Exception e) {

					log.error("[FINANCIAL SYNC] TPEX Redis update failed: year={}, quarter={}", year, quarter, e);
				}
			}

			Thread.sleep(3000);
		}
	}

	private static YearQuarter previousQuarter(YearQuarter current) {
		int year = current.year();
		int quarter = current.quarter();

		if (quarter == 1) {
			return new YearQuarter(year - 1, 4);
		}
		return new YearQuarter(year, quarter - 1);
	}

	public static List<YearQuarter> resolveLastTwoReportQuarters() {

		LocalDate now = LocalDate.now();

		int year = now.getYear();
		int month = now.getMonthValue();

		int currentQuarter = (month - 1) / 3 + 1;

		YearQuarter current = new YearQuarter(year, currentQuarter);

		YearQuarter last = previousQuarter(current);
		YearQuarter secondLast = previousQuarter(last);

		return List.of(last, secondLast);
	}

	public static class YearQuarter {

		private final int year;
		private final int quarter;

		public YearQuarter(int year, int quarter) {
			this.year = year;
			this.quarter = quarter;
		}

		public int year() {
			return year;
		}

		public int quarter() {
			return quarter;
		}
	}
}
