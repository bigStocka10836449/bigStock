package com.bigstock.sharedComponent.service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.aop.framework.AopContext;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.bigstock.sharedComponent.annotation.BigStockCacheableWithLock;
import com.bigstock.sharedComponent.entity.ShareholderStructure;
import com.bigstock.sharedComponent.entity.StockDayPrice;
import com.bigstock.sharedComponent.repository.ShareholderStructureRepository;
import com.bigstock.sharedComponent.repository.StockDayPriceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ShareholderStructureService {
	private final ShareholderStructureRepository shareholderStructureRepository;

	private final StockDayPriceRepository stockDayPriceRepository;

	public List<ShareholderStructure> getAll() {
		return shareholderStructureRepository.findAll();
	}

	@Cacheable(value = "longLivedCache", key = "#id")
	public Optional<ShareholderStructure> getById(String id) {
		return getSelf().getByIdWithDataBase(id);
	}

	@CacheEvict(value = { "shortLivedCache", "longLivedCache", "defaultCache" }, allEntries = true)
	public ShareholderStructure insert(ShareholderStructure shareholderStructure) {
		return shareholderStructureRepository.save(shareholderStructure);
	}

	@CacheEvict(value = { "shortLivedCache", "longLivedCache", "defaultCache" }, allEntries = true)
	public List<ShareholderStructure> insert(List<ShareholderStructure> shareholderStructures) {
		return shareholderStructureRepository.saveAll(shareholderStructures);
	}

	@CacheEvict(value = { "shortLivedCache", "longLivedCache", "defaultCache" }, allEntries = true)
	public void delete(String id) {
		shareholderStructureRepository.deleteById(id);
	}

	@CacheEvict(value = { "shortLivedCache", "longLivedCache", "defaultCache" }, allEntries = true)
	public void delete(ShareholderStructure shareholderStructure) {
		shareholderStructureRepository.delete(shareholderStructure);
	}

	public List<String> getAllShareholderStructureStockCode() {
		return shareholderStructureRepository.getAllShareholderStructureStockCode();
	}

	@Cacheable(value = "longLivedCache", key = "#p0 + '-' + #p1 + '-' + #p2")
	public List<ShareholderStructure> getShareholderStructureLastTwoWeeks(String firstWeekOfYear,
			String secondWeekOfYear, String thirdWeekOfYear) {

		return getSelf().getShareholderStructureLastTwoWeeksWithDataBase(firstWeekOfYear, secondWeekOfYear,
				thirdWeekOfYear);
	}

	@Cacheable(value = "longLivedCache", key = "#stockCode")
	public List<ShareholderStructure> getShareholderStructureByStockCodeDesc(String stockCode) {
		return getSelf().getShareholderStructureByStockCodeDescWithDataBase(stockCode);
	}

	public boolean checkWeekExist(String weekOfYear) {
		return shareholderStructureRepository.countByWeekOfYear(weekOfYear) > 0;
	}

	@BigStockCacheableWithLock(value = "longLivedCache", key = "#id")
	public Optional<ShareholderStructure> getByIdWithDataBase(String id) {
		return shareholderStructureRepository.findById(id);
	}

	@BigStockCacheableWithLock(value = "longLivedCache", key = "#p0 + '-' + #p1 + '-' + #p2")
	public List<ShareholderStructure> getShareholderStructureLastTwoWeeksWithDataBase(String firstWeekOfYear,
			String secondWeekOfYear, String thirdWeekOfYear) {
		return shareholderStructureRepository.getByOverFourHundreLotContinueIncrease(firstWeekOfYear, secondWeekOfYear,
				thirdWeekOfYear);
	}

	@BigStockCacheableWithLock(value = "longLivedCache", key = "#id")
	public List<ShareholderStructure> getShareholderStructureByStockCodeDescWithDataBase(String stockCode) {
//		stockDayPriceRepository
		List<ShareholderStructure> shareholderStructures = shareholderStructureRepository
				.getShareholderStructureByStockCodeDesc(stockCode);
		shareholderStructures.stream().forEach(data -> {
			List<StockDayPrice> stockDayPrices = stockDayPriceRepository.findThisWeekStockDayPrices(data.getStockCode(),
					data.getWeekOfYear());
	        Optional<StockDayPrice> minTradingDayPrice = stockDayPrices.stream()
	                .min(Comparator.comparing(StockDayPrice::getTradingDay));

	        // 找到最大的 tradingDay 的 StockDayPrice 对象
	        Optional<StockDayPrice> maxTradingDayPrice = stockDayPrices.stream()
	                .max(Comparator.comparing(StockDayPrice::getTradingDay));
	        data.setClosingPrice(maxTradingDayPrice.isPresent() ? maxTradingDayPrice.get().getClosingPrice() : "0.0");
	        data.setOpeningPrice(minTradingDayPrice.isPresent() ? minTradingDayPrice.get().getOpeningPrice() : "0.0");
		});
		return shareholderStructureRepository.getShareholderStructureByStockCodeDesc(stockCode);
	}

	private ShareholderStructureService getSelf() {
		return (ShareholderStructureService) AopContext.currentProxy();
	}
}
