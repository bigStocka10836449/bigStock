package com.bigstock.sharedComponent.service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.bigstock.sharedComponent.entity.StockDayPrice;
import com.bigstock.sharedComponent.repository.StockDayPriceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StockDayPriceService {
	private final  StockDayPriceRepository stockDayPriceRepository;
	
	public StockDayPrice save(StockDayPrice stockDayPrice) {
		return stockDayPriceRepository.save(stockDayPrice);
	}
	
	public List<StockDayPrice> saveAll(List<StockDayPrice> stockDayPrices) {
		return stockDayPriceRepository.saveAll(stockDayPrices);
	}
	
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
		return stockDayPriceRepository.findByStockCodeAndTradingDate(stockCode, tradingDate);
	}
}
