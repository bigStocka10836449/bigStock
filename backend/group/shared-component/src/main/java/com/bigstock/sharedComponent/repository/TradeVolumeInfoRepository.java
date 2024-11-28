package com.bigstock.sharedComponent.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bigstock.sharedComponent.entity.TradeVolumeInfo;

public interface TradeVolumeInfoRepository extends JpaRepository<TradeVolumeInfo, TradeVolumeInfo.TradeVolumeInfoId> {
}
