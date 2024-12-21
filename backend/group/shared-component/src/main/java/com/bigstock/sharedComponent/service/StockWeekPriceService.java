package com.bigstock.sharedComponent.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;

import com.bigstock.sharedComponent.entity.StockWeekPrice;
import com.bigstock.sharedComponent.entity.StockWeekPrice.StockWeekPriceId;
import com.bigstock.sharedComponent.repository.StockWeekPriceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StockWeekPriceService {

    private final StockWeekPriceRepository repository;

    public List<StockWeekPrice> findAll() {
        return repository.findAll();
    }

    public Optional<StockWeekPrice> findById(StockWeekPriceId id) {
        return repository.findById(id);
    }

    public StockWeekPrice save(StockWeekPrice stockWeekPrice) {
        return repository.save(stockWeekPrice);
    }

    public List<StockWeekPrice> saveAll(List<StockWeekPrice> stockWeekPrices) {
        return repository.saveAll(stockWeekPrices);
    }
    
    public void deleteById(StockWeekPriceId id) {
        repository.deleteById(id);
    }
    
    public List<StockWeekPrice> findByStockCodeAndTradingDayBeforEqualLimitTwoFourty(String stockCode, String weekOfYear){
    	return repository.findByStockCodeAndTradingDayBeforEqualLimitTwoFourty(stockCode, weekOfYear);
    }
    
    public List<StockWeekPrice> findBySockCode(@Param("stockCode")String stockCode){
    	return repository.findBySockCode(stockCode);
    }
    
    public List<StockWeekPrice> findByWeekOfYear(String weekOfYear){
    	return repository.findByWeekOfYear(weekOfYear);
    }
}

