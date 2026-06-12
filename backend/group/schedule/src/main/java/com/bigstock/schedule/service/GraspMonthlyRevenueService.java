package com.bigstock.schedule.service;

import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import com.bigstock.sharedComponent.dto.MonthlyRevenueVo;
import com.bigstock.sharedComponent.dto.StockRevenueResponse;
import com.bigstock.sharedComponent.entity.StockDayPrice;
import com.bigstock.sharedComponent.service.MonthlyRevenueQueryService;
import com.bigstock.sharedComponent.service.StockDayPriceService;
import com.bigstock.sharedComponent.service.StockRevenueService;
import com.bigstock.sharedComponent.utils.ChromeDriverUtils;
import com.bigstock.sharedComponent.utils.MonthlyRevenueMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
// LOCAL_RANK_TEST_DISABLED: @EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class GraspMonthlyRevenueService {

	private final StockDayPriceService stockDayPriceService;
	private final MonthlyRevenueQueryService monthlyRevenueQueryService;
	private final StockRevenueService stockRevenueService;

	// LOCAL_RANK_TEST_DISABLED: @Scheduled(cron = "${schedule.task.scheduling.cron.expression.update-monthlyRevenue}", zone = "Asia/Taipei")
	@Transactional
//	@PostConstruct
	public void updateMonthlyRevenue() throws RestClientException, URISyntaxException,
			JsonMappingException, JsonProcessingException, InterruptedException {

		Date currentTradeDate = stockDayPriceService.getCurrentTradeDate();
		LocalDate lod = currentTradeDate.toInstant()
	      .atZone(ZoneId.of("Asia/Taipei"))
	      .toLocalDate();
		lod = lod.minusMonths(1);
		String ym = normalizeToYyyyMm(lod.getYear() + "-" + (lod.getMonthValue() <= 9 ? "0" + lod.getMonthValue() :lod.getMonthValue() ));
		List<MonthlyRevenueVo> merged = monthlyRevenueQueryService
				.fetchMonthlyRevenueToRedis(ym);

		List<StockRevenueResponse> mapped = merged.stream().map(MonthlyRevenueMapper::toStockRevenueResponse).toList();

		// 你 shared-component 的方法是 (yearMonth, twse, tpex)
		// 但我們這裡 merged 已經混合 TWSE/TPEX，所以直接全塞到第一個 list，第二個給空即可
		// （shared service 內部會用 market 來組 PK）
		stockRevenueService.upsertFromNormList(ym, mapped, List.of());
	}
    private static String normalizeToYyyyMm(String ym) {
        if (ym == null || ym.isBlank()) {
            return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        }
        String s = ym.trim();
        if (s.matches("^\\d{6}$")) return s.substring(0, 4) + "-" + s.substring(4, 6);
        if (s.matches("^\\d{4}-\\d{2}$")) return s;
        throw new IllegalArgumentException("yearMonth must be yyyy-MM or yyyyMM, but got: " + s);
    }
}
