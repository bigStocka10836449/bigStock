package com.bigstock.sharedComponent.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bigstock.sharedComponent.entity.StockMonthPriceRank;

public interface StockMonthPriceRankRepository
		extends JpaRepository<StockMonthPriceRank, StockMonthPriceRank.StockMonthPriceRankId> {
	@Modifying
	@Query("""
			DELETE FROM StockMonthPriceRank r WHERE r.rankNo >= 361
			""")
	void deleteByRankNoLessThanZero();
	
	
	@Modifying
	@Query(value = """
		UPDATE bstock.stock_month_price_rank t
		SET rank_no = r.new_rank
		FROM (
		    SELECT 
		        stock_code,
		        year,
		        month,
		        ROW_NUMBER() OVER (
		            PARTITION BY stock_code
		            ORDER BY first_trading_day DESC
		        ) AS new_rank
		    FROM bstock.stock_month_price_rank
		) r
		WHERE t.stock_code = r.stock_code
		  AND t.year = r.year
		  AND t.month = r.month
			""", nativeQuery = true)
	void updateRankNo();
	
	 
}
