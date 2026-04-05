package com.bigstock.sharedComponent.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bigstock.sharedComponent.entity.StockInfoTagMapping;

public interface StockInfoTagMappingRepository extends JpaRepository<StockInfoTagMapping, String> {

	Optional<StockInfoTagMapping> findByTag(String tag);
}