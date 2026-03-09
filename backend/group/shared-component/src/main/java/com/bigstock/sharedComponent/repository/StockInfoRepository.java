package com.bigstock.sharedComponent.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bigstock.sharedComponent.entity.StockInfo;

public interface StockInfoRepository extends JpaRepository<StockInfo, String> {
	@Query(value =" select t.* from bstock.stock_info t where length(t.stock_Code) = 4", nativeQuery =  true)
	List<StockInfo> getAllStockCode();
	
	@Query(value =" select t.stock_Code from bstock.stock_info t where t.stock_Type = :stockType and length(t.stock_Code) = 4", nativeQuery =  true)
	List<String> getStockCodeByStockType(@Param("stockType") String stockType);
	
	List<StockInfo> findByStockType(String stockType);
}
