package com.bigstock.sharedComponent.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bigstock.sharedComponent.dto.QuarterlyFinancialResponse;
import com.bigstock.sharedComponent.entity.StockQuarterFinancial;
import com.bigstock.sharedComponent.entity.StockQuarterFinancial.StockQuarterFinancialId;
import com.bigstock.sharedComponent.repository.StockQuarterFinancialRepository;
import com.bigstock.sharedComponent.repository.StockQuarterFinancialUpsertRepository;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockQuarterFinancialService {

    private final StockQuarterFinancialUpsertRepository upsertRepository;
    
    private final StockQuarterFinancialRepository stockQuarterFinancialRepository;

    @Data
    @AllArgsConstructor
    public static class UpsertResult {
        private int inputCount;
        private int uniqCount;
        private int savedCount;
    }

    /**
     * biz 直接丟 list 進來 upsert（不依賴 Redis）
     * - 以 (stockId, market, year, quarter) 去重：只留最新一筆
     * - DB 用 ON CONFLICT 做 upsert
     */
    @Transactional
    public UpsertResult upsertFromNormList(List<QuarterlyFinancialResponse> list) {
        int inputCount = (list == null) ? 0 : list.size();
        if (list == null || list.isEmpty()) return new UpsertResult(inputCount, 0, 0);

        Map<String, QuarterlyFinancialResponse> uniq = new LinkedHashMap<>();
        for (QuarterlyFinancialResponse dto : list) {
            if (dto == null) continue;
            String stockId = safe(dto.getStockId());
            String market = safe(dto.getMarket()).toUpperCase();
            if (stockId.isBlank() || market.isBlank()) continue;

            String k = stockId + "|" + market + "|" + dto.getYear() + "|" + dto.getQuarter();
            uniq.put(k, dto);
        }

        List<QuarterlyFinancialResponse> uniqList = new ArrayList<>(uniq.values());
        int saved = upsertRepository.batchUpsert(uniqList);
        return new UpsertResult(inputCount, uniq.size(), saved);
    }
    
	public Optional<StockQuarterFinancial> findById(String stockId, String market, Integer year, Integer quarter) {
		StockQuarterFinancialId id = new StockQuarterFinancialId();
		id.setMarket(market);
		id.setQuarter(quarter);
		id.setStockId(stockId);
		id.setYear(year);
		return stockQuarterFinancialRepository.findById(id);
	}

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
