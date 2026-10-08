package com.bigstock.schedule.service;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.collections4.CollectionUtils;
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


//	@PostConstruct
	@Scheduled(cron = "0 45 21 * * ?", zone = "Asia/Taipei")
	public void grabFinancialCalendar() throws Exception {

		LocalDate ld = LocalDate.now();

		int year = ld.getYear();
		int month = ld.getMonthValue();

		// 1. Grab current month
		List<FinancialCalendar> moneyDjFinancialCalendars = grabThirdPartyStockDayPrice.grabFinancialCalendar(year,
				month, "moDj");

		// Deduplicate current month
		moneyDjFinancialCalendars = deduplicateFinancialCalendars(moneyDjFinancialCalendars);

		// 2. Grab next month
		LocalDate nextMonthDate = ld.plusMonths(1);

		int nextYear = nextMonthDate.getYear();
		int nextMonth = nextMonthDate.getMonthValue();

		Thread.sleep(5000);

		List<FinancialCalendar> secondmoneyDjFinancialCalendars = grabThirdPartyStockDayPrice
				.grabFinancialCalendar(nextYear, nextMonth, "moDj");

		// Deduplicate next month
		secondmoneyDjFinancialCalendars = deduplicateFinancialCalendars(secondmoneyDjFinancialCalendars);

		// 3. Remove records already existing in current month
		Set<String> currentMonthIds = moneyDjFinancialCalendars.stream().map(FinancialCalendar::getId)
				.collect(Collectors.toSet());

		int originalNextMonthSize = secondmoneyDjFinancialCalendars.size();

		secondmoneyDjFinancialCalendars = secondmoneyDjFinancialCalendars.stream()
				.filter(calendar -> !currentMonthIds.contains(calendar.getId())).toList();

		log.info("Cross-month FinancialCalendar deduplication, " + "removed={}, remaining={}",
				originalNextMonthSize - secondmoneyDjFinancialCalendars.size(), secondmoneyDjFinancialCalendars.size());

		// 4. Save current month to Redis
		String currentCacheKey = "financialCalendar:" + year + ":" + String.format("%02d", month) + ":moDj";

		cacheOperatorService.putSnapshotDataListAtomic("ultraLongLivedCache", currentCacheKey,
				moneyDjFinancialCalendars);

		cacheOperatorService.cleanupOldSnapshots("ultraLongLivedCache", currentCacheKey, 2);

		// 5. Save next month to Redis
		String nextCacheKey = "financialCalendar:" + nextYear + ":" + String.format("%02d", nextMonth) + ":moDj";

		cacheOperatorService.putSnapshotDataListAtomic("ultraLongLivedCache", nextCacheKey,
				secondmoneyDjFinancialCalendars);

		cacheOperatorService.cleanupOldSnapshots("ultraLongLivedCache", nextCacheKey, 2);

		// 6. Delete old cache
		LocalDate oldDate = ld.minusMonths(24);

		String oldCacheKey = "financialCalendar:" + oldDate.getYear() + ":"
				+ String.format("%02d", oldDate.getMonthValue()) + ":moDj";

		cacheOperatorService.delete("ultraLongLivedCache", oldCacheKey);

		cacheOperatorService.cleanupOldSnapshots("ultraLongLivedCache", oldCacheKey, 2);

		// 7. Refresh database
		financialCalendarService.refreshData(moneyDjFinancialCalendars);
		financialCalendarService.refreshData(secondmoneyDjFinancialCalendars);
	}
	private List<FinancialCalendar> deduplicateFinancialCalendars(
	        List<FinancialCalendar> calendars) {

	    if (CollectionUtils.isEmpty(calendars)) {
	        return new ArrayList<>();
	    }

	    Map<String, FinancialCalendar> uniqueMap =
	            new LinkedHashMap<>();

	    for (FinancialCalendar calendar : calendars) {

	        if (calendar == null
	                || calendar.getId() == null
	                || calendar.getId().isBlank()) {
	            continue;
	        }

	        FinancialCalendar existing =
	                uniqueMap.putIfAbsent(
	                        calendar.getId(),
	                        calendar
	                );

	        if (existing != null) {
	            log.warn(
	                    "Duplicate FinancialCalendar, id={}, title={}",
	                    calendar.getId(),
	                    calendar.getTitle()
	            );
	        }
	    }

	    return new ArrayList<>(uniqueMap.values());
	}

}
