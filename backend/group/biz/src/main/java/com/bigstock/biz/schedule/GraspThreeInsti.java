package com.bigstock.biz.schedule;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.bigstock.biz.utils.ChromeDriverUtils;
import com.bigstock.sharedComponent.dto.ThreeInstitutionalTradingResponse;
import com.bigstock.sharedComponent.entity.StockDayPrice;
import com.bigstock.sharedComponent.service.StockThreeInstitutionalTradingService;

import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class GraspThreeInsti {

	private final StockThreeInstitutionalTradingService stockThreeInstitutionalTradingService;

	@Scheduled(cron = "${schedule.task.scheduling.cron.expression.update-threeInsti}", zone = "Asia/Taipei")
	@Transactional
//	@PostConstruct
	public void updateThreeInsti() throws Exception {

		List<StockDayPrice> stockTpexDayPrices = ChromeDriverUtils
				.graspTpexDayPrice("https://www.tpex.org.tw/openapi/v1/tpex_mainboard_quotes");

		Date tradeDate = stockTpexDayPrices.stream().findFirst().get().getTradingDay();
		LocalDate lod = tradeDate.toInstant().atZone(ZoneId.of("Asia/Taipei")).toLocalDate();
		String yyyyMMdd =
		        lod.getYear()
		        + (lod.getMonthValue() <= 9 ? "0" + lod.getMonthValue() : String.valueOf(lod.getMonthValue()))
		        + (lod.getDayOfMonth() <= 9 ? "0" + lod.getDayOfMonth() : String.valueOf(lod.getDayOfMonth()));
		
		List<ThreeInstitutionalTradingResponse> twseThreeInstitutionalTradingResponses = ChromeDriverUtils
				.grabThreeInstiTwse(yyyyMMdd);
		List<ThreeInstitutionalTradingResponse> tpexThreeInstitutionalTradingResponses = ChromeDriverUtils
				.grabThreeInstiTpex(yyyyMMdd);
		stockThreeInstitutionalTradingService.syncFromRedisToDb(yyyyMMdd, twseThreeInstitutionalTradingResponses, tpexThreeInstitutionalTradingResponses);
	}
}
