package com.bigstock.sharedComponent.repository;

import com.bigstock.sharedComponent.entity.StockThreeInstitutionalTrading;
import com.bigstock.sharedComponent.entity.StockThreeInstitutionalTradingId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockThreeInstitutionalTradingRepository
        extends JpaRepository<StockThreeInstitutionalTrading, StockThreeInstitutionalTradingId> {
}