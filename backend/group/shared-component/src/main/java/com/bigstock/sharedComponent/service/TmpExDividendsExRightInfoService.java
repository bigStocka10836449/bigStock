package com.bigstock.sharedComponent.service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;

import com.bigstock.sharedComponent.entity.TmpExDividendsExRightInfo;
import com.bigstock.sharedComponent.entity.TmpExDividendsExRightInfo.TmpExDividendsExRightInfoId;
import com.bigstock.sharedComponent.repository.TmpExDividendsExRightInfoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TmpExDividendsExRightInfoService {

    private final TmpExDividendsExRightInfoRepository repository;

    public TmpExDividendsExRightInfo save(TmpExDividendsExRightInfo entity) {
        return repository.save(entity);
    }

    public List<TmpExDividendsExRightInfo> saveAll(List<TmpExDividendsExRightInfo> entities) {
        return repository.saveAll(entities);
    }

    public List<TmpExDividendsExRightInfo> findByTradingDay(Date tradingDay) {
        return repository.findByTradingDay(tradingDay);
    }

    public Optional<TmpExDividendsExRightInfo> findById(TmpExDividendsExRightInfoId id) {
        return repository.findById(id);
    }

    public void deleteById(TmpExDividendsExRightInfoId id) {
        repository.deleteById(id);
    }
    
	public Optional<TmpExDividendsExRightInfo> findByTradingDayAndStockCode(@Param("tradingDay") Date tradingDay, @Param("stockCode") String stockCode){
		return repository.findByTradingDayAndStockCode(tradingDay, stockCode);
	}
}

