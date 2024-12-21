package com.bigstock.sharedComponent.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bigstock.sharedComponent.entity.TmpExDividendsExRightInfo;

public interface TmpExDividendsExRightInfoRepository extends JpaRepository<TmpExDividendsExRightInfo, TmpExDividendsExRightInfo.TmpExDividendsExRightInfoId> {

	@Query(value = "select tederi.* from bstock.bstock.tmp_ex_dividends_ex_right_info tederi where tederi.trading_day = :tradingDay ", nativeQuery = true)
	public List<TmpExDividendsExRightInfo> findByTradingDay(@Param("tradingDay") Date tradingDay);
	
	@Query(value = "select tederi.* from bstock.bstock.tmp_ex_dividends_ex_right_info tederi where tederi.trading_day = :tradingDay and tederi.stock_code = :stockCode ", nativeQuery = true)
	public Optional<TmpExDividendsExRightInfo> findByTradingDayAndStockCode(@Param("tradingDay") Date tradingDay, @Param("stockCode") String stockCode);
}
