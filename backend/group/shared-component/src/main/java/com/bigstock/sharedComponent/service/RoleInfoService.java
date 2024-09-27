package com.bigstock.sharedComponent.service;

import java.math.BigInteger;
import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import com.bigstock.sharedComponent.annotation.BigStockCacheableWithLock;
import com.bigstock.sharedComponent.entity.RoleInfo;
import com.bigstock.sharedComponent.repository.RoleInfoRepository;

@Service
public class RoleInfoService {

	private final RoleInfoRepository roleInfoRepository;

	public RoleInfoService(RoleInfoRepository roleInfoRepository) {
		this.roleInfoRepository = roleInfoRepository;
	}

	@BigStockCacheableWithLock(value = "longLivedCache", key = "'allRoleInfos'")
	public List<RoleInfo> getAll() {
		return roleInfoRepository.findAll();
	}


	@CacheEvict(value = { "longLivedCache"}, allEntries = true)
	public RoleInfo insert(RoleInfo roleInfo) {
		return roleInfoRepository.save(roleInfo);
	}

	@CacheEvict(value = { "longLivedCache"}, allEntries = true)
	public List<RoleInfo> insert(List<RoleInfo> roleInfos) {
		return roleInfoRepository.saveAll(roleInfos);
	}

	@CacheEvict(value = { "longLivedCache"}, allEntries = true)
	public void deleteById(BigInteger id) {
		roleInfoRepository.deleteById(id);
	}

	@CacheEvict(value = { "longLivedCache"}, allEntries = true)
	public void delete(RoleInfo roleInfo) {
		roleInfoRepository.delete(roleInfo);
	}
}
