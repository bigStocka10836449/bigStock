package com.bigstock.sharedComponent.redis;

import com.bigstock.sharedComponent.dto.ThreeInstitutionalTradingResponse;
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
public class ThreeInstitutionalTradingRedisRepository {

    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;

    public String getNormJson(String marketLower, String yyyyMMdd) {
        String key = "threeinsti:" + marketLower + ":" + yyyyMMdd + ":norm";
        RBucket<String> bucket = redissonClient.getBucket(key, StringCodec.INSTANCE);
        return bucket.get();
    }

    public List<ThreeInstitutionalTradingResponse> getNormList(String marketLower, String yyyyMMdd) {
        String json = getNormJson(marketLower, yyyyMMdd);
        if (json == null || json.isBlank() || "[]".equals(json.trim())) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<ThreeInstitutionalTradingResponse>>() {});
        } catch (Exception e) {
            log.warn("parse norm json failed, market={}, date={}, err={}", marketLower, yyyyMMdd, e.getMessage(), e);
            return Collections.emptyList();
        }
    }
}