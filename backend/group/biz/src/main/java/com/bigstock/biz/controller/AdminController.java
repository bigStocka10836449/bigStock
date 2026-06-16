package com.bigstock.biz.controller;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
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
import org.springframework.web.client.RestClientException;

import com.bigstock.biz.dto.IndustryRequest;
import com.bigstock.sharedComponent.entity.StockDayPrice;
import com.bigstock.sharedComponent.entity.StockInfo;
import com.bigstock.sharedComponent.entity.StockInfoTagMapping;
import com.bigstock.sharedComponent.entity.TradeVolumeInfo;
import com.bigstock.sharedComponent.service.StockInfoService;
import com.bigstock.sharedComponent.service.StockInfoTagMappingService;
import com.bigstock.sharedComponent.service.StockInfoTagService;
import com.bigstock.sharedComponent.service.StockTagCacheService;
import com.bigstock.sharedComponent.utils.ChromeDriverUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

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
	
	private final StockInfoService stockInfoService;

	@PostMapping("insertStockInfoTags")
	public ResponseEntity<String> insertStockInfoTags(@RequestBody IndustryRequest request) {
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

				stockTagMap.computeIfAbsent(stockCode, k -> new LinkedHashSet<>()).add(tag);
			}
			stockTagCacheService.cacheTagStocks(tag, tagName, List.of(stocks));
		}

		//
		stockInfoTagMappingService.bulkUpsert(mappingList);

		// ✅ 2. bulk upsert stock tags
		stockTagMap.entrySet().forEach(entry -> {
			List<String> list = entry.getValue().stream().collect(Collectors.toList());
			stockInfoTagService.upsertTags(entry.getKey(), list);
			stockTagCacheService.cacheStockTags(entry.getKey(), list);
		});
		return ResponseEntity.ok().build();
	}

	@PostMapping("manualGrapStockDayPrice")
	public ResponseEntity<String> manualGrapStockDayPrice() throws JsonMappingException, JsonProcessingException, RestClientException, InterruptedException, URISyntaxException {
		List<StockDayPrice> stockTpexEmergingStockPrices = ChromeDriverUtils.graspTpexEmergingStockDayPrice();
		List<StockInfo> allStockInfos = stockInfoService.getAllStockInfo().stream().filter(data -> {
			return !data.getStockCode().matches(".*[a-zA-Z].*");
		}).toList();
		Map<String, List<StockInfo>> allStockInfoMap = allStockInfos.stream()
				.collect(Collectors.groupingBy(StockInfo::getStockCode));
		List<StockDayPrice> stockTpexDayPrices = ChromeDriverUtils
				.graspTpexDayPrice("https://www.tpex.org.tw/openapi/v1/tpex_mainboard_quotes");

////		   List<StockDayPrice> singleStockDayPrices = entry.getValue();

		List<TradeVolumeInfo> stockTpexTradeVolumeInfos = ChromeDriverUtils
				.graspTpexTtradeVolume("https://www.tpex.org.tw/openapi/v1/tpex_volume_rank");
		Map<String, TradeVolumeInfo> stockTpexTradeVolumeInfosMap = stockTpexTradeVolumeInfos.stream()
				.collect(Collectors.toMap(TradeVolumeInfo::getStockCode, tradeVolumeInfo -> tradeVolumeInfo));
		Date tradeDate = stockTpexDayPrices.stream().findFirst().get().getTradingDay();
		List<StockDayPrice> stockTwseDayPrices = ChromeDriverUtils
				.graspTwseDayPrice("https://openapi.twse.com.tw/v1/exchangeReport/STOCK_DAY_ALL", tradeDate);
		List<TradeVolumeInfo> twseTradeVolumeInfos = stockTwseDayPrices.stream().map(stockDayPrice -> {
			TradeVolumeInfo tradeVolumeInfo = new TradeVolumeInfo();
			tradeVolumeInfo.setTradingDay(stockDayPrice.getTradingDay());
			tradeVolumeInfo.setStockCode(stockDayPrice.getStockCode());
			tradeVolumeInfo.setTradeVolume(stockDayPrice.getTradingVolume());
			return tradeVolumeInfo;
		}).toList();
		return ResponseEntity.ok().build();
	}

}
