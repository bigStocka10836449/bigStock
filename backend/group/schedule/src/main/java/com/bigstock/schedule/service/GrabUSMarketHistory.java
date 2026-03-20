package com.bigstock.schedule.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.bigstock.sharedComponent.redis.CacheOperatorService;
import com.bigstock.sharedComponent.service.GrabThirdPartyStockDayPrice;
import com.bigstock.sharedComponent.service.StockDayPriceService;

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
	
	@Scheduled(cron = "0 30 12 * * ?", zone = "Asia/Taipei")
	public void grabUSHistory() {
		Date currentTradeDate = stockDayPriceService.getCurrentTradeDate();
		LocalDate tradeDateLdt = LocalDate.ofInstant(currentTradeDate.toInstant(), ZoneId.of("Asia/Taipei"));
		LocalDate tradeDateLdtBefore400 = tradeDateLdt.minusDays(400);
		List<String> indicators =  List.of("^SOX","MES=F","MYM=F","MNQ=F");
		indicators.stream().forEach(indicator -> {
			String json = grabThirdPartyStockDayPrice.grabAndCacheMarketHistoryFromYahoo(indicator,
					tradeDateLdtBefore400.atStartOfDay(ZoneOffset.UTC).toInstant().getEpochSecond(),
					tradeDateLdt.atStartOfDay(ZoneOffset.UTC).toInstant().getEpochSecond());
			cacheOperatorService.putCompressedValue("market:raw:history", indicator, json);
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		});
	}
}
