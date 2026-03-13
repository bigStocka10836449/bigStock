package com.bigstock.sharedComponent.redis;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class CacheOperatorService {

    private final RedisTemplate<String, Object> redisTemplate;
    
    public static final String SHORT_LIVED_CACHE = "shortLivedCache";
    
    public static final String MIDDLE_LIVED_CACHE = "middleLivedCache";
    
    public static final String LONG_LIVED_CACHE = "longLivedCache";

    private final Map<String, Duration> slidingTtlMap = Map.of(
            "shortLivedCache", Duration.ofMinutes(120),
            "middleLivedCache", Duration.ofDays(1),
            "longLivedCache", Duration.ofDays(20)
    );

    private final Map<String, Duration> hardTtlMap = Map.of(
            "shortLivedCache", Duration.ofHours(6),
            "middleLivedCache", Duration.ofDays(3),
            "longLivedCache", Duration.ofDays(14)
    );

    public CacheOperatorService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private String buildRedisKey(String cacheName, String key) {
        return cacheName + ":" + key;
    }

    public <T> T get(String cacheName, String key, Class<T> type) {

        String redisKey = buildRedisKey(cacheName, key);

        Object value = redisTemplate.opsForValue().get(redisKey);

        if (value == null) {
            return null;
        }

        touch(cacheName, redisKey);

        return type.cast(value);
    }

    public void put(String cacheName, String key, Object value) {

        String redisKey = buildRedisKey(cacheName, key);

        Duration ttl = slidingTtlMap.getOrDefault(
                cacheName,
                Duration.ofMinutes(30)
        );

        redisTemplate.opsForValue()
                .set(redisKey, value, ttl);
    }

    public void evict(String cacheName, String key) {

        String redisKey = buildRedisKey(cacheName, key);

        redisTemplate.delete(redisKey);
    }

    public void evictBatch(String cacheName, Collection<String> keys) {

        List<String> redisKeys =
                keys.stream()
                        .map(k -> buildRedisKey(cacheName, k))
                        .toList();

        redisTemplate.delete(redisKeys);
    }

    /**
     * 如果cache有被使用到的話自動延長
     * @param cacheName
     * @param redisKey
     */
    private void touch(String cacheName, String redisKey) {

        Duration sliding =
                slidingTtlMap.getOrDefault(
                        cacheName,
                        Duration.ofMinutes(30));

        Duration hard =
                hardTtlMap.getOrDefault(
                        cacheName,
                        sliding.multipliedBy(3));

        Long remain =
                redisTemplate.getExpire(redisKey, TimeUnit.SECONDS);

        if (remain == null || remain <= 0) {
            return;
        }

        if (remain < hard.getSeconds()) {
            redisTemplate.expire(redisKey, sliding);
        }
    }
}