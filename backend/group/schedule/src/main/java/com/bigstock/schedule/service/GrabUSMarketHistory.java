package com.bigstock.schedule.service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.bigstock.sharedComponent.redis.CacheOperatorService;
import com.bigstock.sharedComponent.service.GrabThirdPartyStockDayPrice;
import com.bigstock.sharedComponent.service.StockDayPriceService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class GrabUSMarketHistory {
	
	private final GrabThirdPartyStockDayPrice grabThirdPartyStockDayPrice;
	
	private final StockDayPriceService stockDayPriceService;
	
	private final CacheOperatorService cacheOperatorService;
	
//	@PostConstruct
	@Scheduled(cron = "0 45 14 * * ?", zone = "Asia/Taipei")
	public void grabUSHistory() {
		ObjectMapper objectMapper = new ObjectMapper();
	    Instant now = Instant.now();

	    // Yahoo period2 為 exclusive，使用目前時間作為結束邊界
	    long period2 = now.plusSeconds(60).getEpochSecond();

	    // 往前抓取 400 天
	    long period1 = now.minus(400, ChronoUnit.DAYS)
	            .getEpochSecond();

	    List<String> indicators = List.of(
	            "^SOX",
	            "MES=F",
	            "MYM=F",
	            "MNQ=F"
	    );

	    for (String indicator : indicators) {

	        try {
	            String json = grabThirdPartyStockDayPrice
	                    .grabAndCacheMarketHistoryFromYahoo(
	                            indicator,
	                            period1,
	                            period2
	                    );

	            if (json == null || json.isBlank()) {
	                log.warn("[US HISTORY] Empty response: {}", indicator);
	                continue;
	            }

	            JsonNode root = objectMapper.readTree(json);

	            JsonNode result = root.path("chart").path("result");

	            if (!result.isArray() || result.isEmpty()) {
	                log.warn("[US HISTORY] Invalid chart: {}", indicator);
	                continue;
	            }

	            JsonNode data = result.get(0);
	            JsonNode timestamps = data.path("timestamp");
	            JsonNode closes = data.path("indicators")
	                    .path("quote")
	                    .path(0)
	                    .path("close");

	            if (!timestamps.isArray() || timestamps.isEmpty()
	                    || !closes.isArray() || closes.isEmpty()) {

	                log.warn("[US HISTORY] No history prices: {}", indicator);
	                continue;
	            }

	            long lastTimestamp = timestamps.get(timestamps.size() - 1).asLong();

	            log.info(
	                    "[US HISTORY] symbol={}, bars={}, lastBarNY={}",
	                    indicator,
	                    timestamps.size(),
	                    Instant.ofEpochSecond(lastTimestamp)
	                            .atZone(ZoneId.of("America/New_York"))
	            );

	            cacheOperatorService.putCompressedValue(
	                    "market:raw:history",
	                    indicator,
	                    json
	            );

	        } catch (Exception e) {
	            log.error("[US HISTORY] Failed: {}", indicator, e);
	        }

	        try {
	            Thread.sleep(2_000);
	        } catch (InterruptedException e) {
	            Thread.currentThread().interrupt();
	            break;
	        }
	    }
	}
	
	@Scheduled(cron = "0 00 8 * * ?", zone = "Asia/Taipei")
	public void grabTWPMNHistory() {
		String json = grabThirdPartyStockDayPrice.grabAndCacheTWPMNMarketHistoryFromYahoo();
		cacheOperatorService.putCompressedValue("market:raw:history", "WTX&", json);
	}
	
	@Scheduled(cron = "0 20 17 * * ?", zone = "Asia/Taipei")
	public void grabTWPMHistory() {
		String json = grabThirdPartyStockDayPrice.grabAndCacheTWPMMarketHistoryFromYahoo();
		cacheOperatorService.putCompressedValue("market:raw:history", "WTX00", json);
	}
}
