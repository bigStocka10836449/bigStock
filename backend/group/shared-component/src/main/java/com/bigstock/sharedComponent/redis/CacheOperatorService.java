package com.bigstock.sharedComponent.redis;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.ToDoubleFunction;

import org.apache.poi.ss.formula.functions.T;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Service;

@Service
public class CacheOperatorService {

    private final RedisTemplate<String, Object> redisTemplate;

    public static final int DEFAULT_SERIES_MAX_SIZE = 600;

    private final Map<String, Duration> slidingTtlMap = Map.of(
            "shortLivedCache", Duration.ofMinutes(120),
            "middleLivedCache", Duration.ofDays(1),
            "longLivedCache", Duration.ofDays(7),
            "ultraLongLivedCache", Duration.ofDays(999999)
    );

    private final Map<String, Duration> hardTtlMap = Map.of(
            "shortLivedCache", Duration.ofHours(6),
            "middleLivedCache", Duration.ofDays(3),
            "longLivedCache", Duration.ofDays(14)
    );

    public CacheOperatorService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private String buildKey(String cacheName, String key) {
        return "cache:" + cacheName + ":" + key;
    }

    private void touch(String redisKey, String cacheName) {
        Duration sliding = slidingTtlMap.get(cacheName);
        if (sliding != null) {
            redisTemplate.expire(redisKey, sliding);
        }
    }

    // =========================
    // SIMPLE CACHE
    // =========================

    public void putSimple(String cacheName, String key, Object value) {

        String redisKey = buildKey(cacheName, key);

        redisTemplate.opsForValue().set(redisKey, value);

        touch(redisKey, cacheName);
    }

    public <T> T getSimple(String cacheName, String key, Class<T> clazz) {

        String redisKey = buildKey(cacheName, key);

        Object v = redisTemplate.opsForValue().get(redisKey);

        if (v != null) {
            touch(redisKey, cacheName);
            return clazz.cast(v);
        }

        return null;
    }

    // =========================
    // LIST SERIES CACHE
    // =========================

    public void appendListSeries(
            String cacheName,
            String key,
            Object value,
            int maxSize
    ) {

        String redisKey = buildKey(cacheName, key);

        redisTemplate.opsForList().rightPush(redisKey, value);

        redisTemplate.opsForList().trim(
                redisKey,
                -maxSize,
                -1
        );

        touch(redisKey, cacheName);
    }

    public <T> List<T> getListSeries(
            String cacheName,
            String key,
            Class<T> clazz
    ) {

        String redisKey = buildKey(cacheName, key);

        List<Object> raw =
                redisTemplate.opsForList().range(redisKey, 0, -1);

        if (raw == null) return Collections.emptyList();

        touch(redisKey, cacheName);

        return raw.stream()
                .map(clazz::cast)
                .toList();
    }

    // =========================
    // ZSET SERIES CACHE
    // =========================

    public void appendZSetSeries(
            String cacheName,
            String key,
            Object value,
            double score,
            int maxSize
    ) {

        String redisKey = buildKey(cacheName, key);

        redisTemplate.opsForZSet()
                .add(redisKey, value, score);

        Long size =
                redisTemplate.opsForZSet().size(redisKey);

        if (size != null && size > maxSize) {

            redisTemplate.opsForZSet()
                    .removeRange(redisKey, 0, size - maxSize - 1);
        }

        touch(redisKey, cacheName);
    }

    public <T> List<T> getLatestZSetSeries(
            String cacheName,
            String key,
            Class<T> clazz,
            int limit
    ) {

        String redisKey = buildKey(cacheName, key);

        Set<Object> raw =
                redisTemplate.opsForZSet()
                        .reverseRange(redisKey, 0, limit - 1);

        if (raw == null) return Collections.emptyList();

        touch(redisKey, cacheName);

        return raw.stream()
                .map(clazz::cast)
                .toList();
    }

    public void upsertZSetSeries(
            String cacheName,
            String key,
            Object value,
            double score,
            int maxSize
    ) {

        String redisKey = buildKey(cacheName, key);

        // remove old record with same score (same trading day)
        redisTemplate.opsForZSet()
                .removeRangeByScore(redisKey, score, score);

        // add new one
        redisTemplate.opsForZSet()
                .add(redisKey, value, score);

        // maintain max size
        Long size = redisTemplate.opsForZSet().size(redisKey);

        if (size != null && size > maxSize) {
            redisTemplate.opsForZSet()
                    .removeRange(redisKey, 0, size - maxSize - 1);
        }

        touch(redisKey, cacheName);
    } 
    
	public <T> void batchUpsertZSetSeries(String cacheName, String key, List<T> values,
			ToDoubleFunction<T> scoreExtractor, int maxSize) {

		if (values == null || values.isEmpty()) {
			return;
		}

		String redisKey = buildKey(cacheName, key);

		redisTemplate.executePipelined((RedisCallback<Object>) connection -> {

			RedisSerializer<String> keySerializer = (RedisSerializer<String>) redisTemplate.getKeySerializer();

			@SuppressWarnings("unchecked")
			RedisSerializer<Object> valueSerializer = (RedisSerializer<Object>) redisTemplate.getValueSerializer();

			byte[] rawKey = keySerializer.serialize(redisKey);

			for (T v : values) {
				double score = scoreExtractor.applyAsDouble(v);

				// remove old record with same score
				connection.zRemRangeByScore(rawKey, score, score);

				// add new record
				byte[] rawValue = valueSerializer.serialize(v);
				connection.zAdd(rawKey, score, rawValue);
			}

			return null;
		});

		Long size = redisTemplate.opsForZSet().size(redisKey);

		if (size != null && size > maxSize) {
			redisTemplate.opsForZSet().removeRange(redisKey, 0, size - maxSize - 1);
		}

		touch(redisKey, cacheName);
	}
}