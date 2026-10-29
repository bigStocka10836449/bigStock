package com.bigstock.sharedComponent.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bigstock.sharedComponent.entity.StockQuarterFinancial;

public interface StockQuarterFinancialRepository
		extends JpaRepository<StockQuarterFinancial, StockQuarterFinancial.StockQuarterFinancialId> {

}
