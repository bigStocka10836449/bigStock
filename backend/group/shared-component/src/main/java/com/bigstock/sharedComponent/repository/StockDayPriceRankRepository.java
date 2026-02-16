package com.bigstock.sharedComponent.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bigstock.sharedComponent.entity.StockDayPrice;
import com.bigstock.sharedComponent.entity.StockDayPriceRank;

public interface StockDayPriceRankRepository extends JpaRepository<StockDayPriceRank, StockDayPriceRank.StockDayPriceRankId> {

	@Modifying
	@Query("""
			DELETE FROM StockDayPriceRank r WHERE r.rankNo >= 361
			""")
	void deleteByRankNoLessThanZero();
	
	
	@Modifying
	@Query("""
			Update StockDayPriceRank set rankNo = rankNo + 1
			""")
	void updateRankNo();
	
	
}
