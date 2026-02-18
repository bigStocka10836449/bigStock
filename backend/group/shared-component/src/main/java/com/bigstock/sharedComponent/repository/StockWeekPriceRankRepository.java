package com.bigstock.sharedComponent.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bigstock.sharedComponent.entity.StockWeekPriceRank;

public interface StockWeekPriceRankRepository
		extends JpaRepository<StockWeekPriceRank, StockWeekPriceRank.StockWeekPriceRankId> {

	@Modifying
	@Query(value ="""
			DELETE FROM bstock.stock_week_price_rank r WHERE r.rank_No >= 361
			""", nativeQuery = true)
	void deleteByRankNoLessThanZero();
	
	
	@Modifying
	@Query(value = """
			UPDATE bstock.stock_week_price_rank t
				SET rank_no = r.new_rank
				FROM (
				  SELECT 
				    stock_code,
				    year,
				    week_of_year,
				    RANK() OVER (
				        PARTITION BY stock_code
				        ORDER BY first_trading_day DESC
				    ) AS new_rank
				FROM bstock.stock_week_price_rank
				) r
				WHERE t.stock_code = r.stock_code
				  AND t.year = r.year
				  AND t.week_of_year= r.week_of_year
			""", nativeQuery = true)
	void updateRankNo();
	
	 
}
