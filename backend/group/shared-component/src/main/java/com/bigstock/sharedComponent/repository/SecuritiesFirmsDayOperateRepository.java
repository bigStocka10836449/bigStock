package com.bigstock.sharedComponent.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bigstock.sharedComponent.entity.SecuritiesFirmsDayOperate;

public interface SecuritiesFirmsDayOperateRepository
		extends JpaRepository<SecuritiesFirmsDayOperate, SecuritiesFirmsDayOperate.SecuritiesFirmsDayOperateId> {

	Optional<SecuritiesFirmsDayOperate> findById(SecuritiesFirmsDayOperate.SecuritiesFirmsDayOperateId id);
	
	List<SecuritiesFirmsDayOperate> findByStockCode(String stockCode);
	
	List<SecuritiesFirmsDayOperate> findByStockCodeAndTradingDate(String stockCode, Date tradingDate);
}
