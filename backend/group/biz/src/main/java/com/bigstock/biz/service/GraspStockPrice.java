package com.bigstock.biz.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import com.bigstock.biz.utils.ChromeDriverUtils;
import com.bigstock.sharedComponent.entity.MarginTradingAndShortSellingInfo;
import com.bigstock.sharedComponent.entity.StockDayPrice;
import com.bigstock.sharedComponent.entity.StockDayPriceRank;
import com.bigstock.sharedComponent.entity.StockMonthPrice;
import com.bigstock.sharedComponent.entity.StockMonthPriceRank;
import com.bigstock.sharedComponent.entity.StockWeekPrice;
import com.bigstock.sharedComponent.entity.StockWeekPriceRank;
import com.bigstock.sharedComponent.entity.TmpExDividendsExRightInfo;
import com.bigstock.sharedComponent.entity.TradeVolumeInfo;
import com.bigstock.sharedComponent.service.MarginTradingAndShortSellingInfoService;
import com.bigstock.sharedComponent.service.SecuritiesFirmsDayOperateService;
import com.bigstock.sharedComponent.service.StockDayPriceService;
import com.bigstock.sharedComponent.service.StockExchangeDetailService;
import com.bigstock.sharedComponent.service.StockInfoService;
import com.bigstock.sharedComponent.service.StockMonthPriceService;
import com.bigstock.sharedComponent.service.StockWeekPriceService;
import com.bigstock.sharedComponent.service.TmpExDividendsExRightInfoService;
import com.bigstock.sharedComponent.service.TradeVolumeInfoService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.common.collect.Lists;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class GraspStockPrice {
//	@Value("${schedule.chromeDriverPath.windows.active}")
//	private boolean windowsActive;

//	@Value("${schedule.chromeDriverPath.windows.path}")
//	private String windowsChromeDriverPath;

//	@Value("${schedule.chromeDriverPath.linux.active}")
//	private boolean linuxActive;

//	@Value("${schedule.chromeDriverPath.linux.driver-path}")
//	private String linuxChromeDriverPath;

//	@Value("${schedule.chromeDriverPath.download-path}")
//	private String downloadPath;

//	@Value("${schedule.bpython-url}")
//	private String bpythonUrl;

//	@Value("${schedule.credentials-pathl}")
//	private String credentialsPath;

//	private final GraspHistoryStockPrice graspHistoryStockPrice;

	private final SecuritiesFirmsDayOperateService securitiesFirmsDayOperateService;

	private final StockInfoService stockInfoService;

	private final StockExchangeDetailService stockExchangeDetailService;

	private final StockDayPriceService stockDayPriceService;

	private final MarginTradingAndShortSellingInfoService marginTradingAndShortSellingInfoServices;

	private final TradeVolumeInfoService tradeVolumeInfoService;

	private final TmpExDividendsExRightInfoService tmpExDividendsExRightInfoService;

	private final StockWeekPriceService stockWeekPriceService;

	private final StockMonthPriceService stockMonthPriceService;

	// 爬蟲暫時不做，取消抓取蠟燭圖方法，帶未來真的規模擴大，再走實際正常串接作法
//	@PostConstruct
//	public void grepCandlestickChart() throws InterruptedException, JsonMappingException, JsonProcessingException, RestClientException, URISyntaxException {
//		Date tradeDate = graspHistoryStockPrice.getLastTradeDate();
//		List<String> stockCodes = ChromeDriverUtils
//				.getStockInfoByTdccApi("https://openapi.tdcc.com.tw/v1/opendata/1-2").stream().filter(stockInfo -> StringUtils.isNotBlank(stockInfo.getStockType()))
//				.filter(stockInfo -> List.of("1", "0").contains(stockInfo.getStockType()))
//				.map(stockInfo -> stockInfo.getStockCode()).toList();
//		ChromeDriverUtils.grepCanvas(windowsActive ? windowsChromeDriverPath : linuxChromeDriverPath, stockCodes, tradeDate, stockExchangeDetailService);
//	}
//	@Scheduled(cron = "${schedule.task.scheduling.cron.expression.grasp-stock-price}")
//	@PostConstruct
	public void updateTmpExDivideExRightInfo() throws JsonMappingException, RestClientException,
			JsonProcessingException, URISyntaxException, InterruptedException {
		List<StockDayPrice> stockTpexDayPrices = ChromeDriverUtils
				.graspTpexDayPrice("https://www.tpex.org.tw/openapi/v1/tpex_mainboard_quotes");
		Date tradeDate = stockTpexDayPrices.stream().findFirst().get().getTradingDay();
		List<TmpExDividendsExRightInfo> tmpExDividendsExRightInfos = ChromeDriverUtils
				.grepTmpExDividendsExRightInfo(tradeDate);
		tmpExDividendsExRightInfoService.saveAll(tmpExDividendsExRightInfos);
	}

	// 每天下午5點更新
//	@Scheduled(cron = "${schedule.task.scheduling.cron.expression.grasp-stock-price}")
	// 每周日早上8点触发更新
	@Scheduled(cron = "${schedule.task.scheduling.cron.expression.grasp-stock-price}", zone= "Asia/Taipei")
	@Transactional
//	@PostConstruct
	public void updateStockDayPrice() throws Exception {
		// 先抓DB裡面全部的代號資料

		List<StockDayPrice> stockTpexEmergingStockPrices = ChromeDriverUtils.graspTpexEmergingStockDayPrice();

		List<StockDayPrice> stockTpexDayPrices = ChromeDriverUtils
				.graspTpexDayPrice("https://www.tpex.org.tw/openapi/v1/tpex_mainboard_quotes");

////		   List<StockDayPrice> singleStockDayPrices = entry.getValue();

		List<TradeVolumeInfo> stockTpexTradeVolumeInfos = ChromeDriverUtils
				.graspTpexTtradeVolume("https://www.tpex.org.tw/openapi/v1/tpex_volume_rank");
		Map<String, TradeVolumeInfo> stockTpexTradeVolumeInfosMap = stockTpexTradeVolumeInfos.stream()
				.collect(Collectors.toMap(TradeVolumeInfo::getStockCode, tradeVolumeInfo -> tradeVolumeInfo));
		Date tradeDate = stockTpexDayPrices.stream().findFirst().get().getTradingDay();
		List<StockDayPrice> stockTwseDayPrices = ChromeDriverUtils
				.graspTwseDayPrice("https://openapi.twse.com.tw/v1/exchangeReport/STOCK_DAY_ALL", tradeDate);
		List<TradeVolumeInfo> twseTradeVolumeInfos = stockTwseDayPrices.stream().map(stockDayPrice -> {
			TradeVolumeInfo tradeVolumeInfo = new TradeVolumeInfo();
			tradeVolumeInfo.setTradingDay(stockDayPrice.getTradingDay());
			tradeVolumeInfo.setStockCode(stockDayPrice.getStockCode());
			tradeVolumeInfo.setTradeVolume(stockDayPrice.getTradingVolume());
			return tradeVolumeInfo;
		}).toList();
		List<TmpExDividendsExRightInfo> tmpExDividendsExRightInfos = ChromeDriverUtils
				.grepTmpExDividendsExRightInfo(tradeDate);
		Map<String, List<TmpExDividendsExRightInfo>> groupedtmpExDividendsExRightInfos =
				tmpExDividendsExRightInfos.stream()
			        .collect(Collectors.groupingBy(TmpExDividendsExRightInfo::getStockCode));
		boolean isExDivideOrRightHappen = (stockTpexDayPrices.stream()
				.anyMatch(stockTpexDayPrice -> (stockTpexDayPrice.getChange().contains("除息")
						|| stockTpexDayPrice.getChange().contains("除權")))
				|| stockTwseDayPrices.stream()
						.anyMatch(stockTwseDayPrice -> stockTwseDayPrice.getChange().contains("X")));
		if (isExDivideOrRightHappen) {
			List<TmpExDividendsExRightInfo> todateTmpExDividendsExRightInfo = tmpExDividendsExRightInfoService
					.findByTradingDay(tradeDate);
			if (todateTmpExDividendsExRightInfo.isEmpty()) {
				tmpExDividendsExRightInfoService.saveAll(tmpExDividendsExRightInfos);
			}
		}

//		List<StockDayPrice> ss =stockDayPriceService.findByStartDateAndEndDate(new Date("2024/05/01"), new Date("2024/12/20"))
//		.stream().map(stockDayPrice ->{
//			stockDayPrice.setMonthOfYear((stockDayPrice.getTradingDay().getYear() + 1900) + "W"
//					+ (stockDayPrice.getTradingDay().getMonth() + 1));
//			return stockDayPrice;
//		}).toList();
//		stockDayPriceService.saveAll(ss);

		stockDayPriceService.upsertBatch(stockTpexEmergingStockPrices);
		stockDayPriceService.upsertBatch(stockTpexDayPrices);
		LocalDate tradeDateLdt = LocalDate.ofInstant(tradeDate.toInstant(), ZoneId.of("Asia/Taipei"));
		Integer years = tradeDateLdt.getYear();
		LocalDate tradeDateMinus365 = tradeDateLdt.minusDays(500); 
		Instant instant = tradeDateMinus365.atStartOfDay( ZoneId.of("Asia/Taipei")).toInstant();
		Date tradeDateBefore365Days = Date.from(instant);
		List<StockDayPrice> stockDayPricesFor365Ds = stockDayPriceService.findByStockCodeAndTradingDayBeforEqualLimitTwoFourty(tradeDateBefore365Days, tradeDate);
		Map<String, List<StockDayPrice>> groupedStockDayPrices =
				stockDayPricesFor365Ds.stream()
			        .collect(Collectors.groupingBy(StockDayPrice::getStockCode));
		stockTpexDayPrices.stream().forEach(stockTpexDayPrice -> {
			if(!groupedStockDayPrices.containsKey(stockTpexDayPrice.getStockCode())) {
				return;
			}
			if("5340".equals(stockTpexDayPrice.getStockCode())) {
				log.info("5340");
			}
			List<StockDayPrice> stockDayPrices = groupedStockDayPrices.get(stockTpexDayPrice.getStockCode());
			List<StockDayPrice> allThisStockCodeDayPrices = Lists.newArrayList();
			allThisStockCodeDayPrices.add(stockTpexDayPrice);
			allThisStockCodeDayPrices.addAll(stockDayPrices);
			calculateRSVValueAndLimitDownUp(stockTpexDayPrice,allThisStockCodeDayPrices,groupedtmpExDividendsExRightInfos);
			stockTpexDayPrice.setMonthOfYear((stockTpexDayPrice.getTradingDay().getYear() + 1900) + "W"
					+ (stockTpexDayPrice.getTradingDay().getMonth() + 1));
			if (stockTpexTradeVolumeInfosMap.containsKey(stockTpexDayPrice.getStockCode())) {
				TradeVolumeInfo tradeVolumeInfo = stockTpexTradeVolumeInfosMap.get(stockTpexDayPrice.getStockCode());
				stockTpexDayPrice.setTradingVolume(tradeVolumeInfo.getTradeVolume());
			}
			groupedStockDayPrices.put(stockTpexDayPrice.getStockCode(), allThisStockCodeDayPrices);
		});
		stockTpexEmergingStockPrices.stream().forEach(stockTpexDayPrice -> {
			if(!groupedStockDayPrices.containsKey(stockTpexDayPrice.getStockCode())) {
				return;
			}
			List<StockDayPrice> stockDayPrices = groupedStockDayPrices.get(stockTpexDayPrice.getStockCode());
			List<StockDayPrice> allThisStockCodeDayPrices = Lists.newArrayList();
			allThisStockCodeDayPrices.add(stockTpexDayPrice);
			allThisStockCodeDayPrices.addAll(stockDayPrices);
			calculateRSVValueAndLimitDownUp(stockTpexDayPrice,allThisStockCodeDayPrices,groupedtmpExDividendsExRightInfos);
			stockTpexDayPrice.setMonthOfYear((stockTpexDayPrice.getTradingDay().getYear() + 1900) + "W"
					+ (stockTpexDayPrice.getTradingDay().getMonth() + 1));
			if("5340".equals(stockTpexDayPrice.getStockCode())) {
				log.info("5340");
			}
			if (stockTpexTradeVolumeInfosMap.containsKey(stockTpexDayPrice.getStockCode())) {
				TradeVolumeInfo tradeVolumeInfo = stockTpexTradeVolumeInfosMap.get(stockTpexDayPrice.getStockCode());
				stockTpexDayPrice.setTradingVolume(tradeVolumeInfo.getTradeVolume());
			}
			groupedStockDayPrices.put(stockTpexDayPrice.getStockCode(), allThisStockCodeDayPrices);
		});
		List<StockDayPriceRank> allStockDayPriceRanks = Lists.newArrayList();
		List<StockWeekPriceRank> allStockWeekPriceRanks = Lists.newArrayList();
		stockDayPriceService.upsertBatch(stockTpexEmergingStockPrices);
		stockDayPriceService.upsertBatch(stockTpexDayPrices);
		stockDayPriceService.upsertBatch(stockTwseDayPrices);
		stockTpexDayPrices.stream().forEach(stockTwseDayPrice -> {
			if(groupedStockDayPrices.containsKey(stockTwseDayPrice.getStockCode())) {
				List<StockDayPrice> singleStockWeekPrices = groupedStockDayPrices.get(stockTwseDayPrice.getStockCode());
				StockDayPriceRank lastStockMonthPriceRank = stockDayPriceService.buildRanks(stockTwseDayPrice.getStockCode(), singleStockWeekPrices);
				allStockDayPriceRanks.add(lastStockMonthPriceRank);
			}
		});
		stockTwseDayPrices.stream().forEach(stockTwseDayPrice -> {
			if("5340".equals(stockTwseDayPrice.getStockCode())) {
				log.info("5340");
			}
			if(!groupedStockDayPrices.containsKey(stockTwseDayPrice.getStockCode())) {
				return;
			}
			List<StockDayPrice> stockDayPrices = groupedStockDayPrices.get(stockTwseDayPrice.getStockCode());
			List<StockDayPrice> allThisStockCodeDayPrices = Lists.newArrayList();
			allThisStockCodeDayPrices.add(stockTwseDayPrice);
			allThisStockCodeDayPrices.addAll(stockDayPrices);
			calculateRSVValueAndLimitDownUp(stockTwseDayPrice,allThisStockCodeDayPrices,groupedtmpExDividendsExRightInfos);
			stockTwseDayPrice.setMonthOfYear((stockTwseDayPrice.getTradingDay().getYear() + 1900) + "W"
					+ (stockTwseDayPrice.getTradingDay().getMonth() + 1));
			if (stockTpexTradeVolumeInfosMap.containsKey(stockTwseDayPrice.getStockCode())) {
				TradeVolumeInfo tradeVolumeInfo = stockTpexTradeVolumeInfosMap.get(stockTwseDayPrice.getStockCode());
				stockTwseDayPrice.setTradingVolume(tradeVolumeInfo.getTradeVolume());
			}
			groupedStockDayPrices.put(stockTwseDayPrice.getStockCode(), allThisStockCodeDayPrices);
		});
		stockDayPriceService.upsertBatch(stockTwseDayPrices);
		stockTwseDayPrices.stream().forEach(stockTwseDayPrice -> {
			if(groupedStockDayPrices.containsKey(stockTwseDayPrice.getStockCode())) {
				if ("2330".equals(stockTwseDayPrice.getStockCode())) {
					log.info("2330");
				}
				List<StockDayPrice> singleStockWeekPrices = groupedStockDayPrices.get(stockTwseDayPrice.getStockCode());
				StockDayPriceRank lastStockMonthPriceRank = stockDayPriceService.buildRanks(stockTwseDayPrice.getStockCode(), singleStockWeekPrices);
				allStockDayPriceRanks.add(lastStockMonthPriceRank);
			}
		});
		stockDayPriceService.updateRankNo();
		stockDayPriceService.bulkUpsertDayRankPrices(allStockDayPriceRanks, tradeDate);
		stockDayPriceService.deleteRankNoByStockCode();
		tradeVolumeInfoService.saveAll(stockTpexTradeVolumeInfos);
		tradeVolumeInfoService.saveAll(twseTradeVolumeInfos);
//		.findByStartDateAndEndDate(new Date("2024/05/01"), new Date("2024/12/20")).stream()
		
		
		String targetWeek = stockTpexDayPrices.get(0).getWeekOfYear();
		
		List<StockWeekPrice> stockWeekPriceForYears = stockWeekPriceService.findByYearBeforEqualLimitTwoFourty(String.valueOf(years-6) , String.valueOf(years));
		Map<String, List<StockWeekPrice>> groupedStockWeekPrice =
				stockWeekPriceForYears.stream()
			        .collect(Collectors.groupingBy(StockWeekPrice::getStockCode));
		
		List<StockDayPrice> weekDayPrices =
		    stockDayPriceService.findByWeekOfYear(targetWeek);

		Map<String, List<StockDayPrice>> stockWeekGroupMap =
		    weekDayPrices.stream()
		        .collect(Collectors.groupingBy(StockDayPrice::getStockCode));

		List<StockWeekPrice> allStockWeekPrices = new ArrayList<>();

		stockWeekGroupMap.forEach((stockCode, stockDayList) -> {
			
		    // 過濾無效資料
		    List<StockDayPrice> validDayPrices =
		        stockDayList.stream()
		            .filter(p ->
		                StringUtils.isNotBlank(p.getOpeningPrice())
		                && !p.getOpeningPrice().contains("--"))
		            .toList();

		    if (validDayPrices.isEmpty()) {
		        return;
		    }

		    // 依交易日排序（只做一次）
		    List<StockDayPrice> sortedDays =
		        validDayPrices.stream()
		            .sorted(Comparator.comparing(StockDayPrice::getTradingDay))
		            .toList();

		    StockDayPrice firstDay = sortedDays.get(0);
		    StockDayPrice lastDay  = sortedDays.get(sortedDays.size() - 1);

		    // 計算 high / low / volume
		    BigDecimal high = null;
		    BigDecimal low  = null;
		    int totalTradingVolume = 0;

		    for (StockDayPrice d : sortedDays) {
		        BigDecimal h = new BigDecimal(d.getHighPrice().replace(",", ""));
		        BigDecimal l = new BigDecimal(d.getLowPrice().replace(",", ""));

		        high = (high == null || h.compareTo(high) > 0) ? h : high;
		        low  = (low  == null || l.compareTo(low)  < 0) ? l : low;

		        if (d.getTradingVolume() != null) {
		            totalTradingVolume += Integer.parseInt(d.getTradingVolume());
		        }
		    }

		    // 組裝唯一一筆 StockWeekPrice
		    StockWeekPrice stockWeekPrice = new StockWeekPrice();
		    stockWeekPrice.setStockCode(stockCode);
		    stockWeekPrice.setWeekOfYear(firstDay.getWeekOfYear());
		    stockWeekPrice.setOpeningPrice(new BigDecimal(firstDay.getOpeningPrice().replace(",", "")));
		    stockWeekPrice.setClosingPrice(new BigDecimal(lastDay.getClosingPrice().replace(",", "")));
		    stockWeekPrice.setHighPrice(high);
		    stockWeekPrice.setLowPrice(low);
		    stockWeekPrice.setTradingVolume(totalTradingVolume);
		    stockWeekPrice.setFirstTradingDay(firstDay.getTradingDay());
		    stockWeekPrice.setMonth(firstDay.getTradingDay().getMonth() + 1);
		    stockWeekPrice.setYear(String.valueOf(firstDay.getTradingDay().getYear() + 1900));
		    List<StockWeekPrice> stockWeekPrices = Lists.newArrayList();
		    stockWeekPrices.add(stockWeekPrice);
		    stockWeekPrices.addAll(groupedStockWeekPrice.get(stockCode));
		    calculateRSVValueAndLimitDownUp(stockWeekPrice, stockWeekPrices);
		    allStockWeekPrices.add(stockWeekPrice);
		    groupedStockWeekPrice.put(stockCode, stockWeekPrices);
		});
		stockDayPriceService.updateRankNo();
		stockDayPriceService.bulkUpsertDayRankPrices(allStockDayPriceRanks, tradeDate);
		stockWeekPriceService.batchInsertWeekPrices(allStockWeekPrices);
		stockDayPriceService.deleteRankNoByStockCode();
		
		
//		List<StockWeekPrice> stockWeekPrices = stockWeekPriceService
//				.findByWeekOfYear(stockTpexDayPrices.stream().findFirst().get().getWeekOfYear());
////				.findBySockCode("2330").stream()	.sorted((x1, x2) -> x1.getWeekOfYear().compareTo(x2.getWeekOfYear())).toList();
////				.findAll().stream()	.sorted((x1, x2) -> x1.getWeekOfYear().compareTo(x2.getWeekOfYear())).toList();
//		stockWeekPrices.stream().forEach(stockWeekPrice -> {
//			calculateRSVValueAndLimitDownUp(stockWeekPrice);
//			stockWeekPriceService.save(stockWeekPrice);
//		});
//		
//		
//		stockDayPriceService
//				.findByWeekOfYear(stockTpexDayPrices.stream().findFirst().get().getWeekOfYear()).stream()
//				.collect(Collectors.groupingBy(StockDayPrice::getStockCode)).entrySet().stream()
//				.forEach(innerStockDayPrices -> {
//					List<StockWeekPrice> stockStockWeekPrices = innerStockDayPrices.getValue().stream()
//							.filter(innerStockDayPrice -> (!innerStockDayPrice.getOpeningPrice().contains("--")
//									&& StringUtils.isNotBlank(innerStockDayPrice.getOpeningPrice())))
//							.collect(Collectors.groupingBy(StockDayPrice::getWeekOfYear)).entrySet().stream()
//							.map(innerStockDayWeekInfos -> {
//								List<StockDayPrice> innerStockDayWeekInfo = innerStockDayWeekInfos.getValue().stream()
//										.sorted((x1, x2) -> x1.getTradingDay().compareTo(x2.getTradingDay())).toList();
//								String firstOpenPrice = innerStockDayWeekInfo.stream().findFirst().get()
//										.getOpeningPrice();
//								String closePrice = innerStockDayWeekInfo.get(innerStockDayWeekInfo.size() - 1)
//										.getClosingPrice();
//								String hightestPrice = innerStockDayWeekInfo.stream()
//										.sorted((x1, x2) -> x2.getHighPrice().compareTo(x1.getHighPrice())).findFirst()
//										.get().getHighPrice();
//								String lowestPrice = innerStockDayWeekInfo.stream()
//										.sorted((x1, x2) -> x1.getLowPrice().compareTo(x2.getLowPrice())).findFirst()
//										.get().getLowPrice();
//								Integer totalTradingVolume = innerStockDayWeekInfo.stream().filter(
//										innerStockDay -> ObjectUtils.isNotEmpty(innerStockDay.getTradingVolume()))
//										.mapToInt(innerStockDay -> Integer.valueOf(innerStockDay.getTradingVolume()))
//										.sum();
//								StockWeekPrice stockWeekPrice = new StockWeekPrice();
//								stockWeekPrice.setOpeningPrice(new BigDecimal(firstOpenPrice.replace(",", "")));
//								stockWeekPrice.setClosingPrice(new BigDecimal(closePrice.replace(",", "")));
//								stockWeekPrice.setHighPrice(new BigDecimal(hightestPrice.replace(",", "")));
//								stockWeekPrice.setLowPrice(new BigDecimal(lowestPrice.replace(",", "")));
//								stockWeekPrice.setTradingVolume(totalTradingVolume);
//								stockWeekPrice
//										.setStockCode(innerStockDayWeekInfo.stream().findFirst().get().getStockCode());
//								stockWeekPrice.setWeekOfYear(
//										innerStockDayWeekInfo.stream().findFirst().get().getWeekOfYear());
//								stockWeekPrice.setMonth(
//										innerStockDayWeekInfo.stream().findFirst().get().getTradingDay().getMonth()
//												+ 1);
//								stockWeekPrice.setFirstTradingDay(
//										innerStockDayWeekInfo.stream().findFirst().get().getTradingDay());
//								stockWeekPrice.setYear(String.valueOf(
//										innerStockDayWeekInfo.stream().findFirst().get().getTradingDay().getYear()
//												+ 1900));
//								calculateRSVValueAndLimitDownUp(stockWeekPrice);
//								return stockWeekPrice;
//							}).toList();
//					stockWeekPriceService.saveAll(stockStockWeekPrices);
//				});
		allStockWeekPrices.stream().forEach(allStockWeekPrice ->{
			if(groupedStockWeekPrice.containsKey(allStockWeekPrice.getStockCode())) {
				List<StockWeekPrice> oriStockMonthPriceRanks = Lists.newArrayList();
				oriStockMonthPriceRanks.add(allStockWeekPrice);
				List<StockWeekPrice> singleStockWeekPrices = groupedStockWeekPrice.get(allStockWeekPrice.getStockCode());
				oriStockMonthPriceRanks.addAll(singleStockWeekPrices);
				StockWeekPriceRank stockMonthPriceRanks = stockWeekPriceService.buildRanks(allStockWeekPrice.getStockCode(), oriStockMonthPriceRanks);
				allStockWeekPriceRanks.add(stockMonthPriceRanks);
			
			}
		});
//		
		stockWeekPriceService.updateRankNo();
		stockWeekPriceService.batchInsertWeekRankPrices(allStockWeekPriceRanks);
		stockWeekPriceService.deleteByRankNoLessThanZero();
//		
//		List<StockWeekPrice> stockWeekPrices = stockWeekPriceService
//				.findByWeekOfYear(stockTpexDayPrices.stream().findFirst().get().getWeekOfYear());
//				.findBySockCode("2330").stream()	.sorted((x1, x2) -> x1.getWeekOfYear().compareTo(x2.getWeekOfYear())).toList();
//				.findAll().stream()	.sorted((x1, x2) -> x1.getWeekOfYear().compareTo(x2.getWeekOfYear())).toList();
//		stockWeekPrices.stream().forEach(stockWeekPrice -> {
//			
//			stockWeekPriceService.save(stockWeekPrice);
//		});
		
		
		
		
		

		// 取得欲處理的月份
		String targetMonth = stockTpexDayPrices.get(0).getMonthOfYear();
		List<StockMonthPrice> twoFourtyStockMonthPrices = stockMonthPriceService
				.findByMmonthOfYearAndDesc(targetMonth);
		Map<String, List<StockMonthPrice>> groupedStockMonthPrice =
				twoFourtyStockMonthPrices.stream()
			        .collect(Collectors.groupingBy(StockMonthPrice::getStockCode));
		// 取得該月份所有日成交資料
		List<StockDayPrice> monthDayPrices =
		    stockDayPriceService.findByMonthOfYear(targetMonth);

		// 依 stockCode 分組
		Map<String, List<StockDayPrice>> stockMonthGroupMap =
		    monthDayPrices.stream()
		        .collect(Collectors.groupingBy(StockDayPrice::getStockCode));

		// 用來 batch insert 的集合
		List<StockMonthPrice> allStockMonthPrices = new ArrayList<>();

		stockMonthGroupMap.forEach((stockCode, stockDayList) -> {
			if(groupedStockMonthPrice.containsKey(stockCode)) {
				 // 過濾無效的開盤價資料
			    List<StockDayPrice> validDayPrices =
			        stockDayList.stream()
			            .filter(p ->
			                StringUtils.isNotBlank(p.getOpeningPrice())
			                && !p.getOpeningPrice().contains("--"))
			            .toList();

			    if (validDayPrices.isEmpty()) {
			        return; // 該股票該月無有效資料
			    }

			    // 依交易日排序（只做一次）
			    List<StockDayPrice> sortedDays =
			        validDayPrices.stream()
			            .sorted(Comparator.comparing(StockDayPrice::getTradingDay))
			            .toList();

			    StockDayPrice firstDay = sortedDays.get(0);
			    StockDayPrice lastDay  = sortedDays.get(sortedDays.size() - 1);

			    // 計算最高、最低、成交量
			    BigDecimal high = null;
			    BigDecimal low  = null;
			    int totalTradingVolume = 0;
			    
			    for (StockDayPrice d : sortedDays) {
			        BigDecimal h = new BigDecimal(d.getHighPrice().replace(",", ""));
			        BigDecimal l = new BigDecimal(d.getLowPrice().replace(",", ""));

			        high = (high == null || h.compareTo(high) > 0) ? h : high;
			        low  = (low  == null || l.compareTo(low)  < 0) ? l : low;

			        if (d.getTradingVolume() != null) {
			            totalTradingVolume += Integer.parseInt(d.getTradingVolume());
			        }
			    }

			    // 組裝唯一一筆 StockMonthPrice
			    StockMonthPrice stockMonthPrice = new StockMonthPrice();
			    stockMonthPrice.setStockCode(stockCode);
			    stockMonthPrice.setOpeningPrice(new BigDecimal(firstDay.getOpeningPrice().replace(",", "")));
			    stockMonthPrice.setClosingPrice(new BigDecimal(lastDay.getClosingPrice().replace(",", "")));
			    stockMonthPrice.setHighPrice(high);
			    stockMonthPrice.setLowPrice(low);
			    stockMonthPrice.setTradingVolume(totalTradingVolume);
			    stockMonthPrice.setFirstTradingDay(firstDay.getTradingDay());

			    int year  = firstDay.getTradingDay().getYear() + 1900;
			    int month = firstDay.getTradingDay().getMonth() + 1;

			    stockMonthPrice.setYear(String.valueOf(year));
			    stockMonthPrice.setMonth(month);
			    stockMonthPrice.setMonthOfYear(year + "M" + month);
			    List<StockMonthPrice> stockMonthPrices = Lists.newArrayList();
			    stockMonthPrices.add(stockMonthPrice);
			    stockMonthPrices.addAll(groupedStockMonthPrice.get(stockCode));
			    calculateRSVValueAndLimitDownUp(stockMonthPrice,stockMonthPrices);
			    allStockMonthPrices.add(stockMonthPrice);
			    groupedStockMonthPrice.put(stockCode, stockMonthPrices);
			}
		   
		});

		//  真正合理的 batch insert
		stockMonthPriceService.batchInsertMonthPrices(allStockMonthPrices);
		
		
//		stockDayPriceService
//				.findByMonthOfYear(stockTpexDayPrices.stream().findFirst().get().getMonthOfYear()).stream()
//				.collect(Collectors.groupingBy(StockDayPrice::getStockCode)).entrySet().stream()
//				.forEach(innerStockDayPrices -> {
//					List<StockMonthPrice> stockMonthPrices = innerStockDayPrices.getValue().stream()
//							.filter(innerStockDayPrice -> (!innerStockDayPrice.getOpeningPrice().contains("--")
//									&& StringUtils.isNotBlank(innerStockDayPrice.getOpeningPrice())))
//							.collect(Collectors.groupingBy(StockDayPrice::getMonthOfYear)).entrySet().stream()
//							.map(innerStockDayWeekInfos -> {
//								List<StockDayPrice> innerStockDayWeekInfo = innerStockDayWeekInfos.getValue().stream()
//										.sorted((x1, x2) -> x1.getTradingDay().compareTo(x2.getTradingDay())).toList();
//								Date firstTradeDaye = innerStockDayWeekInfo.stream()
//										.sorted((x1, x2) -> x1.getTradingDay().compareTo(x2.getTradingDay())).toList()
//										.stream().findFirst().get().getTradingDay();
//								String firstOpenPrice = innerStockDayWeekInfo.stream().findFirst().get()
//										.getOpeningPrice();
//								String closePrice = innerStockDayWeekInfo.get(innerStockDayWeekInfo.size() - 1)
//										.getClosingPrice();
//								String hightestPrice = innerStockDayWeekInfo.stream()
//										.sorted((x1, x2) -> x2.getHighPrice().compareTo(x1.getHighPrice())).findFirst()
//										.get().getHighPrice();
//								String lowestPrice = innerStockDayWeekInfo.stream()
//										.sorted((x1, x2) -> x1.getLowPrice().compareTo(x2.getLowPrice())).findFirst()
//										.get().getLowPrice();
//								Integer totalTradingVolume = innerStockDayWeekInfo.stream().filter(
//										innerStockDay -> ObjectUtils.isNotEmpty(innerStockDay.getTradingVolume()))
//										.mapToInt(innerStockDay -> Integer.valueOf(innerStockDay.getTradingVolume()))
//										.sum();
//								StockMonthPrice stockMonthPrice = new StockMonthPrice();
//								stockMonthPrice.setOpeningPrice(new BigDecimal(firstOpenPrice.replace(",", "")));
//								stockMonthPrice.setClosingPrice(new BigDecimal(closePrice.replace(",", "")));
//								stockMonthPrice.setHighPrice(new BigDecimal(hightestPrice.replace(",", "")));
//								stockMonthPrice.setLowPrice(new BigDecimal(lowestPrice.replace(",", "")));
//								stockMonthPrice.setTradingVolume(totalTradingVolume);
//								stockMonthPrice.setFirstTradingDay(firstTradeDaye);
//								stockMonthPrice.setMonthOfYear((innerStockDayWeekInfo.stream().findFirst().get()
//										.getTradingDay().getYear() + 1900) + "M"
//										+ (innerStockDayWeekInfo.stream().findFirst().get().getTradingDay().getMonth()
//												+ 1));
//								stockMonthPrice
//										.setStockCode(innerStockDayWeekInfo.stream().findFirst().get().getStockCode());
//								stockMonthPrice.setMonth(
//										innerStockDayWeekInfo.stream().findFirst().get().getTradingDay().getMonth()
//												+ 1);
//								stockMonthPrice.setYear(String.valueOf(
//										innerStockDayWeekInfo.stream().findFirst().get().getTradingDay().getYear()
//												+ 1900));
//								return stockMonthPrice;
//							}).toList();
//					stockMonthPriceService.saveAll(stockMonthPrices);
//				});

//		List<StockMonthPrice> stockMonthPrices = stockMonthPriceService
//				.findByMmonthOfYear(stockWeekPrices.stream().findFirst().get().getYear() + "M"
//						+ stockWeekPrices.stream().findFirst().get().getMonth())
////				.findBySockCode("2330").stream()	.sorted((x1, x2) -> x1.getWeekOfYear().compareTo(x2.getWeekOfYear())).toList();
////				.findAll()
//				.stream().sorted((x1, x2) -> x1.getMonthOfYear().compareTo(x2.getMonthOfYear())).toList();
//		stockMonthPrices.stream().forEach(stockMonthPrice -> {
//			calculateRSVValueAndLimitDownUp(stockMonthPrice);
////			stockMonthPriceService.save(stockMonthPrice);
//		});
//		stockMonthPriceService.batchInsertMonthPrices(stockMonthPrices);
		List<StockMonthPriceRank> allStockMonthPriceRank = Lists.newArrayList();
		allStockMonthPrices.stream().forEach(stockMonthPrice ->{
			if(groupedStockMonthPrice.containsKey(stockMonthPrice.getStockCode())) {
				
				List<StockMonthPrice> singleStockStockMonthPrices = groupedStockMonthPrice.get(stockMonthPrice.getStockCode());
				List<StockMonthPrice> oriStockMonthPrices = Lists.newArrayList();
				oriStockMonthPrices.add(stockMonthPrice);
				oriStockMonthPrices.addAll(singleStockStockMonthPrices);
				StockMonthPriceRank stockMonthPriceRanks = stockMonthPriceService.buildRanks(stockMonthPrice.getStockCode(), oriStockMonthPrices);
				allStockMonthPriceRank.add(stockMonthPriceRanks);
			}
		});
		stockMonthPriceService.updateRankNo();
		stockMonthPriceService.batchInsertMonthRankPrices(allStockMonthPriceRank);
		stockMonthPriceService.deleteByRankNoLessThanZero();
//		stockWeekPriceService.saveAll(stockWeekPrices);
//		List<StockDayPrice> stockDayPrices = 
//		stockDayPriceService.findPreviousFiftyTowDaysBeforeLastestDayInfo("2330");
//		
//		List<StockDayPrice> lastStockDayPrices = stockDayPrices.subList(stockDayPrices.size() - 10 ,stockDayPrices.size()-1);
//		Date lastestTradingDate = lastStockDayPrices.get(lastStockDayPrices.size() -1).getTradingDay();
//		Date olddestTradingDate = lastStockDayPrices.get(0).getTradingDay();
//		Map<String, List<StockDayPrice>> stockDayPricesMap = stockDayPriceService
//				.findByStartDateAndEndDate(olddestTradingDate, lastestTradingDate).stream()
//				.collect(Collectors.groupingBy(StockDayPrice::getStockCode));
//		List<StockDayPrice> lastStockDayPriceWithCalculateRsvKDs = stockDayPricesMap.entrySet().stream()
//			    .map(entry -> {
//			        List<StockDayPrice> singleStockDayPrices = entry.getValue();
//			        if (singleStockDayPrices.size() < 9) {
//			            return null; // 如果不满足条件，返回 null
//			        }
//
//			        int period = 9;
//			        List<Double> closingPrices = singleStockDayPrices.stream()
//			                .map(stockDayPrice -> Double.valueOf(stockDayPrice.getClosingPrice().replaceAll(",", "")))
//			                .toList();
//			        List<Double> highPrices = singleStockDayPrices.stream()
//			                .map(stockDayPrice -> Double.valueOf(stockDayPrice.getHighPrice().replaceAll(",", "")))
//			                .toList();
//			        List<Double> lowPrices = singleStockDayPrices.stream()
//			                .map(stockDayPrice -> Double.valueOf(stockDayPrice.getLowPrice().replaceAll(",", "")))
//			                .toList();
//
//			        double highestHigh = Double.MIN_VALUE;
//			        double lowestLow = Double.MAX_VALUE;
//
//			        for (int i = 0; i < period; i++) {
//			            highestHigh = Math.max(highestHigh, highPrices.get(i));
//			            lowestLow = Math.min(lowestLow, lowPrices.get(i));
//			        }
//
//			        double latestClosingPrice = closingPrices.get(period - 1);
//			        Double rsv = (latestClosingPrice - lowestLow) / (highestHigh - lowestLow) * 100.0;
//
//			        double previousK = 50.0; // Default initial K value
//			        double previousD = 50.0; // Default initial D value
//			        double smoothingFactor = 1.0 / 3.0;
//
//			        Double k = previousK * (1 - smoothingFactor) + rsv * smoothingFactor;
//			        Double d = previousD * (1 - smoothingFactor) + k * smoothingFactor;
//
//			        StockDayPrice lastStockDayPrice = singleStockDayPrices.get(singleStockDayPrices.size() - 1);
//			        lastStockDayPrice.setLineDvalue(d.toString());
//			        lastStockDayPrice.setLineKvalue(k.toString());
//			        lastStockDayPrice.setLineRSVvalue(rsv.toString());
//
//			        return lastStockDayPrice; 
//			    })
//			    .filter(Objects::nonNull) 
//			    .collect(Collectors.toList());
//		stockDayPriceService.saveAll(lastStockDayPriceWithCalculateRsvKDs);
//		List<StockDayPrice>  stockps = stockDayPriceService.findByStartDateAndEndDate(new Date("2024/05/01"), new Date("2024/12/01"));
//		stockps.stream().forEach(stockDayPrice ->{
//			if(stockDayPrice.getClosingPrice().contains("-") || stockDayPrice.getChange().contains("X")) {
//				return;
//			}
//			if(StringUtils.isBlank(stockDayPrice.getClosingPrice())) {
//				return;
//			}
//			String chage = stockDayPrice.getChange().trim().replace("+", "").replaceAll(",", "");
//			if(chage.equals("0.00")) {
//				chage = "0";
//			} 
//			if(chage.equals("除息") || chage.equals("除權")) {
//				Optional<StockDayPrice> stockDayPriceOp = stockDayPriceService.findByStockCodeAndTradingDayBeforLimitOne(stockDayPrice.getStockCode(), stockDayPrice.getTradingDay());
//				if(stockDayPriceOp.isPresent()) {
//					chage = String.valueOf(Double
//							.valueOf(stockDayPrice.getClosingPrice().replace("+", "").replaceAll(",", ""))
//							- Double.valueOf(
//									stockDayPriceOp.get().getClosingPrice().replace("+", "").replaceAll(",", "")));
//				} else {
//					chage = "0";
//				}
//			}
//			Double standarPrice = Double.valueOf(stockDayPrice.getClosingPrice().replace("+", "").replaceAll(",", "")) - new BigDecimal(chage).doubleValue();
//			Double upperLimitPrice = ChromeDriverUtils.calculateLimitPrice(standarPrice,
//			true);
//	Double lowerLimitPrice = ChromeDriverUtils.calculateLimitPrice(standarPrice,
//			false);
//	stockDayPrice.setLimitDown(lowerLimitPrice.toString());
//	stockDayPrice.setLimitUp(upperLimitPrice.toString());
//	Double closingPrice = Double.valueOf(stockDayPrice.getClosingPrice().replace("+", "").replaceAll(",", ""));
//	stockDayPrice.setLimitDown(lowerLimitPrice.toString());
//	stockDayPrice.setLimitUp(upperLimitPrice.toString());
//	Double rate = ((closingPrice - standarPrice) / standarPrice) * 100;
//	rate = Math.round(rate * 100.0) / 100.0;
//	stockDayPrice.setChangeRate(rate);
//		});
//		for(StockDayPrice stockpsDayPrice : stockps) {
//			stockDayPriceService.save(stockpsDayPrice);
//		}

//		List<StockDayPrice>  stockps = stockDayPriceService.findByStartDateAndEndDate(new Date("2024/05/01"), new Date("2024/12/01"));
//		log.info("finsh sync stockDayPrice");
//		List<StockDayPrice>  needFixedStockDayPrices =stockps.stream().map(stockp ->{
//			if(!stockp.getClosingPrice().contains("-") && (stockp.getChange().contains("X") || stockp.getChange().contains("除息") || stockp.getChange().contains("除權"))) {
//				Optional<TmpExDividendsExRightInfo> tmpExDividendsExRightInfoOp = tmpExDividendsExRightInfoService.findByTradingDayAndStockCode(stockp.getTradingDay(), stockp.getStockCode());
//				
//				if(tmpExDividendsExRightInfoOp.isPresent()) {
//					TmpExDividendsExRightInfo tmpExDividendsExRightInfo = tmpExDividendsExRightInfoOp.get();
//					StockDayPrice copyStockDayPrice = stockp;
//					stockp.setLimitDown(tmpExDividendsExRightInfo.getLimitDown());
//					stockp.setLimitUp(tmpExDividendsExRightInfo.getLimitUp());
//					 BigDecimal closingPrice = new BigDecimal(stockp.getClosingPrice().replace(",", StringUtils.EMPTY));
//				        BigDecimal referencePrice = new BigDecimal(tmpExDividendsExRightInfo.getReferencePrice().replace(",", StringUtils.EMPTY));
//				        BigDecimal change = closingPrice.subtract(referencePrice).setScale(2, RoundingMode.HALF_UP);
//				        stockp.setChange(change.toString());
//				        BigDecimal changeRate = closingPrice.subtract(referencePrice).divide(closingPrice,2, RoundingMode.HALF_UP);
//				        stockp.setChangeRate(changeRate.doubleValue());
//				        return copyStockDayPrice;
//				}
//			}
//			return null;
//		}).filter(stocp -> Optional.ofNullable(stocp).isPresent()).toList();
//		stockDayPriceService.saveAll(needFixedStockDayPrices);
	}

	@Scheduled(cron = "${schedule.task.scheduling.cron.expression.update-margin-trading}", zone= "Asia/Taipei")
	@Transactional
//	@PostConstruct
	public void updateMarginTradingAndShortSellingInfo() throws RestClientException, URISyntaxException,
			JsonMappingException, JsonProcessingException, InterruptedException {
		// 先抓DB裡面全部的代號資料
		List<MarginTradingAndShortSellingInfo> stockTpexarginTradingAndShortSellingInfo = ChromeDriverUtils
				.graspTpexMarginTradingAndShortSellingInfo(
						"https://www.tpex.org.tw/openapi/v1/tpex_mainboard_margin_balance");

		List<StockDayPrice> stockTpexDayPrices = ChromeDriverUtils
				.graspTpexDayPrice("https://www.tpex.org.tw/openapi/v1/tpex_mainboard_quotes");

		Date tradeDate = stockTpexDayPrices.stream().findFirst().get().getTradingDay();

		List<MarginTradingAndShortSellingInfo> stockTwseDayPrices = ChromeDriverUtils
				.graspTwseMarginTradingAndShortSellingInfo("https://openapi.twse.com.tw/v1/exchangeReport/MI_MARGN",
						tradeDate);
		marginTradingAndShortSellingInfoServices.saveAll(stockTpexarginTradingAndShortSellingInfo);
		marginTradingAndShortSellingInfoServices.saveAll(stockTwseDayPrices);
		log.info("finsh sync stockDayPrice");
	}

//
	public static String removeFileExtension(String fileName) {
		// 找到最後一個點的位置
		int lastDotIndex = fileName.lastIndexOf('.');

		// 如果找到了點，截取之前的部分；如果沒有點，則返回原始文件名
		if (lastDotIndex != -1) {
			return fileName.substring(0, lastDotIndex);
		} else {
			return fileName; // 沒有擴展名
		}
	}

	public void calculateRSVValueAndLimitDownUp(StockDayPrice stockTwseDayPrice, List<StockDayPrice> twoFourtyStockDayPrices, Map<String, List<TmpExDividendsExRightInfo>> groupedtmpExDividendsExRightInfos ) {
		if (stockTwseDayPrice.getClosingPrice().equals("---") || stockTwseDayPrice.getClosingPrice().equals("----")
				|| stockTwseDayPrice.getClosingPrice().equals("--") || stockTwseDayPrice.getLowPrice().equals("--")
				|| stockTwseDayPrice.getLowPrice().equals("---") || stockTwseDayPrice.getLowPrice().equals("----")
				|| StringUtils.isBlank(stockTwseDayPrice.getClosingPrice())) {
			return;
		}
		if (twoFourtyStockDayPrices.size() < 9) {
			return; // 如果不满足条件，返回 null
		}

		int period = 9;
		List<Double> closingPrices = twoFourtyStockDayPrices.stream().map(innerTwelfthStockDayPrice -> Double
				.valueOf(innerTwelfthStockDayPrice.getClosingPrice().replaceAll(",", ""))).toList();
		List<Double> highPrices = twoFourtyStockDayPrices.stream().map(innerTwelfthStockDayPrice -> Double
				.valueOf(innerTwelfthStockDayPrice.getHighPrice().replaceAll(",", ""))).toList();
		List<Double> lowPrices = twoFourtyStockDayPrices.stream().map(innerTwelfthStockDayPrice -> Double
				.valueOf(innerTwelfthStockDayPrice.getLowPrice().replaceAll(",", ""))).toList();

		double highestHigh = Double.MIN_VALUE;
		double lowestLow = Double.MAX_VALUE;

		for (int i = 0; i < period; i++) {
			highestHigh = Math.max(highestHigh, highPrices.get(i));
			lowestLow = Math.min(lowestLow, lowPrices.get(i));
		}

		double latestClosingPrice = closingPrices.get(0);
		Double rsv = (latestClosingPrice - lowestLow) / (highestHigh - lowestLow) * 100.0;
		if (rsv.isNaN()) {
			rsv = (latestClosingPrice - (lowestLow - 0.001d)) / (highestHigh - (lowestLow - 0.001d)) * 100.0;
		}

		rsv = roundToThreeDecimalPlaces(rsv);
		Double previousK = StringUtils.isNotBlank(twoFourtyStockDayPrices.get(1).getLineKvalue())
				? Double.valueOf(twoFourtyStockDayPrices.get(1).getLineKvalue())
				: 50; // Default initial K value
		Double previousD = StringUtils.isNotBlank(twoFourtyStockDayPrices.get(1).getLineDvalue())
				? Double.valueOf(twoFourtyStockDayPrices.get(1).getLineDvalue())
				: 50;// Default initial D value
		double smoothingFactor = 1.0 / 3.0;

		Double k = previousK * (1 - smoothingFactor) + rsv * smoothingFactor;
		k = roundToThreeDecimalPlaces(k);
		Double d = previousD * (1 - smoothingFactor) + k * smoothingFactor;
		d = roundToThreeDecimalPlaces(d);
		
		stockTwseDayPrice.setLineDvalue(d.toString());
		twoFourtyStockDayPrices.get(0).setLineDvalue(d.toString());
		stockTwseDayPrice.setLineKvalue(k.toString());
		twoFourtyStockDayPrices.get(0).setLineKvalue(k.toString());
		stockTwseDayPrice.setLineRSVvalue(rsv.toString());
		twoFourtyStockDayPrices.get(0).setLineRSVvalue(rsv.toString());
		String tmpChage = stockTwseDayPrice.getChange().trim().replace("+", "").replaceAll(",", "");
		boolean isNeedSpecialDeal = groupedtmpExDividendsExRightInfos.containsKey(stockTwseDayPrice.getStockCode());
		if (tmpChage.equals("0.00")) {
			tmpChage = "0";
		}
		if (tmpChage.contains("除息") || tmpChage.contains("除權") || tmpChage.contains("X")) {
			Optional<StockDayPrice> stockDayPriceOp = stockDayPriceService.findByStockCodeAndTradingDayBeforLimitOne(
					stockTwseDayPrice.getStockCode(), stockTwseDayPrice.getTradingDay());
			if (stockDayPriceOp.isPresent()) {
				tmpChage = String.valueOf(Double
						.valueOf(stockTwseDayPrice.getClosingPrice().replace("+", "").replaceAll(",", ""))
						- Double.valueOf(stockDayPriceOp.get().getClosingPrice().replace("+", "").replaceAll(",", "")));
			} else {
				tmpChage = "0";
			}
		}
		Double standarPrice = null;
		Double upperLimitPrice = null;
		Double lowerLimitPrice = null;
		if (isNeedSpecialDeal) {
			if (groupedtmpExDividendsExRightInfos.get(stockTwseDayPrice.getStockCode()).stream().findFirst().isPresent()) {
				TmpExDividendsExRightInfo tmpExDividendsExRightInfo = groupedtmpExDividendsExRightInfos.get(stockTwseDayPrice.getStockCode()).stream().findFirst().get();
				upperLimitPrice = Double.valueOf(tmpExDividendsExRightInfo.getLimitUp().replaceAll(",", ""));
				lowerLimitPrice = Double.valueOf(tmpExDividendsExRightInfo.getLimitDown().replaceAll(",", ""));
				standarPrice = new BigDecimal(
						tmpExDividendsExRightInfo.getReferencePrice().replace(",", StringUtils.EMPTY)).doubleValue();
			} else {
				return;
			}
		} else {
			standarPrice = Double.valueOf(stockTwseDayPrice.getClosingPrice().replace("+", "").replaceAll(",", ""))
					- new BigDecimal(tmpChage).doubleValue();
			upperLimitPrice = ChromeDriverUtils.calculateLimitPrice(standarPrice, true);
			lowerLimitPrice = ChromeDriverUtils.calculateLimitPrice(standarPrice, false);
		}
		Double closingPrice = Double.valueOf(stockTwseDayPrice.getClosingPrice().replace("+", "").replaceAll(",", ""));
		stockTwseDayPrice.setLimitDown(lowerLimitPrice.toString());
		twoFourtyStockDayPrices.get(0).setLimitDown(lowerLimitPrice.toString());
		stockTwseDayPrice.setLimitUp(upperLimitPrice.toString());
		twoFourtyStockDayPrices.get(0).setLimitUp(upperLimitPrice.toString());
		Double rate = ((closingPrice - standarPrice) / standarPrice) * 100;
		rate = Math.round(rate * 100.0) / 100.0;
		stockTwseDayPrice.setChangeRate(rate);
		twoFourtyStockDayPrices.get(0).setChangeRate(rate);
		// 新增計算移動平均線 (MA) 的邏輯
		int[] maPeriods = { 240, 120, 60, 20, 10, 5 }; // 定義需要計算的移動平均線週期
		for (int maPeriod : maPeriods) {
			Double maValue = calculateMovingAverage(twoFourtyStockDayPrices, maPeriod);
			switch (maPeriod) {
			case 240:
				stockTwseDayPrice.setTwoFourtyDaysMa(maValue.toString()); // 設置 240 日 MA
				twoFourtyStockDayPrices.get(0).setTwoFourtyDaysMa(maValue.toString());
				break;
			case 120:
				stockTwseDayPrice.setOneTwentyDaysMa(maValue.toString()); // 設置 120 日 MA
				twoFourtyStockDayPrices.get(0).setOneTwentyDaysMa(maValue.toString());
				break;
			case 60:
				stockTwseDayPrice.setSixtyDaysMa(maValue.toString()); // 設置 60 日 MA
				twoFourtyStockDayPrices.get(0).setSixtyDaysMa(maValue.toString()); 
				break;
			case 20:
				stockTwseDayPrice.setTwentyDaysMa(maValue.toString()); // 設置 20 日 MA
				twoFourtyStockDayPrices.get(0).setTwentyDaysMa(maValue.toString());
				break;
			case 10:
				stockTwseDayPrice.setTenDaysMa(maValue.toString()); // 設置 10 日 MA
				twoFourtyStockDayPrices.get(0).setTenDaysMa(maValue.toString()); 
				break;
			case 5:
				stockTwseDayPrice.setFiveDaysMa(maValue.toString()); // 設置 5 日 MA
				twoFourtyStockDayPrices.get(0).setFiveDaysMa(maValue.toString());
				break;
			}
		}
	}

	public void calculateRSVValueAndLimitDownUp(StockMonthPrice stockMonthPrice, List<StockMonthPrice> twoFourtyStockMonthPrices) {

		if (twoFourtyStockMonthPrices.size() < 9) {
			return; // 如果不满足条件，返回 null
		}

		int period = 9;
		List<BigDecimal> closingPrices = twoFourtyStockMonthPrices.stream()
				.map(innerTwelfthStockDayPrice -> innerTwelfthStockDayPrice.getClosingPrice()).toList();
		List<BigDecimal> highPrices = twoFourtyStockMonthPrices.stream()
				.map(innerTwelfthStockDayPrice -> innerTwelfthStockDayPrice.getHighPrice()).toList();
		List<BigDecimal> lowPrices = twoFourtyStockMonthPrices.stream()
				.map(innerTwelfthStockDayPrice -> innerTwelfthStockDayPrice.getLowPrice()).toList();

		BigDecimal highestHigh = new BigDecimal(Double.MIN_VALUE);
		BigDecimal lowestLow = new BigDecimal(Double.MAX_VALUE);

		for (int i = 0; i < period; i++) {
			highestHigh = highestHigh.max(highPrices.get(i));
			lowestLow = lowestLow.min(lowPrices.get(i));
		}

		BigDecimal latestClosingPrice = closingPrices.get(0);
		BigDecimal rsv = (latestClosingPrice.subtract(lowestLow))
				.divide(highestHigh.subtract(lowestLow), 6, RoundingMode.HALF_UP).multiply(new BigDecimal("100.0"));
		BigDecimal previousK = ObjectUtils.isNotEmpty(twoFourtyStockMonthPrices.get(1).getLineKValue())
				? twoFourtyStockMonthPrices.get(1).getLineKValue()
				: new BigDecimal("50"); // Default initial K value
		BigDecimal previousD = ObjectUtils.isNotEmpty(twoFourtyStockMonthPrices.get(1).getLineDValue())
				? twoFourtyStockMonthPrices.get(1).getLineDValue()
				: new BigDecimal("50");// Default initial D value

		BigDecimal smoothingFactor = BigDecimal.ONE.divide(new BigDecimal("3.0"), 4, RoundingMode.HALF_UP);
		BigDecimal k = previousK.multiply(BigDecimal.ONE.subtract(smoothingFactor)).add(rsv.multiply(smoothingFactor));
		k = k.setScale(3, RoundingMode.HALF_UP);
		BigDecimal d = previousD.multiply(BigDecimal.ONE.subtract(smoothingFactor)).add(k.multiply(smoothingFactor));
		d = d.setScale(3, RoundingMode.HALF_UP);

		stockMonthPrice.setLineDValue(d);
		twoFourtyStockMonthPrices.get(0).setLineDValue(d);
		stockMonthPrice.setLineKValue(k);
		twoFourtyStockMonthPrices.get(0).setLineDValue(d);
		stockMonthPrice.setLineRsvValue(rsv);
		twoFourtyStockMonthPrices.get(0).setLineRsvValue(rsv);
		// 新增計算移動平均線 (MA) 的邏輯
		int[] maPeriods = { 240, 120, 60, 20, 10, 5 }; // 定義需要計算的移動平均線週期
		for (int maPeriod : maPeriods) {
			BigDecimal maValue = calculateStockMonthPriceMovingAverage(twoFourtyStockMonthPrices, maPeriod);
			switch (maPeriod) {
			case 240:
				stockMonthPrice.setTwoFourtyMonthMa(maValue); // 設置 240 日 MA
				twoFourtyStockMonthPrices.get(0).setTwoFourtyMonthMa(maValue);
				break;
			case 120:
				stockMonthPrice.setOneTwentyMonthMa(maValue); // 設置 120 日 MA
				twoFourtyStockMonthPrices.get(0).setOneTwentyMonthMa(maValue);
				break;
			case 60:
				stockMonthPrice.setSixtyMonthMa(maValue); // 設置 60 日 MA
				twoFourtyStockMonthPrices.get(0).setSixtyMonthMa(maValue);
				break;
			case 20:
				stockMonthPrice.setTwentyMonthMa(maValue); // 設置 20 日 MA
				twoFourtyStockMonthPrices.get(0).setTwentyMonthMa(maValue);
				break;
			case 10:
				stockMonthPrice.setTenMonthMa(maValue); // 設置 10 日 MA
				twoFourtyStockMonthPrices.get(0).setTenMonthMa(maValue);
				break;
			case 5:
				stockMonthPrice.setFiveMonthMa(maValue); // 設置 5 日 MA
				twoFourtyStockMonthPrices.get(0).setFiveMonthMa(maValue);
				break;
			}
		}
	}

	public void calculateRSVValueAndLimitDownUp(StockWeekPrice stockWeekPrice, List<StockWeekPrice> twoFourtyStockWeekPrices) {
		if (twoFourtyStockWeekPrices.size() < 9) {
			return; // 如果不满足条件，返回 null
		}

		int period = 9;
		List<BigDecimal> closingPrices = twoFourtyStockWeekPrices.stream()
				.map(innerTwelfthStockDayPrice -> innerTwelfthStockDayPrice.getClosingPrice()).toList();
		List<BigDecimal> highPrices = twoFourtyStockWeekPrices.stream()
				.map(innerTwelfthStockDayPrice -> innerTwelfthStockDayPrice.getHighPrice()).toList();
		List<BigDecimal> lowPrices = twoFourtyStockWeekPrices.stream()
				.map(innerTwelfthStockDayPrice -> innerTwelfthStockDayPrice.getLowPrice()).toList();

		BigDecimal highestHigh = new BigDecimal(Double.MIN_VALUE);
		BigDecimal lowestLow = new BigDecimal(Double.MAX_VALUE);

		for (int i = 0; i < period; i++) {
			highestHigh = highestHigh.max(highPrices.get(i));
			lowestLow = lowestLow.min(lowPrices.get(i));
		}

		BigDecimal latestClosingPrice = closingPrices.get(0);
		BigDecimal rsv = (latestClosingPrice.subtract(lowestLow))
				.divide(highestHigh.subtract(lowestLow), 6, RoundingMode.HALF_UP).multiply(new BigDecimal("100.0"));
		BigDecimal previousK = ObjectUtils.isNotEmpty(twoFourtyStockWeekPrices.get(1).getLineKValue())
				? twoFourtyStockWeekPrices.get(1).getLineKValue()
				: new BigDecimal("50"); // Default initial K value
		BigDecimal previousD = ObjectUtils.isNotEmpty(twoFourtyStockWeekPrices.get(1).getLineDValue())
				? twoFourtyStockWeekPrices.get(1).getLineDValue()
				: new BigDecimal("50");// Default initial D value

		BigDecimal smoothingFactor = BigDecimal.ONE.divide(new BigDecimal("3.0"), 4, RoundingMode.HALF_UP);
		BigDecimal k = previousK.multiply(BigDecimal.ONE.subtract(smoothingFactor)).add(rsv.multiply(smoothingFactor));
		k = k.setScale(3, RoundingMode.HALF_UP);
		BigDecimal d = previousD.multiply(BigDecimal.ONE.subtract(smoothingFactor)).add(k.multiply(smoothingFactor));
		d = d.setScale(3, RoundingMode.HALF_UP);

		stockWeekPrice.setLineDValue(d);
		twoFourtyStockWeekPrices.get(0).setLineDValue(d);
		stockWeekPrice.setLineKValue(k);
		twoFourtyStockWeekPrices.get(0).setLineKValue(k);
		stockWeekPrice.setLineRsvValue(rsv);
		twoFourtyStockWeekPrices.get(0).setLineRsvValue(rsv);
		// 新增計算移動平均線 (MA) 的邏輯
		int[] maPeriods = { 240, 120, 60, 20, 10, 5 }; // 定義需要計算的移動平均線週期
		for (int maPeriod : maPeriods) {
			BigDecimal maValue = calculateStockWeekPriceMovingAverage(twoFourtyStockWeekPrices, maPeriod);
			switch (maPeriod) {
			case 240:
				stockWeekPrice.setTwoFourtyWeekMa(maValue); // 設置 240 日 MA
				twoFourtyStockWeekPrices.get(0).setTwoFourtyWeekMa(maValue);
				break;
			case 120:
				stockWeekPrice.setOneTwentyWeekMa(maValue); // 設置 120 日 MA
				twoFourtyStockWeekPrices.get(0).setOneTwentyWeekMa(maValue); 
				break;
			case 60:
				stockWeekPrice.setSixtyWeekMa(maValue); // 設置 60 日 MA
				twoFourtyStockWeekPrices.get(0).setSixtyWeekMa(maValue);
				break;
			case 20:
				stockWeekPrice.setTwentyWeekMa(maValue); // 設置 20 日 MA
				twoFourtyStockWeekPrices.get(0).setTwentyWeekMa(maValue);
				break;
			case 10:
				stockWeekPrice.setTenWeekMa(maValue); // 設置 10 日 MA
				twoFourtyStockWeekPrices.get(0).setTenWeekMa(maValue);
				break;
			case 5:
				stockWeekPrice.setFiveWeekMa(maValue); // 設置 5 日 MA
				twoFourtyStockWeekPrices.get(0).setFiveWeekMa(maValue); 
				break;
			}
		}
	}

	private double calculateMovingAverage(List<StockDayPrice> stockDayPrices, int period) {
		if (stockDayPrices.size() < period) {
			return 0.0; // 如果資料不足，返回 0
		}
		double average = stockDayPrices.stream().limit(period)
				.mapToDouble(stockDayPrice -> Double.valueOf(stockDayPrice.getClosingPrice().replaceAll(",", "")))
				.average().orElse(0.0);

		// 使用 BigDecimal 保留小數點第 4 位（四捨五入）
		return new BigDecimal(average).setScale(4, RoundingMode.HALF_UP).doubleValue();
	}

	private double roundToThreeDecimalPlaces(double value) {
		return new BigDecimal(value).setScale(3, RoundingMode.HALF_UP).doubleValue();
	}

	// 計算股票週期移動平均價
	private BigDecimal calculateStockWeekPriceMovingAverage(List<StockWeekPrice> stockDayPrices, int period) {
		if (stockDayPrices.size() < period) {
			return BigDecimal.ZERO; // 如果資料不足，返回 0
		}
		BigDecimal total = stockDayPrices.stream().limit(period).map(stockDayPrice -> stockDayPrice.getClosingPrice())
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		// 計算平均值並保留小數點第 4 位（四捨五入）
		return total.divide(new BigDecimal(period), 4, RoundingMode.HALF_UP);
	}

	// 計算股票週期移動平均價
	private BigDecimal calculateStockMonthPriceMovingAverage(List<StockMonthPrice> stockMonthPrice, int period) {
		if (stockMonthPrice.size() < period) {
			return BigDecimal.ZERO; // 如果資料不足，返回 0
		}
		BigDecimal total = stockMonthPrice.stream().limit(period).map(stockDayPrice -> stockDayPrice.getClosingPrice())
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		// 計算平均值並保留小數點第 4 位（四捨五入）
		return total.divide(new BigDecimal(period), 4, RoundingMode.HALF_UP);
	}
}
