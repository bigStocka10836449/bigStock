package com.bigstock.sharedComponent.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.bigstock.sharedComponent.entity.MarginTradingAndShortSellingInfo;

@Repository
public interface MarginTradingAndShortSellingInfoRepository extends
		JpaRepository<MarginTradingAndShortSellingInfo, MarginTradingAndShortSellingInfo.MarginTradingAndShortSellingInfoId> {

	@Query(value = "SELECT 1 FROM bstock.bstock.margin_trading_and_short_selling_info mtassi WHERE mtassi .trading_day =:tradingDay  LIMIT 1", nativeQuery = true)
	public 
}
