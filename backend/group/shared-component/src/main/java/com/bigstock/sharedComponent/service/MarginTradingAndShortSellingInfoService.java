package com.bigstock.sharedComponent.service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bigstock.sharedComponent.entity.MarginTradingAndShortSellingInfo;
import com.bigstock.sharedComponent.repository.MarginTradingAndShortSellingInfoRepository;

@Service
public class MarginTradingAndShortSellingInfoService {

    private final MarginTradingAndShortSellingInfoRepository repository;

    @Autowired
    public MarginTradingAndShortSellingInfoService(MarginTradingAndShortSellingInfoRepository repository) {
        this.repository = repository;
    }

    public List<MarginTradingAndShortSellingInfo> getAllRecords() {
        return repository.findAll();
    }

    public Optional<MarginTradingAndShortSellingInfo> getRecordById(MarginTradingAndShortSellingInfo.MarginTradingAndShortSellingInfoId id) {
        return repository.findById(id);
    }

    public MarginTradingAndShortSellingInfo saveRecord(MarginTradingAndShortSellingInfo record) {
        return repository.save(record);
    }

    public void deleteRecordById(MarginTradingAndShortSellingInfo.MarginTradingAndShortSellingInfoId id) {
        repository.deleteById(id);
    }
    
    public List<MarginTradingAndShortSellingInfo> saveAll(List<MarginTradingAndShortSellingInfo> marginTradingAndShortSellingInfos){
    	return repository.saveAll(marginTradingAndShortSellingInfos);
    }
    
    public List<MarginTradingAndShortSellingInfo> findMarginTradingAndShortSellingInfoByDateRange(String stockCode, Date firstDate, Date secondDate){
    	return repository.findMarginTradingAndShortSellingInfoByDateRange(stockCode, firstDate, secondDate);
    }
}
