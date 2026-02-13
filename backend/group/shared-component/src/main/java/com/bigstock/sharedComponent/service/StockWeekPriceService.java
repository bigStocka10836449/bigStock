package com.bigstock.sharedComponent.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;

import com.bigstock.sharedComponent.entity.StockMonthPriceRank;
import com.bigstock.sharedComponent.entity.StockWeekPrice;
import com.bigstock.sharedComponent.entity.StockWeekPrice.StockWeekPriceId;
import com.bigstock.sharedComponent.entity.StockWeekPriceRank;
import com.bigstock.sharedComponent.repository.StockWeekPriceRankRepository;
import com.bigstock.sharedComponent.repository.StockWeekPriceRepository;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StockWeekPriceService {

	private final StockWeekPriceRepository repository;
	
	private final StockWeekPriceRankRepository stockWeekPriceRankRepository;
	
	private final EntityManager entityManager;

	public List<StockWeekPrice> findAll() {
		return repository.findAll();
	}

	public Optional<StockWeekPrice> findById(StockWeekPriceId id) {
		return repository.findById(id);
	}

	public StockWeekPrice save(StockWeekPrice stockWeekPrice) {
		return repository.save(stockWeekPrice);
	}

	public List<StockWeekPrice> saveAll(List<StockWeekPrice> stockWeekPrices) {
		return repository.saveAll(stockWeekPrices);
	}

	public void deleteById(StockWeekPriceId id) {
		repository.deleteById(id);
	}

	public List<StockWeekPrice> findByStockCodeAndTradingDayBeforEqualLimitTwoFourty(String stockCode,
			String weekOfYear) {
		return repository.findByStockCodeAndTradingDayBeforEqualLimitTwoFourty(stockCode, weekOfYear);
	}

	public List<StockWeekPrice> findBySockCode(@Param("stockCode") String stockCode) {
		return repository.findBySockCode(stockCode);
	}

	public List<StockWeekPrice> findByWeekOfYear(String weekOfYear) {
		return repository.findByWeekOfYear(weekOfYear);
	}

	public List<StockWeekPrice> findStockCodeAndLimit(String stockCode, Integer limit) {
		return repository.findStockCodeAndLimit(stockCode, limit);
	}
	
	public List<StockWeekPrice> findRankByStockCode(String stockCode){
		return repository.findByStockCode(stockCode);
	}
	
	public void deleteRankByStockCode(String stockCode) {
		stockWeekPriceRankRepository.deleteByStockCode(stockCode);
	}
	
	@Transactional
	public void batchInsertWeekPrices(List<StockWeekPrice> weekPrices) {

		int batchSize = 100;

		for (int i = 0; i < weekPrices.size(); i++) {
			entityManager.persist(weekPrices.get(i));

			if (i > 0 && i % batchSize == 0) {
				entityManager.flush();
				entityManager.clear();
			}
		}

		entityManager.flush();
		entityManager.clear();
	}
	
	@Transactional
	public void batchMergeMonthPrices(List<StockWeekPrice> weekPrices) {

		int batchSize = 100;

		for (int i = 0; i < weekPrices.size(); i++) {
			entityManager.persist(weekPrices.get(i));

			if (i > 0 && i % batchSize == 0) {
				entityManager.flush();
				entityManager.clear();
			}
		}

		entityManager.flush();
		entityManager.clear();
	}
	
	public List<StockWeekPriceRank> buildRanks(String stockCode, List<StockWeekPrice> WeekPrices) {

		// 依 年 + 交易日期排序 由新到舊排序
		List<StockWeekPrice> sorted = WeekPrices.stream()
				.sorted(Comparator.comparing(StockWeekPrice::getFirstTradingDay).reversed())
				.limit(720).toList();

		List<StockWeekPriceRank> ranks = new ArrayList<>(sorted.size());

		int rankNo = 1;
		for (StockWeekPrice p : sorted) {

			StockWeekPriceRank r = new StockWeekPriceRank();


		    r.setStockCode(p.getStockCode());
		    r.setYear(p.getYear());
		    r.setMonth(p.getMonth());


		    r.setWeekOfYear(p.getWeekOfYear());
		    r.setFirstTradingDay(p.getFirstTradingDay()); 

		    r.setOpeningPrice(p.getOpeningPrice());
		    r.setClosingPrice(p.getClosingPrice());
		    r.setHighPrice(p.getHighPrice());
		    r.setLowPrice(p.getLowPrice());

		    r.setTradingVolume(p.getTradingVolume());
		    r.setChangeRate(p.getChangeRate());

	
		    r.setLineKValue(p.getLineKValue());
		    r.setLineDValue(p.getLineDValue());
		    r.setLineRsvValue(p.getLineRsvValue());


		    r.setFiveWeekMa(p.getFiveWeekMa());
		    r.setTenWeekMa(p.getTenWeekMa());
		    r.setTwentyWeekMa(p.getTwentyWeekMa());
		    r.setSixtyWeekMa(p.getSixtyWeekMa());
		    r.setOneTwentyWeekMa(p.getOneTwentyWeekMa());
		    r.setTwoFourtyWeekMa(p.getTwoFourtyWeekMa());


		    r.setRankNo(rankNo++);

		    ranks.add(r);
		}

		return ranks;
	}
	
	@Transactional
	public void batchInsertWeekRankPrices(List<StockWeekPriceRank> monthPrices) {

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
}
