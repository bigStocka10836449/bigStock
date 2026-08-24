package com.bigstock.sharedComponent.redis;


import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.bigstock.sharedComponent.dto.StockTrendCache;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockTrendRedisService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    
    private final CacheOperatorService cacheOperatorService;

    private static final String KEY_PREFIX = "stock:trend:";

    public void saveBatch(List<StockTrendCache> caches) {

        if (CollectionUtils.isEmpty(caches)) {
            return;
        }

        Map<String, String> redisData = new LinkedHashMap<>();

        for (StockTrendCache cache : caches) {

            try {
                String key = KEY_PREFIX + cache.getStockCode();
                String value = objectMapper.writeValueAsString(cache);

                redisData.put(key, value);

            } catch (Exception e) {
                log.error("Failed to serialize stock trend cache, stockCode={}",
                        cache.getStockCode(), e);
            }
        }
        cacheOperatorService.putHashPipelined(redisData);
    }

    public StockTrendCache get(String stockCode) {

        String key = KEY_PREFIX + stockCode;
        String value = redisTemplate.opsForValue().get(key);

        if (value == null) {
            return null;
        }

        try {
            return objectMapper.readValue(value, StockTrendCache.class);
        } catch (Exception e) {
            log.error("Failed to deserialize stock trend cache, stockCode={}", stockCode, e);
            return null;
        }
    }
}
