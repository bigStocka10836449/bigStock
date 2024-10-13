package com.bigstock.schedule.service;

import java.net.URISyntaxException;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import com.bigstock.schedule.utils.ChromeDriverUtils;
import com.bigstock.sharedComponent.entity.StockDayPrice;
import com.bigstock.sharedComponent.service.StockDayPriceService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class GraspHistoryStockPrice {
	
	@Value("${schedule.manual-date-range.tpex-baseurl}")
	private String manualDateRangeTpexBaseurl;
	
	@Value("${schedule.manual-date-range.twse-baseurl}")
	private String manualDateRangeTwseBaseurl;
	
	private final StockDayPriceService stockDayPriceService;
	
	private final StockInfoService stockInfoService;
	
	public void manualGrapRangeHistoryStockPrice(Date startDate, Date endDate) {
		
		stockInfoService.getStockCodeByStockType("0").stream().forEach(stockCode ->{
			List<StockDayPrice> stockDayPrices;
			try {
				stockDayPrices = ChromeDriverUtils.getTpexStockHistory(startDate, endDate, manualDateRangeTpexBaseurl, stockCode);
				stockDayPriceService.saveAll(stockDayPrices);
			} catch (Exception e) {
				throw new RuntimeException(e);
			} 
		});
		
		stockInfoService.getStockCodeByStockType("1").stream().forEach(stockCode ->{
			List<StockDayPrice> stockDayPrices;
			try {
				stockDayPrices = ChromeDriverUtils.getTwseStockHistory(startDate, endDate, manualDateRangeTwseBaseurl, stockCode);
				stockDayPriceService.saveAll(stockDayPrices);
			} catch (Exception e) {
				log.warn(e.getMessage(), e);
			} 
		});
	}
	
}
