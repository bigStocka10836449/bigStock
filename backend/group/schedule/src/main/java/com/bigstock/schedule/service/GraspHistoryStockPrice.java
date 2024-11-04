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
	
	public void manualGrapRangeHistoryStockPrice(Date startDate, Date endDate, String stockType) {
		
		if("0".equals(stockType)) {
			
			stockInfoService.getStockCodeByStockType("0").stream().forEach(stockCode ->{
				List<StockDayPrice> stockDayPrices;
				try {
					log.info("sync stockCode: {} , startDate : {} , endDate: {}", stockCode, startDate, endDate);
					stockDayPrices = ChromeDriverUtils.getTpexStockHistory(startDate, endDate, manualDateRangeTpexBaseurl, stockCode);
					stockDayPriceService.saveAll(stockDayPrices);
					Thread.sleep(3000);
				} catch (Exception e) {
					throw new RuntimeException(e);
				} 
			});
			log.info("finsh TPEX StockCodePrice sync, startDate : {} , endDate: {}", startDate, endDate);
		} else {
			stockInfoService.getStockCodeByStockType("1").stream().forEach(stockCode ->{
				List<StockDayPrice> stockDayPrices;
				try {
					log.info("sync stockCode: {} , startDate : {} , endDate: {}", stockCode, startDate, endDate);
					stockDayPrices = ChromeDriverUtils.getTwseStockHistory(startDate, endDate, manualDateRangeTwseBaseurl, stockCode);
					stockDayPriceService.saveAll(stockDayPrices);
					Thread.sleep(3000);
				} catch (Exception e) {
					log.warn(e.getMessage(), e);
				} 
			});
			log.info("finsh TWSE StockCodePrice sync, startDate : {} , endDate: {}", startDate, endDate);
		}
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
