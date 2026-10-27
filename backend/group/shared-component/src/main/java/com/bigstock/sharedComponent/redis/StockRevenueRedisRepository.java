package com.bigstock.sharedComponent.redis;

import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Component;

import com.bigstock.sharedComponent.dto.StockRevenueResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockRevenueRedisRepository {

    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;

    private static final String PREFIX = "REVENUE:MONTHLY:";

    private static final int KEEP_MONTHS = 24;

    private static final ZoneId TAIPEI_ZONE =
            ZoneId.of("Asia/Taipei");

    private static final TypeReference<List<StockRevenueResponse>>
            LIST_TYPE =
            new TypeReference<List<StockRevenueResponse>>() {};

    private static String key(String yearMonth) {
        return PREFIX + YearMonth.parse(yearMonth).toString();
    }

    /**
     * 最近一個已結束月份。
     */
    private static YearMonth latestMonth() {
        return YearMonth.now(TAIPEI_ZONE).minusMonths(1);
    }

    /**
     * 是否在最近 24 個已結束月份內。
     */
    private static boolean inRetentionWindow(String yearMonth) {

        YearMonth target = YearMonth.parse(yearMonth);
        YearMonth latest = latestMonth();
        YearMonth oldest = latest.minusMonths(KEEP_MONTHS - 1);

        return !target.isBefore(oldest)
                && !target.isAfter(latest);
    }

    // ============================================================
    // READ RAW JSON
    // ============================================================

    public String getMonthlyJson(String yearMonth) {

        RBucket<String> bucket = redissonClient.getBucket(
                key(yearMonth),
                StringCodec.INSTANCE
        );

        return bucket.get();
    }

    // ============================================================
    // PARSE JSON
    // ============================================================

    public List<StockRevenueResponse> parseMonthlyJson(
            String yearMonth,
            String json) {

        if (json == null) {
            return Collections.emptyList();
        }

        if (json.trim().isEmpty()) {
            throw new IllegalStateException(
                    "Redis monthly revenue JSON is blank: " + yearMonth
            );
        }

        try {

            List<StockRevenueResponse> list =
                    objectMapper.readValue(json, LIST_TYPE);

            if (list == null) {
                throw new IllegalStateException(
                        "Redis monthly revenue JSON is null: " + yearMonth
                );
            }

            return list;

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Invalid Redis monthly revenue JSON: " + yearMonth,
                    e
            );
        }
    }

    // ============================================================
    // READ MONTHLY LIST
    // ============================================================

    public List<StockRevenueResponse> getMonthlyList(
            String yearMonth) {

        String json = getMonthlyJson(yearMonth);

        return parseMonthlyJson(yearMonth, json);
    }

    // ============================================================
    // WRITE MONTHLY SNAPSHOT
    //
    // 不設定 TTL。
    // 只保留最近 24 個已結束月份。
    // ============================================================

    public void saveMonthlyList(
            String yearMonth,
            List<StockRevenueResponse> list) {

        String normalizedMonth =
                YearMonth.parse(yearMonth).toString();

        /*
         * 舊月份仍可寫入 PostgreSQL，
         * 但不應重新放回 Redis。
         */
        if (!inRetentionWindow(normalizedMonth)) {

            log.info(
                    "[REVENUE] Skip Redis write outside retention window, month={}",
                    normalizedMonth
            );

            cleanupOldMonthlyRevenue();
            return;
        }

        try {

            List<StockRevenueResponse> data =
                    list == null
                            ? Collections.emptyList()
                            : list;

            String json =
                    objectMapper.writeValueAsString(data);

            RBucket<String> bucket = redissonClient.getBucket(
                    key(normalizedMonth),
                    StringCodec.INSTANCE
            );

            /*
             * 單一 Redis SET。
             * 不設定 TTL。
             */
            bucket.set(json);

            cleanupOldMonthlyRevenue();

            log.info(
                    "[REVENUE] Redis updated, month={}, count={}",
                    normalizedMonth,
                    data.size()
            );

        } catch (Exception e) {

            log.error(
                    "[REVENUE] Redis write failed, month={}",
                    normalizedMonth,
                    e
            );

            throw new IllegalStateException(
                    "Redis revenue write failed: " + normalizedMonth,
                    e
            );
        }
    }

    // ============================================================
    // REDIS BACKFILL
    //
    // SETNX:
    // Redis Key 不存在才寫入。
    // ============================================================

    public boolean saveMonthlyListIfAbsent(
            String yearMonth,
            List<StockRevenueResponse> list) {

        String normalizedMonth =
                YearMonth.parse(yearMonth).toString();

        if (!inRetentionWindow(normalizedMonth)) {
            return false;
        }

        if (list == null || list.isEmpty()) {
            return false;
        }

        try {

            String json =
                    objectMapper.writeValueAsString(list);

            RBucket<String> bucket = redissonClient.getBucket(
                    key(normalizedMonth),
                    StringCodec.INSTANCE
            );

            return bucket.trySet(json);

        } catch (Exception e) {

            log.error(
                    "[REVENUE] Redis backfill failed, month={}",
                    normalizedMonth,
                    e
            );

            throw new IllegalStateException(
                    "Redis revenue backfill failed: " + normalizedMonth,
                    e
            );
        }
    }

    // ============================================================
    // CLEANUP
    //
    // 清除最近 24 個已結束月份以外的 Key。
    //
    // 掃描 Redis，不使用 KEYS 指令。
    // ============================================================

    public void cleanupOldMonthlyRevenue() {

        YearMonth latest = latestMonth();

        YearMonth oldestAllowed =
                latest.minusMonths(KEEP_MONTHS - 1);

        List<String> deleteKeys = new ArrayList<>();

        for (String redisKey :
                redissonClient.getKeys()
                        .getKeysByPattern(PREFIX + "*")) {

            String monthStr =
                    redisKey.substring(PREFIX.length());

            try {

                YearMonth month =
                        YearMonth.parse(monthStr);

                if (month.isBefore(oldestAllowed)
                        || month.isAfter(latest)) {

                    deleteKeys.add(redisKey);
                }

            } catch (DateTimeParseException e) {

                log.warn(
                        "[REVENUE] Ignore invalid Redis key={}",
                        redisKey
                );
            }
        }

        if (!deleteKeys.isEmpty()) {

            long deleted = redissonClient.getKeys().delete(
                    deleteKeys.toArray(new String[0])
            );

            log.info(
                    "[REVENUE] Cleanup, oldest={}, latest={}, deleted={}",
                    oldestAllowed,
                    latest,
                    deleted
            );
        }
    }
}