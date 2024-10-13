package com.bigstock.schedule.service;

import java.net.URISyntaxException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import com.bigstock.schedule.utils.ChromeDriverUtils;
import com.bigstock.sharedComponent.entity.ShareholderStructure;
import com.bigstock.sharedComponent.entity.StockDayPrice;
import com.bigstock.sharedComponent.service.ShareholderStructureService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class GraspShareholderStructureService {

	@Value("${schedule.chromeDriverPath.windows.active}")
	private boolean windowsActive;

//	@Value("${schedule.chromeDriverPath.windows.path}")
//	private String windowsChromeDriverPath;

	@Value("${schedule.chromeDriverPath.linux.active}")
	private boolean linuxActive;
//
//	@Value("${schedule.chromeDriverPath.linux.driver-path}")
//	private String linuxChromeDriverPath;
	
	@Value("${schedule.task.scheduling.cron.expression.sync-start-date}")
	private String syncStartDate;
	
	@Value("${schedule.tdccQryStockUrl}")
	private String tdccQryStockUrl;

	@Value("${schedule.overTheCounterUrl}")
	private String overTheCounterUrl;

	private final ShareholderStructureService shareholderStructureService;

	private final StockInfoService stockInfoService;

//	@PostConstruct
	// 每周日早上8点触发更新
//	@Scheduled(cron = "${schedule.task.scheduling.cron.expression.update-shareholder-structure}")
//	public void updateShareholderStructure()
//			throws RestClientException, URISyntaxException, JsonMappingException, JsonProcessingException {
//		// 先抓DB裡面全部的代號資料
//		List<Map<Integer, String>> stockCodeWeekInfos = ChromeDriverUtils
//				.graspShareholderStructureFromTDCCApi("https://openapi.tdcc.com.tw/v1/opendata/1-5");
//		List<ShareholderStructure> shareholderStructures = Lists.newArrayList();
//		stockCodeWeekInfos.stream().forEach(stockCodeWeekInfo -> {
//			String stockCode = stockCodeWeekInfo.get(37);
//			if (stockCode.equals("3686")) {
//				log.warn("");
//			}
//			try {
//				Optional<StockInfo> stockInfoOp = stockInfoService.findById(stockCode);
//				if (stockInfoOp.isPresent()) {
//					log.info("ssList is empty : {}, so create data", stockCode);
//					ShareholderStructure shareholderStructure = refreshStockLatestInfo(stockCode,
//							stockInfoOp.get().getStockName(), stockCodeWeekInfo);
//					shareholderStructures.add(shareholderStructure);
//				} else {
//					log.info(String.format("ssList is empty : %1s , and StockInfo is not exsits either", stockCode));
//				}
//			} catch (InterruptedException e) {
//				log.error(e.getMessage(), e);
//			}
//		});
//		shareholderStructureService.insert(shareholderStructures);
//	}

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
//	@Scheduled(cron = "${schedule.task.scheduling.cron.expression.update-stock-info}")
//	public void updateStockInfo() throws InterruptedException, JsonMappingException, RestClientException,
//			JsonProcessingException, URISyntaxException {
//		List<StockInfo> stockInfos = ChromeDriverUtils
//				.getStockInfoByTdccApi("https://openapi.tdcc.com.tw/v1/opendata/1-2");
//		stockInfoService.insertAll(stockInfos);
//		log.info("finsh sync updateStockInfo ");
//	}
	

	
}
