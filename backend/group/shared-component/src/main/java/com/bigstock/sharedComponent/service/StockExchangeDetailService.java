package com.bigstock.sharedComponent.service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;

import com.bigstock.sharedComponent.entity.StockExchangeDetail;
import com.bigstock.sharedComponent.repository.StockExchangeDetailRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StockExchangeDetailService {

	private final StockExchangeDetailRepository stockExchangeDetailRepository;

	public StockExchangeDetail saveStockExchangeDetail(StockExchangeDetail stockExchangeDetail) {
		return stockExchangeDetailRepository.save(stockExchangeDetail);
	}

	public List<StockExchangeDetail> getAllStockExchangeDetails() {
		return stockExchangeDetailRepository.findAll();
	}

	public Optional<StockExchangeDetail> getStockExchangeDetailById(StockExchangeDetail.StockExchangeDetailId id) {
		return stockExchangeDetailRepository.findById(id);
	}

	public void deleteStockExchangeDetailById(StockExchangeDetail.StockExchangeDetailId id) {
		stockExchangeDetailRepository.deleteById(id);
	}

	public List<StockExchangeDetail> findByStockCodeAndTradingDate(String stockCode, Date tradingDate) {
		return stockExchangeDetailRepository.findByStockCodeAndTradingDate(stockCode, tradingDate);
	}

	public List<StockExchangeDetail> findByTradingDate(Date tradingDate) {
		return stockExchangeDetailRepository.findByTradingDate(tradingDate);
	}

	public List<StockExchangeDetail> saveAll(List<StockExchangeDetail> stockExchangeDetails) {
		return stockExchangeDetailRepository.saveAll(stockExchangeDetails);
	}

	public boolean checkIsStockExchangeDetailExsits(@Param("stockCode") String stockCode,
			@Param("tradingDate") Date tradingDate) {
		return stockExchangeDetailRepository.checkIsStockExchangeDetailExsits(stockCode, tradingDate);
	}

	public List<StockExchangeDetail> findByStockCodeAndTradingDateOrderBySeqAsc(String stockCode, Date tradingDate) {
		return stockExchangeDetailRepository.findByStockCodeAndTradingDateOrderBySeqAsc(stockCode, tradingDate);
	}
}