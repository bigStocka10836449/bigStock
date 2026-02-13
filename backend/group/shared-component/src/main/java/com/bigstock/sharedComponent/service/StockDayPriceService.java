package com.bigstock.sharedComponent.service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
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
import com.bigstock.sharedComponent.entity.StockDayPriceRank;
import com.bigstock.sharedComponent.entity.StockMonthPriceRank;
import com.bigstock.sharedComponent.repository.StockDayPriceRankRepository;
import com.bigstock.sharedComponent.repository.StockDayPriceRepository;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StockDayPriceService {
	private final StockDayPriceRepository stockDayPriceRepository;
	
	private final StockDayPriceRankRepository stockDayPriceRankRepository;
	
	private final EntityManager entityManager;

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

	public Optional<StockDayPrice> findByStockCodeAndTradingDayBeforeAndClosePriceIsValidLimitOne(String stockCode,
			Date endDate) {
		return stockDayPriceRepository.findByStockCodeAndTradingDayBeforeAndClosePriceIsValidLimitOne(stockCode,
				endDate);
	}

	public Integer checkIsTradingDateRangeContaineNotCalculate(@Param("stockCode") String stockCode,
			@Param("startDate") Date startDate, @Param("endDate") Date endDate) {
		return stockDayPriceRepository.checkIsTradingDateRangeContaineNotCalculate(stockCode, startDate, endDate);
	}

	public Optional<StockDayPrice> findLastestStockDayPrice() {
		return stockDayPriceRepository.findLastestStockDayPrice();
	}

	List<String> findClosingPriceReachLimitUpStockCodeByTradingDay(Date tradingDay) {
		return stockDayPriceRepository.findClosingPriceReachLimitUpStockCodeByTradingDay(tradingDay);
	}

	public List<StockDayPrice> findTodateReachLimitUp(Date endDate) {
		return stockDayPriceRepository.findTodateReachLimitUp(endDate);
	}

	public List<StockDayPrice> findByWeekOfYear(String weekOfYear) {
		return stockDayPriceRepository.findByWeekOfYear(weekOfYear);
	}

	public List<StockDayPrice> findByMonthOfYear(String monthOfYear) {
		return stockDayPriceRepository.findByMonthOfYear(monthOfYear);
	}

	public List<StockDayPrice> findStockCodeAndLimit(String stockCode, Integer limit) {
		return stockDayPriceRepository.findStockCodeAndLimit(stockCode, limit);
	}

	private StockDayPriceService getSelf() {
		return (StockDayPriceService) AopContext.currentProxy();
	}
	
	@Transactional
	public void batchInsertDayRankPrices(List<StockDayPriceRank> monthPrices) {

		int batchSize = 100;

		for (int i = 0; i < monthPrices.size(); i++) {
			entityManager.persist(monthPrices.get(i));

			if (i > 0 && i % batchSize == 0) {
				entityManager.flush();
				entityManager.clear();
			}
		}

		entityManager.flush();
		entityManager.clear();
	}
	
	public void deleteRankByStockCode(String stockCode) {
		stockDayPriceRankRepository.deleteByStockCode(stockCode);
	}

	public List<StockDayPriceRank> buildRanks(String stockCode, List<StockDayPrice> monthPrices) {

		// 依 年 + 月 由新到舊排序
		List<StockDayPrice> sorted = monthPrices.stream()
				.sorted(Comparator.comparing(StockDayPrice::getTradingDay).reversed())
				.limit(720).toList();

		List<StockDayPriceRank> ranks = new ArrayList<>(sorted.size());

		int rankNo = 1;
		for (StockDayPrice p : sorted) {

			StockDayPriceRank r = new StockDayPriceRank();

		    // =========================
		    // 🔹 PK / Business Key
		    // =========================
		    r.setStockCode(p.getStockCode());
		    r.setWeekOfYear(p.getWeekOfYear());
		    r.setMonthOfYear(p.getMonthOfYear());

		    // =========================
		    // 🔹 時間相關欄位
		    // =========================
		    r.setMonthOfYear(p.getMonthOfYear());
		    r.setTradingDay(p.getTradingDay()); 
		    // 若你擔心 Date 被修改，可改成：
		    // new Date(p.getFirstTradingDay().getTime())

		    // =========================
		    // 🔹 價格資訊（完整快照）
		    // =========================
		    r.setOpeningPrice(p.getOpeningPrice());
		    r.setClosingPrice(p.getClosingPrice());
		    r.setHighPrice(p.getHighPrice());
		    r.setLowPrice(p.getLowPrice());

		    // =========================
		    // 🔹 成交 / 變動
		    // =========================
		    r.setTradingVolume(p.getTradingVolume());
		    r.setChangeRate(p.getChangeRate());

		    // =========================
		    // 🔹 技術指標（KD / RSV）
		    // =========================
		    r.setLineKvalue(p.getLineKvalue());
		    r.setLineDvalue(p.getLineDvalue());
		    r.setLineRSVvalue(p.getLineRSVvalue());

		    // =========================
		    // 🔹 均線（MA）
		    // =========================
		    r.setFiveDaysMa(p.getFiveDaysMa());
		    r.setTenDaysMa(p.getTenDaysMa());
		    r.setTwentyDaysMa(p.getTwentyDaysMa());
		    r.setSixtyDaysMa(p.getSixtyDaysMa());
		    r.setOneTwentyDaysMa(p.getOneTwentyDaysMa());
		    r.setTwoFourtyDaysMa(p.getTwoFourtyDaysMa());

		    // =========================
		    // 🔹 Rank only（唯一新產生）
		    // =========================
		    r.setRankNo(rankNo++);

		    ranks.add(r);
		}

		return ranks;
	}
}
