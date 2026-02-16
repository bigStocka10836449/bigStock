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
	@Query("""
			Update StockMonthPriceRank set rankNo = rankNo + 1
			""")
	void updateRankNo();
	
	 
}
