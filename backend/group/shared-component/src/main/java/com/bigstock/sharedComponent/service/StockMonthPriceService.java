package com.bigstock.sharedComponent.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import com.bigstock.sharedComponent.entity.StockMonthPrice;
import com.bigstock.sharedComponent.entity.StockMonthPrice.StockMonthPriceId;
import com.bigstock.sharedComponent.entity.StockMonthPriceRank;
import com.bigstock.sharedComponent.repository.StockMonthPriceRankRepository;
import com.bigstock.sharedComponent.repository.StockMonthPriceRepository;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StockMonthPriceService {

	private final EntityManager entityManager;

	private final StockMonthPriceRepository repository;
	
	private final StockMonthPriceRankRepository stockMonthPriceRankRepository;

	public StockMonthPrice save(StockMonthPrice stockMonthPrice) {
		return repository.save(stockMonthPrice);
	}

	public List<StockMonthPrice> saveAll(List<StockMonthPrice> stockMonthPrices) {
		return repository.saveAll(stockMonthPrices);
	}

	public void delete(StockMonthPriceId id) {
		repository.deleteById(id);
	}
	
	public void deleteRankByStockCode(String stockCode) {
		stockMonthPriceRankRepository.deleteByStockCode(stockCode);
	}

	public StockMonthPrice findById(StockMonthPriceId id) {
		return repository.findById(id).orElse(null);
	}
	
	public List<StockMonthPrice> findRankByStockCode(String stockCode){
		return repository.findByStockCode(stockCode);
	}

	public List<StockMonthPrice> findByStockCodeAndMmonthOfYearBeforEqualLimitTwoFourty(String stockCode,
			String monthOfYear) {
		return repository.findByStockCodeAndMmonthOfYearBeforEqualLimitTwoFourty(stockCode, monthOfYear);
	}

	public List<StockMonthPrice> findByMmonthOfYear(String monthOfYear) {
		return repository.findByMmonthOfYear(monthOfYear);
	}

	public List<StockMonthPrice> findStockCodeAndLimit(String stockCode, Integer limit) {
		return repository.findStockCodeAndLimit(stockCode, limit);
	}

	public List<StockMonthPrice> findAll() {
		return repository.findAll();
	}

	@Transactional
	public void batchInsertMonthPrices(List<StockMonthPrice> monthPrices) {

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
	
	@Transactional
	public void batchInsertMonthRankPrices(List<StockMonthPriceRank> monthPrices) {

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

	public List<StockMonthPriceRank> buildRanks(String stockCode, List<StockMonthPrice> monthPrices) {

		// 依 年 + 月 由新到舊排序
		List<StockMonthPrice> sorted = monthPrices.stream()
				.sorted(Comparator.comparingInt((StockMonthPrice p) -> Integer.parseInt(p.getYear()))
						.thenComparingInt(StockMonthPrice::getMonth).reversed())
				.limit(720).toList();

		List<StockMonthPriceRank> ranks = new ArrayList<>(sorted.size());

		int rankNo = 1;
		for (StockMonthPrice p : sorted) {

		    StockMonthPriceRank r = new StockMonthPriceRank();

		    // =========================
		    // 🔹 PK / Business Key
		    // =========================
		    r.setStockCode(p.getStockCode());
		    r.setYear(p.getYear());
		    r.setMonth(p.getMonth());

		    // =========================
		    // 🔹 時間相關欄位
		    // =========================
		    r.setMonthOfYear(p.getMonthOfYear());
		    r.setFirstTradingDay(p.getFirstTradingDay()); 
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
		    r.setLineKValue(p.getLineKValue());
		    r.setLineDValue(p.getLineDValue());
		    r.setLineRsvValue(p.getLineRsvValue());

		    // =========================
		    // 🔹 均線（MA）
		    // =========================
		    r.setFiveMonthMa(p.getFiveMonthMa());
		    r.setTenMonthMa(p.getTenMonthMa());
		    r.setTwentyMonthMa(p.getTwentyMonthMa());
		    r.setSixtyMonthMa(p.getSixtyMonthMa());
		    r.setOneTwentyMonthMa(p.getOneTwentyMonthMa());
		    r.setTwoFourtyMonthMa(p.getTwoFourtyMonthMa());

		    // =========================
		    // 🔹 Rank only（唯一新產生）
		    // =========================
		    r.setRankNo(rankNo++);

		    ranks.add(r);
		}

		return ranks;
	}
}
