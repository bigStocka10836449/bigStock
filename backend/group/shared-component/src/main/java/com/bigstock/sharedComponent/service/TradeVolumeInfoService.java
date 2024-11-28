package com.bigstock.sharedComponent.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.bigstock.sharedComponent.entity.TradeVolumeInfo;
import com.bigstock.sharedComponent.entity.TradeVolumeInfo.TradeVolumeInfoId;
import com.bigstock.sharedComponent.repository.TradeVolumeInfoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TradeVolumeInfoService {

    private final TradeVolumeInfoRepository repository;

    public List<TradeVolumeInfo> findAll() {
        return repository.findAll();
    }

    public Optional<TradeVolumeInfo> findById(TradeVolumeInfoId id) {
        return repository.findById(id);
    }

    public TradeVolumeInfo save(TradeVolumeInfo tradeVolumeInfo) {
        return repository.save(tradeVolumeInfo);
    }

    public void deleteById(TradeVolumeInfoId id) {
        repository.deleteById(id);
    }
    
    public List<TradeVolumeInfo> saveAll(List<TradeVolumeInfo> tradeVolumeInfos) {
        return repository.saveAll(tradeVolumeInfos);
    }
}
