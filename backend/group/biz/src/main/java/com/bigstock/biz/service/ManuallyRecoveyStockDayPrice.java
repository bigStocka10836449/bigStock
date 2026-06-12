package com.bigstock.biz.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.compress.utils.Lists;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Service;

import com.bigstock.sharedComponent.entity.StockDayPrice;
import com.bigstock.sharedComponent.entity.StockInfo;
import com.bigstock.sharedComponent.redis.CacheOperatorService;
import com.bigstock.sharedComponent.service.GrabThirdPartyStockDayPrice;
import com.bigstock.sharedComponent.service.MarginTradingAndShortSellingInfoService;
import com.bigstock.sharedComponent.service.RankStockChangeService;
import com.bigstock.sharedComponent.service.StockDayPriceService;
import com.bigstock.sharedComponent.service.StockInfoService;
import com.bigstock.sharedComponent.service.StockMonthPriceService;
import com.bigstock.sharedComponent.service.StockWeekPriceService;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
// LOCAL_RANK_TEST_DISABLED: @EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class ManuallyRecoveyStockDayPrice {
	
	private final StockInfoService stockInfoService;
	private final GrabThirdPartyStockDayPrice grabThirdPartyStockDayPrice;
	private final StockDayPriceService stockDayPriceService;

	private final StockWeekPriceService stockWeekPriceService;

	private final StockMonthPriceService stockMonthPriceService;

	private final RankStockChangeService rankStockChangeService;

	private final CacheOperatorService cacheOperatorService;

	private final MarginTradingAndShortSellingInfoService marginTradingAndShortSellingInfoService;
	
//	@PostConstruct
	public void excecute() {
		List<String> tpexStockCodes = stockInfoService.getStockCodeByStockType("0").stream().filter(data -> {
			return !data.matches(".*[a-zA-Z].*");
		}).toList();
		List<String> twseStockCodes = stockInfoService.getStockCodeByStockType("1").stream().filter(data -> {
			return !data.matches(".*[a-zA-Z].*");
		}).toList();
		List<String> allStockCodes = Lists.newArrayList();
		List<StockInfo> allStockInfos = stockInfoService.getAllStockInfo().stream().filter(data -> {
			return !data.getStockCode().matches(".*[a-zA-Z].*");
		}).toList();
		Map<String, List<StockInfo>> allStockInfoMap = allStockInfos.stream()
				.collect(Collectors.groupingBy(StockInfo::getStockCode));
		allStockCodes.addAll(tpexStockCodes);
		allStockCodes.addAll(twseStockCodes);

		Date tradeDate = stockDayPriceService.getCurrentTradeDate();
		LocalDate tradeDateLdt = LocalDate.ofInstant(tradeDate.toInstant(), ZoneId.of("Asia/Taipei"));
		Integer years = tradeDateLdt.getYear();
		LocalDate tradeDateMinus365 = tradeDateLdt.minusDays(500);
		Instant instant = tradeDateMinus365.atStartOfDay(ZoneId.of("Asia/Taipei")).toInstant();
		Date tradeDateBefore365Days = Date.from(instant);
		List<StockDayPrice> stockDayPricesFor365Ds = stockDayPriceService
				.findByStockCodeAndTradingDayBeforEqualLimitTwoFourty(tradeDateBefore365Days, tradeDate);
		Map<String, List<StockDayPrice>> groupedStockDayPrices = stockDayPricesFor365Ds.stream().filter(data -> {
			java.util.Date tradingDay = data.getTradingDay();
			LocalDate dataTradeDateLdt = ((java.sql.Date) tradingDay).toLocalDate();
			return dataTradeDateLdt.compareTo(tradeDateLdt) <= 0;
		}

		).collect(Collectors.groupingBy(StockDayPrice::getStockCode));

		groupedStockDayPrices.entrySet().stream().filter(entry -> CollectionUtils.isNotEmpty(entry.getValue()))
				.forEach(entry -> {
					String stockCode = entry.getKey();
					List<StockDayPrice> stockDayPrices = entry.getValue();
					if (allStockInfoMap.containsKey(stockCode)) {
						List<StockDayPrice> cacheStockDayPrices = cacheOperatorService.getCompressedZSetAllScore(
								"ultraLongLivedCache", "stock:compressed:" + stockCode, StockDayPrice.class);
						if (CollectionUtils.isNotEmpty(cacheStockDayPrices)) {

							cacheOperatorService.upsertCompressedZSetSeries("ultraLongLivedCache",
									"stock:compressed:" + stockCode, stockDayPrices.stream().findFirst().get(),
									stockDayPrices.stream().findFirst().get().getTradingDay().getTime(),
									CacheOperatorService.DEFAULT_SERIES_MAX_SIZE);
						} else {
							cacheOperatorService.batchUpsertCompressedZSetSeries("ultraLongLivedCache",
									"stock:compressed:" + stockCode, stockDayPrices,
									stockDayPrice -> stockDayPrice.getTradingDay().getTime(),
									CacheOperatorService.DEFAULT_SERIES_MAX_SIZE);
						}
					} else {
						log.warn("stock_info missing : {}", stockCode);
					}
				});
	}
	
}
