package com.bigstock.schedule.service;

import java.net.URISyntaxException;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import com.bigstock.schedule.utils.ChromeDriverUtils;
import com.bigstock.sharedComponent.entity.StockDayPrice;
import com.bigstock.sharedComponent.service.StockDayPriceService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class GraspStockPrice {
	@Value("${schedule.chromeDriverPath.windows.active}")
	private boolean windowsActive;

	@Value("${schedule.chromeDriverPath.windows.path}")
	private String windowsChromeDriverPath;

	@Value("${schedule.chromeDriverPath.linux.active}")
	private boolean linuxActive;

	@Value("${schedule.chromeDriverPath.linux.driver-path}")
	private String linuxChromeDriverPath;
	
//	@Value("${schedule.chromeDriverPath.linux.chrome-path}")
//	private String linuxChromePath;

	@Value("${schedule.stock-price.url.tpex}")
	private String stockPriceTPEXUrl;

	@Value("${schedule.stock-price.url.twse}")
	private String stockPriceTWSEUrl;

	private final StockDayPriceService stockDayPriceService;

//	@PostConstruct
	// 每周日早上8点触发更新
//	@Scheduled(cron = "${schedule.task.scheduling.cron.expression.grasp-stock-price}")
	public void updateShareholderStructure() throws RestClientException, URISyntaxException, JsonMappingException, JsonProcessingException, InterruptedException {
		// 先抓DB裡面全部的代號資料
		List<StockDayPrice> stockTpexDayPrices = ChromeDriverUtils.graspTpexDayPrice("https://www.tpex.org.tw/openapi/v1/tpex_mainboard_quotes");
		
		Date tradeDate = stockTpexDayPrices.stream().findFirst().get().getTradingDay();
		List<StockDayPrice> stockTwseDayPrices =  ChromeDriverUtils.graspTwseDayPrice("https://openapi.twse.com.tw/v1/exchangeReport/STOCK_DAY_ALL",tradeDate);
		stockDayPriceService.saveAll(stockTpexDayPrices);
		stockDayPriceService.saveAll(stockTwseDayPrices);
		log.info("finsh sync stockDayPrice");
	}
}
