package com.bigstock.sharedComponent.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.bigstock.sharedComponent.entity.StockDayPriceRank;

public interface StockDayPriceRankRepository extends JpaRepository<StockDayPriceRank, StockDayPriceRank.StockDayPriceRankId> {

	@Query(value = """
			select s.* from bstock.stock_day_price_rank s where s.stock_Code = :stockCode order by s.rank_No desc
			""", nativeQuery = true)
	List<StockDayPriceRank> findByIdStockCode(String stockCode);
	
	@Query(value = """
			select s.* from bstock.stock_day_price_rank s where s.trading_Day = :tradingDay
			""", nativeQuery = true)
	List<StockDayPriceRank> findByIdTradingDay(LocalDate tradingDay);
	
	@Modifying
	@Query(value = """
			DELETE FROM bstock.stock_day_price_rank r WHERE r.rank_No >= 361
			""", nativeQuery = true)
	void deleteByRankNoLessThanZero();
	
	
	@Modifying
	@Query(value ="""
				             
	  UPDATE bstock.stock_day_price_rank t
		SET rank_no = r.new_rank
		FROM (
		    SELECT 
		        stock_code,
		        trading_day,
		        ROW_NUMBER() OVER (
		            PARTITION BY stock_code
		            ORDER BY trading_day DESC
		        ) AS new_rank
		    FROM bstock.stock_day_price_rank
		) r
		WHERE t.stock_code = r.stock_code
		  AND t.trading_day = r.trading_day
			""", nativeQuery = true)
	void updateRankNo();
	
	
}
