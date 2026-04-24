package com.bigstock.schedule.service;

import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.threeten.bp.LocalDate;

import com.bigstock.sharedComponent.entity.FinancialCalendar;
import com.bigstock.sharedComponent.redis.CacheOperatorService;
import com.bigstock.sharedComponent.service.FinancialCalendarService;
import com.bigstock.sharedComponent.service.GrabThirdPartyStockDayPrice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class GrabFinancialCalendarService {

	private final CacheOperatorService cacheOperatorService;
	
	private final GrabThirdPartyStockDayPrice grabThirdPartyStockDayPrice;
	
	private final FinancialCalendarService financialCalendarService;
	
	private final RestTemplate restTemplate = new RestTemplate();

//	@PostConstruct
	@Scheduled(cron = "0 45 21 * * ?", zone = "Asia/Taipei")
	public void grabFinancialCalendar() throws Exception {
		LocalDate ld = LocalDate.now();
		int year = ld.getYear();
		int month = ld.getMonth().getValue();
		List<FinancialCalendar> moneyDjFinancialCalendars = grabThirdPartyStockDayPrice.grabFinancialCalendar( ld.getYear(), ld.getMonth().getValue(), "moDj");
		cacheOperatorService.putSnapshotDataListAtomic("ultraLongLivedCache",
				"financialCalendar:" + year+ ":" + String.format("%02d", month) + ":moDj",
				moneyDjFinancialCalendars);
		month = month + 1;
		if((month + 1) > 12) {
			year = year +1;
			month = 1;
		}
		Thread.sleep(5000);
		List<FinancialCalendar> secondmoneyDjFinancialCalendars = grabThirdPartyStockDayPrice.grabFinancialCalendar( year, month, "moDj");
		cacheOperatorService.putSnapshotDataListAtomic("ultraLongLivedCache",
				"financialCalendar:" + year+ ":" + String.format("%02d", month) + ":moDj",
				secondmoneyDjFinancialCalendars);
		cacheOperatorService.cleanupOldSnapshots("ultraLongLivedCache",  "financialCalendar:" + year+ ":" + String.format("%02d", month) + ":moDj", 2);
		ld = ld.minusMonths(24);
		cacheOperatorService.delete("ultraLongLivedCache",
				"financialCalendar:" + ld.getYear() + ":" + String.format("%02d", ld.getMonth().getValue()) + ":moDj");
		cacheOperatorService.cleanupOldSnapshots("ultraLongLivedCache", "financialCalendar:" + ld.getYear() + ":" + String.format("%02d", ld.getMonth().getValue()) + ":moDj", 2);
		financialCalendarService.refreshData(moneyDjFinancialCalendars);
		financialCalendarService.refreshData(secondmoneyDjFinancialCalendars);
		
	}

}
