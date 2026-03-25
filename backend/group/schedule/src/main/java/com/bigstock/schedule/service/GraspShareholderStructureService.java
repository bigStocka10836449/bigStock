package com.bigstock.schedule.service;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import com.bigstock.sharedComponent.entity.ShareholderStructure;
import com.bigstock.sharedComponent.entity.StockInfo;
import com.bigstock.sharedComponent.entity.StockMonthPrice;
import com.bigstock.sharedComponent.entity.StockWeekPrice;
import com.bigstock.sharedComponent.redis.CacheOperatorService;
import com.bigstock.sharedComponent.service.ShareholderStructureService;
import com.bigstock.sharedComponent.service.StockInfoService;
import com.bigstock.sharedComponent.utils.ChromeDriverUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.common.collect.Lists;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class GraspShareholderStructureService {

//	@Value("${schedule.chromeDriverPath.windows.active}")
//	private boolean windowsActive;

//	@Value("${schedule.chromeDriverPath.windows.path}")
//	private String windowsChromeDriverPath;

//	@Value("${schedule.chromeDriverPath.linux.active}")
//	private boolean linuxActive;
//
//	@Value("${schedule.chromeDriverPath.linux.driver-path}")
//	private String linuxChromeDriverPath;
	
//	@Value("${schedule.task.scheduling.cron.expression.sync-start-date}")
//	private String syncStartDate;
	
//	@Value("${schedule.tdccQryStockUrl}")
//	private String tdccQryStockUrl;

//	@Value("${schedule.overTheCounterUrl}")
//	private String overTheCounterUrl;

	private final ShareholderStructureService shareholderStructureService;

	private final StockInfoService stockInfoService;
	
	private final CacheOperatorService cacheOperatorService;

//	@PostConstruct
	// 每天晚上8點更新
	@Scheduled(cron = "${schedule.task.scheduling.cron.expression.update-shareholder-structure}", zone= "Asia/Taipei")
	public void updateShareholderStructure()
			throws RestClientException, URISyntaxException, JsonMappingException, JsonProcessingException {
		// 先抓DB裡面全部的代號資料
		List<Map<Integer, String>> stockCodeWeekInfos = ChromeDriverUtils
				.graspShareholderStructureFromTDCCApi("https://openapi.tdcc.com.tw/v1/opendata/1-5");
		List<ShareholderStructure> shareholderStructures = Lists.newArrayList();
		List<String> tpexStockCodes = stockInfoService.getStockCodeByStockType("0").stream().filter(data -> {
			return !data.matches(".*[a-zA-Z].*");
		}).toList();
		List<String> twseStockCodes = stockInfoService.getStockCodeByStockType("1").stream().filter(data -> {
			return !data.matches(".*[a-zA-Z].*");
		}).toList();
		List<String> allStockCodes = Lists.newArrayList();
		allStockCodes.addAll(tpexStockCodes);
		allStockCodes.addAll(twseStockCodes);
		stockCodeWeekInfos.stream().forEach(stockCodeWeekInfo -> {
			String stockCode = stockCodeWeekInfo.get(37);
			try {
//				if(!stockCode.matches("\\d{4}")) {
//					return;
//				}
				if(stockCode.trim().equals("2330")) {
					log.info(stockCode);
				}
				Optional<StockInfo> stockInfoOp = stockInfoService.findById(stockCode.trim());
//				if (!stockInfoOp.isPresent()) {
					log.info("ssList is empty : {}, so create data", stockCode);
					ShareholderStructure shareholderStructure = refreshStockLatestInfo(stockCode.trim(),
							stockInfoOp.isPresent() ? stockInfoOp.get().getStockName().trim() : stockCode.trim(), stockCodeWeekInfo);
					shareholderStructures.add(shareholderStructure);
//				} else {
//					log.info(String.format("ssList is empty : %1s , and StockInfo is not exsits either", stockCode));
//				}
			} catch (InterruptedException e) {
				log.error(e.getMessage(), e);
			}
		});
		try {
			shareholderStructureService.bulkUpsertShareholderStructure(shareholderStructures);
		} catch (Exception e) {
			log.info(String.format("bulkUpsertShareholderStructure inser fail : %s", e.getMessage()), e);
		}
		Map<String, List<ShareholderStructure>> groupedShareholderStructures = shareholderStructureService.getAll().stream()
		.collect(Collectors.groupingBy(ShareholderStructure::getStockCode));
		groupedShareholderStructures.entrySet().stream().filter(entry -> CollectionUtils.isNotEmpty(entry.getValue()))
		.forEach(entry -> {
			String stockCode = entry.getKey();
			List<ShareholderStructure> singleShareholderStructures = entry.getValue();
			if (allStockCodes.contains(stockCode)) {
				List<ShareholderStructure> cacheStockWeekPrices = cacheOperatorService.getCompressedZSetAllScore(
						"ultraLongLivedCache", "shareholderStructure:compressed:" + stockCode, ShareholderStructure.class);
				if (CollectionUtils.isNotEmpty(cacheStockWeekPrices)) {
					double weekOfYearScore =  ShareholderStructureService.weekOfYearToScore(singleShareholderStructures.stream().findFirst().get().getWeekOfYear());
					cacheOperatorService.upsertCompressedZSetSeries("ultraLongLivedCache",
							"shareholderStructure:compressed:" + stockCode, singleShareholderStructures.stream().findFirst().get(),
							weekOfYearScore,
							CacheOperatorService.DEFAULT_SERIES_MAX_SIZE);
				} else {
					 
					cacheOperatorService.batchUpsertCompressedZSetSeries("ultraLongLivedCache",
							"shareholderStructure:compressed:" + stockCode, singleShareholderStructures,
							shareholderStructure -> ShareholderStructureService.weekOfYearToScore(shareholderStructure.getWeekOfYear()),
							CacheOperatorService.DEFAULT_SERIES_MAX_SIZE);
				}
			} else {
				log.warn("stock_info missing : {}", stockCode);
			}
		});
//		Date currentTradeDate = stockDayPriceService.getCurrentTradeDate();
//		LocalDate tradeDateLdt = LocalDate.ofInstant(currentTradeDate.toInstant(), ZoneId.of("Asia/Taipei"));
//		Integer years = tradeDateLdt.getYear();
//		LocalDate tradeDateMinus365 = tradeDateLdt.minusDays(500);
//		Instant instant = tradeDateMinus365.atStartOfDay(ZoneId.of("Asia/Taipei")).toInstant();
//		Date tradeDateBefore365Days = Date.from(instant);
//		List<MarginTradingAndShortSellingInfo> stockDayPricesFor365Ds = marginTradingAndShortSellingInfoService
//				.findByTradingDayBeforEqualLimitTwoFourty(tradeDateBefore365Days, currentTradeDate);
		List<StockInfo> stockInfos = ChromeDriverUtils
				.getStockInfoByTdccApi("https://openapi.tdcc.com.tw/v1/opendata/1-2");
		stockInfoService.insertAll(stockInfos);
	}

	private ShareholderStructure refreshStockLatestInfo(String stockCode, String stockName,
			Map<Integer, String> weekInfo) throws InterruptedException {
		return createShareholderStructure(weekInfo, stockCode, stockName);
	}

	private ShareholderStructure createShareholderStructure(Map<Integer, String> weekInfo, String stockCode,
			String stockName) {
		ShareholderStructure shareholderStructure = new ShareholderStructure();

		weekInfo.forEach((key, value) -> {
			switch (key) {
			case 0 -> shareholderStructure.setWeekOfYear(value);
			case 1 -> shareholderStructure.setCountDate(value);
			case 2 -> shareholderStructure.setClosingPrice(value);
			case 3 -> shareholderStructure.setPriceChange(value);
			case 4 -> shareholderStructure.setPriceChangePercent(value);
			case 5 -> shareholderStructure.setTdccStock(value);
			case 6 -> shareholderStructure.setLessThanOneBoardLot(value);
			case 7 -> shareholderStructure.setBetweenOneAndFiveBoardLot(value);
			case 8 -> shareholderStructure.setBetweenFiveAndTenBoardLot(value);
			case 9 -> shareholderStructure.setBetweenTenAndFifteenBoardLot(value);
			case 10 -> shareholderStructure.setBetweenFifteenAndTwentyBoardLot(value);
			case 11 -> shareholderStructure.setBetweenTwentyAndThirtyBoardLot(value);
			case 12 -> shareholderStructure.setBetweenThirtyAndFortyBoardLot(value);
			case 13 -> shareholderStructure.setBetweenFortyAndFiftyBoardLot(value);
			case 14 -> shareholderStructure.setBetweenFiftyAndOneHundredBoardLot(value);
			case 15 -> shareholderStructure.setBetweenOneHundredAndTwoHundredBoardLot(value);
			case 16 -> shareholderStructure.setBetweenTwoHundredAndFourHundredBoardLot(value);
			case 17 -> shareholderStructure.setBetweenFourHundredAndSixHundredBoardLot(value);
			case 18 -> shareholderStructure.setBetweenSixHundredAndEightHundredBoardLot(value);
			case 19 -> shareholderStructure.setBetweenEightHundredAndOneThousandBoardLot(value);
			case 20 -> shareholderStructure.setOverOneThousandBoardLot(value);
			case 21 -> shareholderStructure.setStockTotal(value);
			case 22 -> shareholderStructure.setLessThanOneBoardLotPeople(value);
			case 23 -> shareholderStructure.setBetweenOneAndFiveBoardLotPeople(value);
			case 24 -> shareholderStructure.setBetweenFiveAndTenBoardLotPeople(value);
			case 25 -> shareholderStructure.setBetweenTenAndFifteenBoardLotPeople(value);
			case 26 -> shareholderStructure.setBetweenFifteenAndTwentyBoardLotPeople(value);
			case 27 -> shareholderStructure.setBetweenTwentyAndThirtyBoardLotPeople(value);
			case 28 -> shareholderStructure.setBetweenThirtyAndFortyBoardLotPeople(value);
			case 29 -> shareholderStructure.setBetweenFortyAndFiftyBoardLotPeople(value);
			case 30 -> shareholderStructure.setBetweenFiftyAndOneHundredBoardLotPeople(value);
			case 31 -> shareholderStructure.setBetweenOneHundredAndTwoHundredBoardLotPeople(value);
			case 32 -> shareholderStructure.setBetweenTwoHundredAndFourHundredBoardLotPeople(value);
			case 33 -> shareholderStructure.setBetweenFourHundredAndSixHundredBoardLotPeople(value);
			case 34 -> shareholderStructure.setBetweenSixHundredAndEightHundredBoardLotPeople(value);
			case 35 -> shareholderStructure.setBetweenEightHundredAndOneThousandBoardLotPeople(value);
			case 36 -> shareholderStructure.setOverOneThousandBoardLotPeople(value);
			case 38 -> shareholderStructure.setTotalPeople(value);
			}
		});
		shareholderStructure.setStockCode(stockCode);
		shareholderStructure.setStockName(stockName);
		shareholderStructure.setId(stockCode + shareholderStructure.getWeekOfYear());
		return shareholderStructure;
	}
	


//	@PostConstruct
	@Scheduled(cron = "${schedule.task.scheduling.cron.expression.update-stock-info}", zone= "Asia/Taipei")
	public void updateStockInfo() throws InterruptedException, JsonMappingException, RestClientException,
			JsonProcessingException, URISyntaxException {
		List<StockInfo> stockInfos = ChromeDriverUtils
				.getStockInfoByTdccApi("https://openapi.tdcc.com.tw/v1/opendata/1-2");
		stockInfoService.insertAll(stockInfos);
		log.info("finsh sync updateStockInfo ");
	}
	

	
}
