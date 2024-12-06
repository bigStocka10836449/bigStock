package com.bigstock.sharedComponent.service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.aop.framework.AopContext;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;

import com.bigstock.sharedComponent.annotation.BigStockCacheableWithLock;
import com.bigstock.sharedComponent.entity.StockDayPrice;
import com.bigstock.sharedComponent.repository.StockDayPriceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StockDayPriceService {
	private final StockDayPriceRepository stockDayPriceRepository;

	@CacheEvict(value = { "shortLivedCache", "longLivedCache", "defaultCache" }, allEntries = true)
	public StockDayPrice save(StockDayPrice stockDayPrice) {
		return stockDayPriceRepository.save(stockDayPrice);
	}

	@CacheEvict(value = { "shortLivedCache", "longLivedCache", "defaultCache" }, allEntries = true)
	public List<StockDayPrice> saveAll(List<StockDayPrice> stockDayPrices) {
		return stockDayPriceRepository.saveAll(stockDayPrices);
	}

	@CacheEvict(value = { "shortLivedCache", "longLivedCache", "defaultCache" }, allEntries = true)
	public void deleteByIds(List<StockDayPrice.StockDayPriceId> ids) {
		stockDayPriceRepository.deleteAllByIdInBatch(ids);
	};

	public Optional<StockDayPrice> findById(StockDayPrice.StockDayPriceId id) {
		return stockDayPriceRepository.findById(id);
	}

	public List<StockDayPrice> findByStockCode(String stockCode) {
		return stockDayPriceRepository.findByStockCode(stockCode);
	}

	public List<StockDayPrice> findThisWeekStockDayPrices(String stockCode, String weekOfYear) {
		return stockDayPriceRepository.findThisWeekStockDayPrices(stockCode, weekOfYear);
	}

	public Optional<StockDayPrice> findByStockCodeAndTradingDate(String stockCode, Date tradingDate) {
		return stockDayPriceRepository.findByStockCodeAndTradingDay(stockCode, tradingDate);
	}

	@Cacheable(value = "middleLivedCache", key = "#p0 + '-' + #p1 + '-' + #p2")
	public List<StockDayPrice> findByStockCodeAndStartDateAndEndDateCache(String stockCode, String startDate,
			String endDate) throws ParseException {
		return getSelf().findByStockCodeAndStartDateAndEndDate(stockCode, startDate, endDate);
	}

	@BigStockCacheableWithLock(value = "middleLivedCache", key = "#p0 + '-' + #p1 + '-' + #p2")
	public List<StockDayPrice> findByStockCodeAndStartDateAndEndDate(String stockCode, String startDate, String endDate)
			throws ParseException {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		return stockDayPriceRepository.findByStockCodeAndStartDateAndEndDate(stockCode, sdf.parse(startDate),
				sdf.parse(endDate));
	}

	public boolean checkIsTradingDateIsExsits(Date tradingDay) {
		return NumberUtils.INTEGER_ONE.equals(stockDayPriceRepository.checkIsTradingDateIsExsits(tradingDay));
	}

	public List<StockDayPrice> findPreviousFiftyTowDaysBeforeLastestDayInfo(String stockCode) {
		return stockDayPriceRepository.findPreviousFiftyTowDaysBeforeLastestDayInfo(stockCode);
	}

	public List<StockDayPrice> findByStartDateAndEndDate(@Param("startDate") Date startDate,
			@Param("endDate") Date endDate) {
		return stockDayPriceRepository.findByStartDateAndEndDate(startDate, endDate);
	}

	public Optional<StockDayPrice> findByStockCodeAndTradingDayBeforLimitOne(String stockCode, Date endDate) {
		return stockDayPriceRepository.findByStockCodeAndTradingDayBeforLimitOne(stockCode, endDate);
	}

	public List<StockDayPrice> findByStockCodeAndTradingDayBeforEqualLimitTwoFourty(String stockCode, Date endDate) {
		return stockDayPriceRepository.findByStockCodeAndTradingDayBeforEqualLimitTwoFourty(stockCode, endDate);
	}
	
	public Integer checkIsTradingDateRangeContaineNotCalculate(@Param("stockCode") String stockCode,
			@Param("startDate") Date startDate, @Param("endDate") Date endDate) {
		return stockDayPriceRepository.checkIsTradingDateRangeContaineNotCalculate(stockCode, startDate, endDate);
	}

	public Optional<StockDayPrice> findLastestStockDayPrice(){
		return stockDayPriceRepository.findLastestStockDayPrice();
	}
	
	List<String> findClosingPriceReachLimitUpStockCodeByTradingDay(Date tradingDay){
		return stockDayPriceRepository.findClosingPriceReachLimitUpStockCodeByTradingDay(tradingDay);
	}
	
	public List<StockDayPrice> findTodateReachLimitUp(Date endDate){
		return stockDayPriceRepository.findTodateReachLimitUp(endDate);
	}
	
	public List<String> findListStockCode(){
		return stockDayPriceRepository.findListStockCode();
	}
	
	
	private StockDayPriceService getSelf() {
		return (StockDayPriceService) AopContext.currentProxy();
	}
}
