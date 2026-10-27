package com.bigstock.sharedComponent.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.bigstock.sharedComponent.dto.StockRevenueResponse;
import com.bigstock.sharedComponent.entity.StockRevenue;
import com.bigstock.sharedComponent.entity.StockRevenueId;
import com.bigstock.sharedComponent.redis.StockRevenueRedisRepository;
import com.bigstock.sharedComponent.repository.StockRevenueRepository;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockRevenueService {

    private final StockRevenueRepository repository;

    private final StockRevenueRedisRepository redisRepository;

    // ============================================================
    // RESULT
    // ============================================================

    @Data
    @AllArgsConstructor
    public static class SyncResult {

        private String yearMonth;

        private int redisCount;

        private int savedCount;
    }

    // ============================================================
    // REDIS ACCESS
    // ============================================================

    public String getMonthlyJson(String yearMonth) {

        return redisRepository.getMonthlyJson(yearMonth);
    }

    public List<StockRevenueResponse> getMonthlyList(
            String yearMonth) {

        return redisRepository.getMonthlyList(yearMonth);
    }

    /**
     * 直接解析已讀取的 JSON。
     *
     * 避免同一月份重複呼叫 Redis GET。
     */
    public List<StockRevenueResponse> parseMonthlyJson(
            String yearMonth,
            String json) {

        return redisRepository.parseMonthlyJson(
                yearMonth,
                json
        );
    }

    /**
     * Redis Miss 後，使用 SETNX 回填。
     */
    public boolean saveMonthlyListIfAbsent(
            String yearMonth,
            List<StockRevenueResponse> list) {

        if (list == null || list.isEmpty()) {
            return false;
        }

        return redisRepository.saveMonthlyListIfAbsent(
                yearMonth,
                list
        );
    }

    // ============================================================
    // POSTGRESQL READ
    // ============================================================

    @Transactional(readOnly = true)
    public List<StockRevenue> findByRevenueMonths(
            List<LocalDate> months) {

        if (months == null || months.isEmpty()) {
            return Collections.emptyList();
        }

        return repository.findByRevenueMonths(months);
    }

    /**
     * 批次查詢 PostgreSQL 缺少的月份。
     *
     * Return:
     * yyyy-MM -> List<StockRevenueResponse>
     */
    @Transactional(readOnly = true)
    public Map<String, List<StockRevenueResponse>>
            findMonthlyRevenueFromDb(
                    List<String> missingMonths) {

        if (missingMonths == null || missingMonths.isEmpty()) {
            return Collections.emptyMap();
        }

        List<LocalDate> dates =
                missingMonths.stream()
                        .map(month ->
                                YearMonth.parse(month).atDay(1)
                        )
                        .collect(Collectors.toList());

        /*
         * PostgreSQL 一次查詢。
         */
        List<StockRevenue> entities =
                repository.findByRevenueMonths(dates);

        Map<String, List<StockRevenueResponse>> result =
                new HashMap<>();

        for (StockRevenue entity : entities) {

            if (entity == null || entity.getId() == null) {
                continue;
            }

            StockRevenueId id = entity.getId();

            String yearMonth =
                    YearMonth.from(
                            id.getRevenueMonth()
                    ).toString();

            StockRevenueResponse dto =
                    toResponse(entity);

            result.computeIfAbsent(
                    yearMonth,
                    k -> new ArrayList<>()
            ).add(dto);
        }

        return result;
    }

    // ============================================================
    // REDIS -> DB
    // ============================================================

    /**
     * 從 Redis 讀取指定月份並寫入 PostgreSQL。
     *
     * Redis 原始資料不變。
     */
    @Transactional
    public SyncResult syncFromRedisToDb(
            String yearMonth) {

        String normalizedMonth =
                YearMonth.parse(yearMonth).toString();

        List<StockRevenueResponse> list =
                redisRepository.getMonthlyList(
                        normalizedMonth
                );

        int redisCount = list.size();

        Map<StockRevenueId, StockRevenue> uniq =
                new LinkedHashMap<>();

        putAll(uniq, list);

        if (!uniq.isEmpty()) {

            repository.saveAll(uniq.values());
        }

        return new SyncResult(
                normalizedMonth,
                redisCount,
                uniq.size()
        );
    }

    // ============================================================
    // OFFICIAL DATA -> DB + REDIS
    // ============================================================

    /**
     * 接收 TWSE、TPEX 正規化營收資料。
     *
     * 1. 合併、去重
     * 2. PostgreSQL Upsert
     * 3. PostgreSQL Commit
     * 4. Redis Snapshot 覆蓋
     *
     * 不設定 Redis TTL。
     */
    @Transactional
    public SyncResult upsertFromNormList(
            String yearMonth,
            List<StockRevenueResponse> twse,
            List<StockRevenueResponse> tpex) {

        String normalizedMonth =
                YearMonth.parse(yearMonth).toString();

        /*
         * 不允許某個市場抓取失敗時，
         * 將整個月 Redis 覆蓋成不完整資料。
         */
        if (twse == null || tpex == null) {

            throw new IllegalArgumentException(
                    "TWSE and TPEX revenue lists must not be null"
            );
        }

        Map<StockRevenueId, StockRevenue> uniq =
                new LinkedHashMap<>();

        putAll(uniq, twse);
        putAll(uniq, tpex);

        /*
         * 不用空資料覆蓋已有的月營收 Snapshot。
         */
        if (uniq.isEmpty()) {

            log.warn(
                    "[REVENUE] Skip empty monthly snapshot, month={}",
                    normalizedMonth
            );

            return new SyncResult(
                    normalizedMonth,
                    0,
                    0
            );
        }

        /*
         * 確認所有資料都是指定月份。
         */
        LocalDate expectedDate =
                YearMonth.parse(normalizedMonth).atDay(1);

        for (StockRevenueId id : uniq.keySet()) {

            if (!expectedDate.equals(id.getRevenueMonth())) {

                throw new IllegalArgumentException(
                        "Revenue month mismatch, expected="
                                + normalizedMonth
                                + ", actual="
                                + id.getRevenueMonth()
                );
            }
        }

        /*
         * PostgreSQL Upsert。
         */
        repository.saveAll(uniq.values());

        /*
         * 建立 Redis Snapshot。
         *
         * 從去重後的 Entity 轉回 DTO，
         * 確保兩邊使用相同的資料。
         */
        final List<StockRevenueResponse> redisList =
                new ArrayList<>(uniq.size());

        for (StockRevenue entity : uniq.values()) {
            redisList.add(toResponse(entity));
        }

        /*
         * PostgreSQL Commit 成功後更新 Redis。
         */
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {

                    @Override
                    public void afterCommit() {

                        redisRepository.saveMonthlyList(
                                normalizedMonth,
                                redisList
                        );

                        log.info(
                                "[REVENUE] DB committed, Redis updated, month={}, count={}",
                                normalizedMonth,
                                redisList.size()
                        );
                    }
                }
        );

        return new SyncResult(
                normalizedMonth,
                redisList.size(),
                uniq.size()
        );
    }

    // ============================================================
    // ENTITY -> RESPONSE
    // ============================================================

    private StockRevenueResponse toResponse(
            StockRevenue entity) {

        StockRevenueId id = entity.getId();

        StockRevenueResponse dto =
                new StockRevenueResponse();

        dto.setStockId(id.getStockId());
        dto.setStockName(entity.getStockName());
        dto.setMarket(id.getMarket());

        dto.setDate(
                YearMonth.from(
                        id.getRevenueMonth()
                ).toString()
        );

        dto.setRevenue(entity.getRevenue());

        return dto;
    }

    // ============================================================
    // DEDUPLICATION
    // ============================================================

    private void putAll(
            Map<StockRevenueId, StockRevenue> uniq,
            List<StockRevenueResponse> list) {

        if (list == null || list.isEmpty()) {
            return;
        }

        for (StockRevenueResponse dto : list) {

            if (dto == null) {
                continue;
            }

            String stockId =
                    safe(dto.getStockId());

            String market =
                    safe(dto.getMarket())
                            .toUpperCase(Locale.ROOT);

            if (stockId.isEmpty() || market.isEmpty()) {
                continue;
            }

            LocalDate revenueMonth =
                    parseYearMonthToFirstDay(
                            dto.getDate()
                    );

            StockRevenueId id = new StockRevenueId(
                    revenueMonth,
                    market,
                    stockId
            );

            StockRevenue entity =
                    new StockRevenue(id);

            entity.setStockName(
                    safe(dto.getStockName())
            );

            entity.setRevenue(
                    dto.getRevenue()
            );

            /*
             * 相同複合 PK 保留最後一筆。
             */
            uniq.put(id, entity);
        }
    }

    // ============================================================
    // YEAR MONTH PARSER
    //
    // 支援：
    // yyyy-MM
    // yyyyMM
    // yyyy-MM-dd
    // ============================================================

    private static LocalDate parseYearMonthToFirstDay(
            String value) {

        String s = safe(value);

        if (s.isEmpty()) {

            throw new IllegalArgumentException(
                    "Revenue date is blank"
            );
        }

        if (s.matches("\\d{4}-\\d{2}")) {

            return YearMonth.parse(s).atDay(1);
        }

        if (s.matches("\\d{6}")) {

            String normalized =
                    s.substring(0, 4)
                            + "-"
                            + s.substring(4, 6);

            return YearMonth.parse(normalized).atDay(1);
        }

        if (s.matches("\\d{4}-\\d{2}-\\d{2}")) {

            LocalDate date = LocalDate.parse(s);

            return date.withDayOfMonth(1);
        }

        throw new IllegalArgumentException(
                "Unsupported revenue date format: " + s
        );
    }

    private static String safe(String value) {

        return value == null
                ? ""
                : value.trim();
    }
}