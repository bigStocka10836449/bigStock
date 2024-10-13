package com.bigstock.schedule.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.bigstock.sharedComponent.entity.StockInfo;
import com.bigstock.sharedComponent.repository.StockInfoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StockInfoService {
	private final StockInfoRepository stockInfoRepository;

	public List<String> getAllStockCode() {
		return stockInfoRepository.getAllStockCode();
	}
	
	public List<StockInfo> insertAll(List<StockInfo> stockInfos){
		return stockInfoRepository.saveAll(stockInfos);
	}
	
	public List<StockInfo> findByStockType(String stockType){
		return stockInfoRepository.findByStockType(stockType);
	}
	
	public Optional<StockInfo> findById(String stockCode){
		return stockInfoRepository.findById(stockCode);
	}
	
	public List<String> getStockCodeByStockType(String stockType) {
		return stockInfoRepository.getStockCodeByStockType(stockType);
	}
}
