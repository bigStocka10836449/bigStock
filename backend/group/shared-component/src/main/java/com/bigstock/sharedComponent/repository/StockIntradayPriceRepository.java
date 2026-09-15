package com.bigstock.sharedComponent.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.bigstock.sharedComponent.entity.StockIntradayPrice;

@Repository
public interface StockIntradayPriceRepository
		extends JpaRepository<StockIntradayPrice, StockIntradayPrice.StockIntradayPriceId> {

	@Query(value = """
			SELECT *
				FROM (
				    SELECT
				        sip.*,
				        ROW_NUMBER() OVER (
				            PARTITION BY sip.stock_code
				            ORDER BY sip.trading_time DESC
				        ) AS rn
				    FROM bstock.stock_intraday_price sip
				    WHERE sip.period = :period
				) t
				WHERE t.rn <= 60
				ORDER BY t.stock_code, t.trading_time ASC;
			""", nativeQuery = true)
	List<StockIntradayPrice> findTop60ByPeriod(@Param("period") String period);

	List<StockIntradayPrice> findTop60ByStockCodeAndPeriodOrderByTradingTimeDesc(String stockCode, String period);

	List<StockIntradayPrice> findByStockCodeAndPeriodOrderByTradingTimeAsc(String stockCode, String period);

	Optional<StockIntradayPrice> findFirstByStockCodeAndPeriodOrderByTradingTimeDesc(String stockCode, String period);

	Optional<StockIntradayPrice> findFirstByStockCodeAndPeriodOrderByTradingTimeAsc(String stockCode, String period);

	@Modifying
	@Query("DELETE FROM StockIntradayPrice s " + "WHERE s.period = :period " + "AND s.tradingTime < :cutoffTime")
	int deleteBefore(@Param("period") String period, @Param("cutoffTime") LocalDateTime cutoffTime);
}
