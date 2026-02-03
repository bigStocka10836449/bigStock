package com.bigstock.sharedComponent.redis;

import com.bigstock.sharedComponent.dto.StockRevenueResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockRevenueRedisRepository {

    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;

    private static String key(String yearMonth) {
        return "REVENUE:MONTHLY:" + yearMonth; // e.g. 2025-11
    }

    public String getMonthlyJson(String yearMonth) {
        RBucket<String> bucket = redissonClient.getBucket(key(yearMonth), StringCodec.INSTANCE);
        return bucket.get();
    }

    public List<StockRevenueResponse> getMonthlyList(String yearMonth) {
        String json = getMonthlyJson(yearMonth);
        if (json == null || json.isBlank() || "[]".equals(json.trim())) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<StockRevenueResponse>>() {});
        } catch (Exception e) {
            log.warn("parse monthly revenue json failed, yearMonth={}, err={}", yearMonth, e.getMessage(), e);
            return Collections.emptyList();
        }
    }
}
