package com.bigstock.sharedComponent.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bigstock.sharedComponent.entity.StockWeekPriceRank;

public interface StockWeekPriceRankRepository
		extends JpaRepository<StockWeekPriceRank, StockWeekPriceRank.StockWeekPriceRankId> {

	@Modifying
	@Query("""
			DELETE FROM StockWeekPriceRank r WHERE r.rankNo >= 361
			""")
	void deleteByRankNoLessThanZero();
	
	
	@Modifying
	@Query("""
			Update StockWeekPriceRank set rankNo = rankNo + 1
			""")
	void updateRankNo();
	
	 
}
