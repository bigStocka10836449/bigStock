package com.bigstock.sharedComponent.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bigstock.sharedComponent.entity.SecuritiesFirmsDayOperate;
import com.bigstock.sharedComponent.entity.StockDayPrice;

public interface SecuritiesFirmsDayOperateRepository
		extends JpaRepository<SecuritiesFirmsDayOperate, SecuritiesFirmsDayOperate.SecuritiesFirmsDayOperateId> {

	Optional<SecuritiesFirmsDayOperate> findById(SecuritiesFirmsDayOperate.SecuritiesFirmsDayOperateId id);
	
	List<SecuritiesFirmsDayOperate> findByStockCode(String stockCode);
	
	List<SecuritiesFirmsDayOperate> findByStockCodeAndTradingDate(String stockCode, Date tradingDate);
	

	@Query(value = """
			SELECT CASE
			       WHEN EXISTS (
			            SELECT 1
			            FROM bstock.securities_firms_day_operate sfdo
			            WHERE sfdo.stock_code = :stockCode
			            AND sfdo.trading_date = :tradingDate
			       )
			       THEN 1
			       ELSE 0
			       END
			""", nativeQuery = true)
	int chechIsFinishedWithTradingDate(@Param("stockCode") String stockCode, @Param("tradingDate") Date tradingDate);

}
