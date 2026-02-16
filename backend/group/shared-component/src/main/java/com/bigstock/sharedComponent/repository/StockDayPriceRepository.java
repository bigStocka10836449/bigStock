package com.bigstock.sharedComponent.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bigstock.sharedComponent.entity.StockDayPrice;
import com.bigstock.sharedComponent.entity.StockWeekPrice;

public interface StockDayPriceRepository extends JpaRepository<StockDayPrice, StockDayPrice.StockDayPriceId> {

	List<StockDayPrice> findByStockCode(String stockCode);

	@Query(value = """
			  select sdp.*
			  from bstock.stock_day_price sdp
			  where sdp.trading_day = :tradingDay
			    and sdp.closing_price ~ '^[0-9]+(\\.[0-9]+)?$'
			    and sdp.lmit_up       ~ '^[0-9]+(\\.[0-9]+)?$'
			    and sdp.closing_price::numeric = sdp.lmit_up::numeric
			  order by sdp.stock_code asc
			""", nativeQuery = true)
	List<StockDayPrice> findTodateReachLimitUp(@Param("tradingDay") Date endDate);

	@Query("select t from StockDayPrice t where t.stockCode = :stockCode and t.weekOfYear = :weekOfYear order by t.tradingDay asc")
	List<StockDayPrice> findThisWeekStockDayPrices(@Param("stockCode") String stockCode,
			@Param("weekOfYear") String weekOfYear);

	@Query(value = "select sdp.* from bstock.bstock.stock_day_price sdp where sdp.stock_code =:stockCode order by sdp.trading_day desc limit 1", nativeQuery = true)
	Optional<StockDayPrice> findLastestStockDayPriceByStockCode(@Param("stockCode") String stockCode);

	@Query(value = "select sdp.* from bstock.bstock.stock_day_price sdp where sdp.trading_day between :startDate and :endDate order by sdp.trading_day asc", nativeQuery = true)
	List<StockDayPrice> findByStartDateAndEndDate(@Param("startDate") Date startDate, @Param("endDate") Date endDate);

	@Query(value = "select sdp.* from bstock.bstock.stock_day_price sdp where sdp.stock_code = :stockCode and sdp.trading_day <= ( "
			+ "select sdp1.trading_day from bstock.bstock.stock_day_price sdp1 order by sdp1.trading_day desc limit 1) "
			+ "order by sdp.trading_day desc  limit 240", nativeQuery = true)
	List<StockDayPrice> findPreviousFiftyTowDaysBeforeLastestDayInfo(@Param("stockCode") String stockCode);

	@Query(value = "select sdp.* from bstock.bstock.stock_day_price sdp where sdp.stock_code = :stockCode and sdp.trading_day <= ( "
			+ "select sdp1.trading_day from bstock.bstock.stock_day_price sdp1 order by sdp1.trading_day desc limit 1) "
			+ " and sdp.closing_price not like '%-%' and trim(sdp.closing_price) != ''  order by sdp.trading_day desc  limit :limit", nativeQuery = true)
	List<StockDayPrice> findStockCodeAndLimit(@Param("stockCode") String stockCode, @Param("limit") Integer limit);

	@Query(value = "SELECT 1 FROM bstock.bstock.stock_day_price sdp WHERE sdp.trading_day =:tradingDay  LIMIT 1", nativeQuery = true)
	Integer checkIsTradingDateIsExsits(Date tradingDay);

	@Query(value = "SELECT 1 FROM bstock.bstock.stock_day_price sdp  where sdp.stock_code =:stockCode and sdp.trading_day between :startDate and :endDate and sdp.line_rsv_value is null  LIMIT 1", nativeQuery = true)
	Integer checkIsTradingDateRangeContaineNotCalculate(@Param("stockCode") String stockCode,
			@Param("startDate") Date startDate, @Param("endDate") Date endDate);

	@Query(value = """
			  select sdp.*
			  from bstock.stock_day_price sdp
			  where sdp.stock_code = :stockCode
			    and sdp.trading_day < :endDate
			    and sdp.closing_price ~ '^[0-9]+(\\.[0-9]+)?$'
			  order by sdp.trading_day desc
			  limit 1
			""", nativeQuery = true)
	Optional<StockDayPrice> findByStockCodeAndTradingDayBeforLimitOne(@Param("stockCode") String stockCode,
			@Param("endDate") Date endDate);

	@Query(value = """
			  select sdp.*
			  from bstock.stock_day_price sdp
			  where sdp.stock_code = :stockCode
			    and sdp.trading_day < :endDate
			    and sdp.closing_price ~ '^[0-9]+(\\.[0-9]+)?$'
			  order by sdp.trading_day desc
			  limit 1
			""", nativeQuery = true)
	Optional<StockDayPrice> findByStockCodeAndTradingDayBeforeAndClosePriceIsValidLimitOne(
			@Param("stockCode") String stockCode, @Param("endDate") Date endDate);

	@Query(value = """
			select *
			from bstock.stock_day_price
			where trading_day between :startDateMinus360 and :endDate
			and stock_code between '0000' and '9999'
			and closing_price not in ('--', '----', '')
			and closing_price is not null
			order by stock_code, trading_day desc
			""", nativeQuery = true)
	List<StockDayPrice> findByStockCodeAndTradingDayBeforEqualLimitTwoFourty(@Param("startDateMinus360") Date startDateMinus360,
			@Param("endDate") Date endDate);
	
//	@Query(value = """
//			  select sdp.*
//			  from bstock.stock_day_price sdp
//			  where sdp.stock_code = :stockCode
//			    and sdp.trading_day <= :endDate
//			    and sdp.closing_price ~ '^[0-9]+(\\.[0-9]+)?$'
//			  order by sdp.trading_day desc
//			  limit 240
//			""", nativeQuery = true)
//	List<StockDayPrice> findByStockCodeAndTradingDayBeforEqualLimitTwoFourty(@Param("stockCode") String stockCode,
//			@Param("endDate") Date endDate);

	Optional<StockDayPrice> findByStockCodeAndTradingDay(String stockCode, Date tradingDay);

	@Query(value = """
			select sdp1.trading_day from bstock.bstock.stock_day_price sdp1 order by sdp1.trading_day desc limit 1
			""", nativeQuery = true)
	Optional<StockDayPrice> findLastestStockDayPrice();

	@Query(value = """
			  select sdp.stock_code
			  from bstock.stock_day_price sdp
			  where sdp.trading_day = :tradingDay
			    and sdp.closing_price ~ '^[0-9]+(\\.[0-9]+)?$'
			    and sdp.lmit_up       ~ '^[0-9]+(\\.[0-9]+)?$'
			    and sdp.closing_price = sdp.lmit_up
			    and sdp.change NOT LIKE '%--%'
			    and sdp.change NOT LIKE '%除息%'
			    and sdp.change NOT LIKE '%除權%'
			""", nativeQuery = true)
	List<String> findClosingPriceReachLimitUpStockCodeByTradingDay(@Param("tradingDay") Date tradingDay);

	@Query(value = """
			select sdp.* from bstock.bstock.stock_day_price sdp
			 where sdp.stock_code =:stockCode and sdp.trading_day between :startDate and :endDate order by sdp.trading_day asc
			""", nativeQuery = true)
	List<StockDayPrice> findByStockCodeAndStartDateAndEndDate(@Param("stockCode") String stockCode,
			@Param("startDate") Date startDate, @Param("endDate") Date endDate);

	@Query(value = "select sdp.* from bstock.bstock.stock_day_price sdp where sdp.week_of_year =:weekOfYear ", nativeQuery = true)
	List<StockDayPrice> findByWeekOfYear(@Param("weekOfYear") String weekOfYear);

	@Query(value = "select sdp.* from bstock.bstock.stock_day_price sdp where sdp.month_of_year =:monthOfYear ", nativeQuery = true)
	List<StockDayPrice> findByMonthOfYear(@Param("monthOfYear") String monthOfYear);
	
	
}
