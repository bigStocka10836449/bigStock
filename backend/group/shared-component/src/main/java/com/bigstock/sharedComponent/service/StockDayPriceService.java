package com.bigstock.sharedComponent.service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import com.bigstock.sharedComponent.annotation.BigStockCacheableWithLock;
import com.bigstock.sharedComponent.entity.StockDayPrice;
import com.bigstock.sharedComponent.repository.StockDayPriceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StockDayPriceService {
	private final  StockDayPriceRepository stockDayPriceRepository;
	
	@CacheEvict(value = { "shortLivedCache", "longLivedCache", "defaultCache" }, allEntries = true)
	public StockDayPrice save(StockDayPrice stockDayPrice) {
		return stockDayPriceRepository.save(stockDayPrice);
	}
	
	@CacheEvict(value = { "shortLivedCache", "longLivedCache", "defaultCache" }, allEntries = true)
	public List<StockDayPrice> saveAll(List<StockDayPrice> stockDayPrices) {
		return stockDayPriceRepository.saveAll(stockDayPrices);
	}
	
	@CacheEvict(value = { "shortLivedCache", "longLivedCache", "defaultCache" }, allEntries = true)
	public void deleteByIds(List<StockDayPrice.StockDayPriceId> ids) {
		stockDayPriceRepository.deleteAllByIdInBatch(ids);
	};
	
	public Optional<StockDayPrice> findById(StockDayPrice.StockDayPriceId id){
		return stockDayPriceRepository.findById(id);
	}
	 
	public List<StockDayPrice> findByStockCode(String stockCode){
		return stockDayPriceRepository.findByStockCode(stockCode);
	}
	
	public List<StockDayPrice> findThisWeekStockDayPrices(String stockCode, String weekOfYear) {
		return stockDayPriceRepository.findThisWeekStockDayPrices(stockCode, weekOfYear);
	}
	public Optional<StockDayPrice> findByStockCodeAndTradingDate(String stockCode, Date tradingDate){
		return stockDayPriceRepository.findByStockCodeAndTradingDay(stockCode, tradingDate);
	}

	@BigStockCacheableWithLock(value = "middleLivedCache", key = "#p0 + '-' + #p1 + '-' + #p2")
	public List<StockDayPrice> findByStockCodeAndStartDateAndEndDate(String stockCode,
			 Date startDate, Date endDate) {
		return stockDayPriceRepository.findByStockCodeAndStartDateAndEndDate(stockCode, startDate, endDate);
	}
	
	public boolean checkIsTradingDateIsExsits(Date tradingDay) {
		return stockDayPriceRepository.checkIsTradingDateIsExsits(tradingDay);
	}
}
