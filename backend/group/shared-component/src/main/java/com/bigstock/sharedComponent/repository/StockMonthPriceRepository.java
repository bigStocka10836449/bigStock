package com.bigstock.sharedComponent.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bigstock.sharedComponent.entity.StockMonthPrice;

public interface StockMonthPriceRepository extends JpaRepository<StockMonthPrice, StockMonthPrice.StockMonthPriceId> {
	
	@Query(value = "select smp.* from bstock.bstock.stock_month_price smp where smp.stock_code =:stockCode and smp.month_of_year <= :monthOfYear order by smp.month_of_year desc limit 240", 
		    nativeQuery = true)
	List<StockMonthPrice> findByStockCodeAndMmonthOfYearBeforEqualLimitTwoFourty(@Param("stockCode")String stockCode, @Param("monthOfYear") String monthOfYear);
	
	@Query(value = "select smp.* from bstock.bstock.stock_month_price smp where  smp.month_of_year = :monthOfYear", 
		    nativeQuery = true)
	List<StockMonthPrice> findByMmonthOfYear(@Param("monthOfYear") String monthOfYear);
	
	@Query(value = "select smp.* from bstock.bstock.stock_month_price smp where smp.stock_code = :stockCode and smp.month_of_year <= ( "
			+ "select smp1.month_of_year  from bstock.bstock.stock_month_price smp1 order by smp1.month_of_year desc limit 1) "
			+ " order by smp.year , smp.month  desc  limit :limit", nativeQuery = true)
	List<StockMonthPrice> findStockCodeAndLimit(@Param("stockCode") String stockCode,@Param("limit") Integer limit);
	
}

