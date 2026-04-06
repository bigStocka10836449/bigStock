package com.bigstock.biz.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bigstock.biz.dto.IndustryRequest;
import com.bigstock.sharedComponent.entity.StockInfoTagMapping;
import com.bigstock.sharedComponent.service.StockInfoTagMappingService;
import com.bigstock.sharedComponent.service.StockInfoTagService;
import com.bigstock.sharedComponent.service.StockTagCacheService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequiredArgsConstructor
@RequestMapping("admin")
@Slf4j
public class AdminController {

	private final StockInfoTagService stockInfoTagService;
	
	private final StockInfoTagMappingService stockInfoTagMappingService;
	
	private final StockTagCacheService stockTagCacheService;

	@PostMapping("insertStockInfoTags")
	public ResponseEntity<String> insertStockInfoTags(
			@RequestBody IndustryRequest request) {
		   List<StockInfoTagMapping> mappingList = new ArrayList<>();
		    Map<String, Set<String>> stockTagMap = new HashMap<>();

		    for (List<String> row : request.getData()) {

		        String tag = row.get(0).trim().toUpperCase();
		        String tagName = row.get(1).trim();
		        String stockStr = row.get(2);
		        StockInfoTagMapping stockInfoTagMapping = new StockInfoTagMapping();
		        stockInfoTagMapping.setTag(tag);
		        stockInfoTagMapping.setTagName(tagName);
		        // 
		        mappingList.add(stockInfoTagMapping);

		        //
		        String[] stocks = stockStr.split("、");

		        for (String stock : stocks) {
		            String stockCode = stock.trim();

		            stockTagMap
		                .computeIfAbsent(stockCode, k -> new LinkedHashSet<>())
		                .add(tag);
		        }
		        stockTagCacheService.cacheTagStocks(tag, tagName, List.of(stocks));
		    }

		    //
		    stockInfoTagMappingService.bulkUpsert(mappingList);

		    // ✅ 2. bulk upsert stock tags
		    stockTagMap.entrySet().forEach(entry -> {
		    	List<String> list = entry.getValue().stream()
	                       .collect(Collectors.toList());
		    	stockInfoTagService.upsertTags(entry.getKey(), list);
		    	stockTagCacheService.cacheStockTags(entry.getKey(), list);
		    });
		return ResponseEntity.ok().build();
	}
	

}
