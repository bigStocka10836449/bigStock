package com.bigstock.sharedComponent.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bigstock.sharedComponent.entity.StockDayPrice;
import com.bigstock.sharedComponent.entity.StockMonthPrice;
import com.bigstock.sharedComponent.entity.StockWeekPrice;

public interface StockWeekPriceRepository extends JpaRepository<StockWeekPrice, StockWeekPrice.StockWeekPriceId>{

	
	@Query(value = """
			select * from bstock.stock_week_price where year between :startyearMinus6 and :endYear  order by week_of_year desc
			""", nativeQuery = true)
	List<StockWeekPrice> findByYearBeforEqualLimitTwoFourty(@Param("startyearMinus6")String startyearMinus6, @Param("endYear") String endYear);
	
//	@Query(value = "select swp.* from bstock.bstock.stock_week_price swp where swp.stock_code =:stockCode and swp.week_of_year <= :weekOfYear order by swp.week_of_year desc limit 240", 
//		    nativeQuery = true)
//	List<StockWeekPrice> findByStockCodeAndTradingDayBeforEqualLimitTwoFourty(@Param("stockCode")String stockCode, @Param("weekOfYear") String weekOfYear);
	@Query(value = "select swp.* from bstock.bstock.stock_week_price swp where swp.stock_code =:stockCode ", nativeQuery = true)
	List<StockWeekPrice> findBySockCode(@Param("stockCode")String stockCode);
	
	@Query(value = "select swp.* from bstock.bstock.stock_week_price swp where swp.week_of_year = :weekOfYear", nativeQuery = true)
	List<StockWeekPrice> findByWeekOfYear(@Param("weekOfYear")String weekOfYear);
	
	@Query(value = "select swp.* from bstock.bstock.stock_week_price swp where swp.stock_code = :stockCode and swp.week_of_year <= ( "
			+ "select swp1.week_of_year  from bstock.bstock.stock_week_price swp1 order by swp1.week_of_year desc limit 1) "
			+ "order by swp.week_of_year desc  limit :limit", nativeQuery = true)
	List<StockWeekPrice> findStockCodeAndLimit(@Param("stockCode") String stockCode,@Param("limit") Integer limit);
	
	List<StockWeekPrice> findByStockCode(String stockCode);
}
