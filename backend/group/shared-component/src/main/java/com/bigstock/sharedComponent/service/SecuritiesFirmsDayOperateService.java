package com.bigstock.sharedComponent.service;

import java.util.Date;
import java.util.List;

import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import com.bigstock.sharedComponent.entity.SecuritiesFirmsDayOperate;
import com.bigstock.sharedComponent.entity.ShareholderStructure;
import com.bigstock.sharedComponent.repository.SecuritiesFirmsDayOperateRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SecuritiesFirmsDayOperateService {
	private final SecuritiesFirmsDayOperateRepository securitiesFirmsDayOperateRepository;
	
	public SecuritiesFirmsDayOperate insert(SecuritiesFirmsDayOperate securitiesFirmsDayOperate) {
		return securitiesFirmsDayOperateRepository.save(securitiesFirmsDayOperate);
	}
	
	public List<SecuritiesFirmsDayOperate> insertAll(List<SecuritiesFirmsDayOperate> securitiesFirmsDayOperates) {
		return securitiesFirmsDayOperateRepository.saveAll(securitiesFirmsDayOperates);
	}
	
	public void deleteById(SecuritiesFirmsDayOperate.SecuritiesFirmsDayOperateId id) {
		securitiesFirmsDayOperateRepository.deleteById(id);
	}
	
	public void deleteByIds(List<SecuritiesFirmsDayOperate.SecuritiesFirmsDayOperateId> ids) {
		securitiesFirmsDayOperateRepository.deleteAllByIdInBatch(ids);
	}
	
	public List<SecuritiesFirmsDayOperate> getByStockCode(String stockCode){
		return securitiesFirmsDayOperateRepository.findByStockCode(stockCode);
	}
	
	public List<SecuritiesFirmsDayOperate> getByStockCodeAndTradingDay(String stockCode, Date tradingDay){
		return securitiesFirmsDayOperateRepository.findByStockCodeAndTradingDay(stockCode, tradingDay);
	}
}
