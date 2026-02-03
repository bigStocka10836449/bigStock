package com.bigstock.sharedComponent.service;

import com.bigstock.sharedComponent.dto.StockRevenueResponse;
import com.bigstock.sharedComponent.entity.StockRevenue;
import com.bigstock.sharedComponent.entity.StockRevenueId;
import com.bigstock.sharedComponent.redis.StockRevenueRedisRepository;
import com.bigstock.sharedComponent.repository.StockRevenueRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockRevenueService {

    private final StockRevenueRepository repository;
    private final StockRevenueRedisRepository redisRepository;

    @Data
    @AllArgsConstructor
    public static class SyncResult {
        private String yearMonth; // yyyy-MM
        private int redisCount;
        private int savedCount;
    }

    /**
     * 從 Redis 的 REVENUE:MONTHLY:yyyy-MM 讀出 list，然後寫入 DB（saveAll upsert）。
     * - Redis 內容不動（保留）
     * - DB 用複合 PK (revenue_month, market, stock_id) 自動覆蓋更新
     */
    @Transactional
    public SyncResult syncFromRedisToDb(String yearMonth) {
        List<StockRevenueResponse> list = redisRepository.getMonthlyList(yearMonth);
        int redisCount = list.size();

        Map<StockRevenueId, StockRevenue> uniq = new LinkedHashMap<>();
        putAll(uniq, list);

        if (!uniq.isEmpty()) {
            repository.saveAll(uniq.values());
        }

        return new SyncResult(yearMonth, redisCount, uniq.size());
    }

    /**
     * biz 直接丟 list 進來 upsert（不依賴 Redis）
     */
    @Transactional
    public SyncResult upsertFromNormList(
            String yearMonth,
            List<StockRevenueResponse> twse,
            List<StockRevenueResponse> tpex
    ) {
        Map<StockRevenueId, StockRevenue> uniq = new LinkedHashMap<>();
        putAll(uniq, twse);
        putAll(uniq, tpex);

        if (!uniq.isEmpty()) {
            repository.saveAll(uniq.values());
        }

        int twseCount = (twse == null) ? 0 : twse.size();
        int tpexCount = (tpex == null) ? 0 : tpex.size();

        return new SyncResult(yearMonth, twseCount + tpexCount, uniq.size());
    }

    private void putAll(Map<StockRevenueId, StockRevenue> uniq, List<StockRevenueResponse> list) {
        if (list == null || list.isEmpty()) return;

        for (StockRevenueResponse dto : list) {
            if (dto == null) continue;

            String stockId = safe(dto.getStockId());
            String market = safe(dto.getMarket()).toUpperCase();

            if (stockId.isBlank() || market.isBlank()) continue;

            // dto.date = yyyy-MM → 存成 yyyy-MM-01
            LocalDate revenueMonth = parseYearMonthToFirstDay(dto.getDate());

            StockRevenueId id = new StockRevenueId(revenueMonth, market, stockId);
            StockRevenue e = new StockRevenue(id);

            e.setStockName(safe(dto.getStockName()));
            e.setRevenue(dto.getRevenue());

            // ✅ 同 key 覆蓋：只留最新
            uniq.put(id, e);
        }
    }

    private static LocalDate parseYearMonthToFirstDay(String yyyyMm) {
        String s = safe(yyyyMm);
        if (s.isBlank()) {
            throw new IllegalArgumentException("date(yearMonth) is blank");
        }
        // 允許 yyyy-MM 或 yyyyMM
        if (s.contains("-")) {
            return LocalDate.parse(s + "-01");
        }
        if (s.length() == 6) {
            return LocalDate.parse(s.substring(0, 4) + "-" + s.substring(4, 6) + "-01");
        }
        // fallback：如果有人傳 yyyy-MM-dd 也能吃
        if (s.length() == 10 && s.contains("-")) {
            LocalDate d = LocalDate.parse(s);
            return LocalDate.of(d.getYear(), d.getMonth(), 1);
        }
        throw new IllegalArgumentException("unsupported yearMonth format: " + s);
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
