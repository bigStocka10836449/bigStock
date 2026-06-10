package com.bigstock.sharedComponent.service;

import java.time.Duration;
import java.util.Optional;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.bigstock.sharedComponent.dto.SecuritiesFirmsRankResult;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SecuritiesFirmsRankRedisService {

    private static final String RANK_KEY_PREFIX = "sfdo:rank";
    private static final String READY_KEY_PREFIX = "sfdo:rank:ready";
    private static final String LATEST_END_DATE_KEY = "sfdo:rank:latest-end-date";

    /**
     * 固定區間排行快取保留 14 天。
     */
    private static final Duration RANK_TTL = Duration.ofDays(14);

    private static final Duration READY_TTL = Duration.ofDays(14);

    private final StringRedisTemplate stringRedisTemplate;

    private final ObjectMapper objectMapper;

    public String buildFixedRankKey(String stockCode, Integer rangeDays, String endDate) {
        return RANK_KEY_PREFIX + ":" + stockCode + ":" + rangeDays + ":" + endDate;
    }

    public String buildReadyKey(String endDate) {
        return READY_KEY_PREFIX + ":" + endDate;
    }

    public Optional<SecuritiesFirmsRankResult> getFixedRank(String stockCode, Integer rangeDays, String endDate) {
        try {
            String key = buildFixedRankKey(stockCode, rangeDays, endDate);
            String json = stringRedisTemplate.opsForValue().get(key);

            if (json == null || json.isBlank()) {
                return Optional.empty();
            }

            SecuritiesFirmsRankResult result = objectMapper.readValue(json, SecuritiesFirmsRankResult.class);
            result.setSource("REDIS");
            return Optional.of(result);

        } catch (Exception e) {
            log.error("getFixedRank failed. stockCode={}, rangeDays={}, endDate={}",
                    stockCode, rangeDays, endDate, e);
            return Optional.empty();
        }
    }

    public void putFixedRank(String stockCode, Integer rangeDays, String endDate, SecuritiesFirmsRankResult result) {
        try {
            String key = buildFixedRankKey(stockCode, rangeDays, endDate);
            String json = objectMapper.writeValueAsString(result);
            stringRedisTemplate.opsForValue().set(key, json, RANK_TTL);

        } catch (Exception e) {
            log.error("putFixedRank failed. stockCode={}, rangeDays={}, endDate={}",
                    stockCode, rangeDays, endDate, e);
            throw new RuntimeException(e);
        }
    }

    public void markDailyReady(String endDate) {
        stringRedisTemplate.opsForValue().set(buildReadyKey(endDate), "DONE", READY_TTL);
        stringRedisTemplate.opsForValue().set(LATEST_END_DATE_KEY, endDate, READY_TTL);
    }

    public boolean isDailyReady(String endDate) {
        String value = stringRedisTemplate.opsForValue().get(buildReadyKey(endDate));
        return "DONE".equals(value);
    }

    public Optional<String> getLatestEndDate() {
        return Optional.ofNullable(stringRedisTemplate.opsForValue().get(LATEST_END_DATE_KEY));
    }
}