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

	@Query("select t from StockDayPrice t where t.stockCode = :stockCode and t.weekOfYear = :weekOfYear order by t.tradingDay asc")
	List<StockDayPrice> findThisWeekStockDayPrices(@Param("stockCode") String stockCode,
			@Param("weekOfYear") String weekOfYear);
	
	
	@Query(value = "SELECT 1 FROM bstock.bstock.stock_day_price sdp WHERE sdp.trading_day =:tradingDay  LIMIT 1", nativeQuery = true)
	Integer checkIsTradingDateIsExsits(Date tradingDay);
	
	Optional<StockDayPrice> findByStockCodeAndTradingDay(String stockCode, Date tradingDay);
	
	@Query(value = "select sdp.* from bstock.bstock.stock_day_price sdp where sdp.stock_code =:stockCode and sdp.trading_day between :startDate and :endDate" , nativeQuery = true)
	List<StockDayPrice> findByStockCodeAndStartDateAndEndDate(@Param("stockCode") String stockCode, @Param("startDate") Date startDate, @Param("endDate") Date endDate);
}
