package com.bigstock.sharedComponent.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.bigstock.sharedComponent.entity.StockMonthPrice;
import com.bigstock.sharedComponent.entity.StockMonthPrice.StockMonthPriceId;
import com.bigstock.sharedComponent.repository.StockMonthPriceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StockMonthPriceService {

	private final StockMonthPriceRepository repository;

	public StockMonthPrice save(StockMonthPrice stockMonthPrice) {
		return repository.save(stockMonthPrice);
	}

	public List<StockMonthPrice> saveAll(List<StockMonthPrice> stockMonthPrices) {
		return repository.saveAll(stockMonthPrices);
	}

	public void delete(StockMonthPriceId id) {
		repository.deleteById(id);
	}

	public StockMonthPrice findById(StockMonthPriceId id) {
		return repository.findById(id).orElse(null);
	}

	public List<StockMonthPrice> findByStockCodeAndMmonthOfYearBeforEqualLimitTwoFourty(String stockCode,
			String monthOfYear) {
		return repository.findByStockCodeAndMmonthOfYearBeforEqualLimitTwoFourty(stockCode, monthOfYear);
	}

	public List<StockMonthPrice> findByMmonthOfYear(String monthOfYear) {
		return repository.findByMmonthOfYear(monthOfYear);
	}
	
	
	public List<StockMonthPrice> findStockCodeAndLimit(String stockCode, Integer limit){
		return repository.findStockCodeAndLimit(stockCode, limit);
	}
	
	public List<StockMonthPrice> findAll(){
		return repository.findAll();
	}
}
