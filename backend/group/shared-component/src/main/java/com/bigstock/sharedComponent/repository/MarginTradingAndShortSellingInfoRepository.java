package com.bigstock.sharedComponent.repository;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.bigstock.sharedComponent.entity.MarginTradingAndShortSellingInfo;

@Repository
public interface MarginTradingAndShortSellingInfoRepository extends
		JpaRepository<MarginTradingAndShortSellingInfo, MarginTradingAndShortSellingInfo.MarginTradingAndShortSellingInfoId> {

	@Query(value = "select mtassi.* from bstock.bstock.margin_trading_and_short_selling_info mtassi where  mtassi.stock_code =:stockCode and mtassi.trading_day between :firstDate and :secondDate order by mtassi.trading_day desc", nativeQuery = true)
	public List<MarginTradingAndShortSellingInfo> findMarginTradingAndShortSellingInfoByDateRange(@Param("stockCode") String stockCode, @Param("firstDate") Date firstDate, @Param("secondDate") Date secondDate);
	
}
