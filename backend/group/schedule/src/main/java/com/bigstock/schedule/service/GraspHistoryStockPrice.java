package com.bigstock.schedule.service;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.bigstock.schedule.utils.ChromeDriverUtils;
import com.bigstock.sharedComponent.entity.StockDayPrice;
import com.bigstock.sharedComponent.service.StockDayPriceService;
import com.bigstock.sharedComponent.service.StockInfoService;

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
	
	
	public Date getLastTradeDate() {
		Date currentDate = Calendar.getInstance().getTime();
		return stockInfoService.getStockCodeByStockType("0").stream().map(stockCode -> {
			try {
				return ChromeDriverUtils
						.getTpexStockHistory(currentDate, currentDate, manualDateRangeTpexBaseurl, stockCode).stream()
						.map(stockDayPrice -> stockDayPrice.getTradingDate()).toList();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}).filter(tradingDates -> CollectionUtils.isNotEmpty(tradingDates)).flatMap(List::stream)
				.reduce((first, second) -> second).orElseThrow(() -> new RuntimeException("找不到最新的交易日期"));
	}
}
