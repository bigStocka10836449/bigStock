package com.bigstock.sharedComponent.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bigstock.sharedComponent.entity.StockWeekPriceRank;

public interface StockWeekPriceRepositoryRank extends JpaRepository<StockWeekPriceRank, StockWeekPriceRank.StockWeekPriceRankId>{

}
