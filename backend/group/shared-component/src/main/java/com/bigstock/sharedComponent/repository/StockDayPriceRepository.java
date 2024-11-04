package com.bigstock.sharedComponent.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bigstock.sharedComponent.entity.StockDayPrice;

public interface StockDayPriceRepository extends JpaRepository<StockDayPrice, StockDayPrice.StockDayPriceId> {

	List<StockDayPrice> findByStockCode(String stockCode);

	@Query("select t from StockDayPrice t where t.stockCode = :stockCode and t.weekOfYear = :weekOfYear order by t.tradingDate asc")
	List<StockDayPrice> findThisWeekStockDayPrices(@Param("stockCode") String stockCode,
			@Param("weekOfYear") String weekOfYear);
	
	Optional<StockDayPrice> findByStockCodeAndTradingDate(String stockCode, Date tradingDate);
	
}
