package com.bigstock.sharedComponent.repository;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bigstock.sharedComponent.entity.StockExchangeDetail;

public interface StockExchangeDetailRepository extends JpaRepository<StockExchangeDetail, StockExchangeDetail.StockExchangeDetailId> {
    
	@Query(value = "SELECT sed.* FROM bstock.stock_exchange_detail sed  WHERE sed.stock_code = :stockCode and sed.trading_date =:tradingDate ORDER by seq.seq asc", nativeQuery = true)
	List<StockExchangeDetail> findByStockCodeAndTradingDateOrderBySeqAsc(String stockCode, Date tradingDate);
	
	List<StockExchangeDetail> findByStockCodeAndTradingDate(String stockCode, Date tradingDate);
	
	List<StockExchangeDetail> findByTradingDate(Date tradingDate);
	
	@Query(value = "select exists (select 1 from bstock.stock_exchange_detail sed where sed.stock_code = :stockCode and sed.trading_date = :tradingDate)", nativeQuery = true)
	boolean checkIsStockExchangeDetailExsits(@Param("stockCode") String stockCode, @Param("tradingDate") Date tradingDate);
}
