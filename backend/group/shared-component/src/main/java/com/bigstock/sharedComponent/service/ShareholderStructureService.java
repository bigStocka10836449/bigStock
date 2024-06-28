package com.bigstock.sharedComponent.service;

import java.util.List;
import java.util.Optional;

import org.springframework.aop.framework.AopContext;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.bigstock.sharedComponent.annotation.BacchusCacheableWithLock;
import com.bigstock.sharedComponent.entity.ShareholderStructure;
import com.bigstock.sharedComponent.repository.ShareholderStructureRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ShareholderStructureService {
	private final ShareholderStructureRepository shareholderStructureRepository;

	private final RedissonCacheService redissonCacheService;

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

		return getSelf().getShareholderStructureLastTwoWeeksWithDataBase(firstWeekOfYear, secondWeekOfYear, thirdWeekOfYear);
	}

	@Cacheable(value = "longLivedCache", key = "#stockCode")
	public List<ShareholderStructure> getShareholderStructureByStockCodeDesc(String stockCode) {
		return getSelf().getShareholderStructureByStockCodeDescWithDataBase(stockCode);
	}

	public boolean checkWeekExist(String weekOfYear) {
		return shareholderStructureRepository.countByWeekOfYear(weekOfYear) > 0;
	}

	@BacchusCacheableWithLock(value = "longLivedCache", key = "#id")
	public Optional<ShareholderStructure> getByIdWithDataBase(String id) {
		return shareholderStructureRepository.findById(id);
	}

	@BacchusCacheableWithLock(value = "longLivedCache", key = "#p0 + '-' + #p1 + '-' + #p2")
	public List<ShareholderStructure> getShareholderStructureLastTwoWeeksWithDataBase(String firstWeekOfYear,
			String secondWeekOfYear, String thirdWeekOfYear) {
		return shareholderStructureRepository.getByOverFourHundreLotContinueIncrease(firstWeekOfYear, secondWeekOfYear, thirdWeekOfYear);
	}

	@BacchusCacheableWithLock(value = "longLivedCache", key = "#id")
	public List<ShareholderStructure> getShareholderStructureByStockCodeDescWithDataBase(String stockCode) {
		return shareholderStructureRepository.getShareholderStructureByStockCodeDesc(stockCode);
	}
	
	private ShareholderStructureService getSelf() {
		return (ShareholderStructureService) AopContext.currentProxy();
	}
}
