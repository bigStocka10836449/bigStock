package com.bigstock.sharedComponent.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bigstock.sharedComponent.entity.MarginTradingAndShortSellingInfo;

@Repository
public interface MarginTradingAndShortSellingInfoRepository extends
		JpaRepository<MarginTradingAndShortSellingInfo, MarginTradingAndShortSellingInfo.MarginTradingAndShortSellingInfoId> {

	// Custom queries can be added here
}
