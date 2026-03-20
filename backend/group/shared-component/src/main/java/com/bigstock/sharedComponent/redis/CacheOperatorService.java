package com.bigstock.sharedComponent.redis;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.ToDoubleFunction;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Service;

import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CacheOperatorService {

	@Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
	@Autowired
    @Qualifier("rawRedisTemplate")
    private  RedisTemplate<String, byte[]> rawRedisTemplate;

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
    
    
    public <T> List<T> getZSetSeries(
            String cacheName,
            String key,
            Class<T> clazz
    ) {

        String redisKey = buildKey(cacheName, key);

        Set<Object> raw =
                redisTemplate.opsForZSet().range(redisKey, 0, -1);

        if (raw == null || raw.isEmpty()) {
            return Collections.emptyList();
        }

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
	
	public void putCompressedValue(
	        String cacheName,
	        String key,
	        String json
	) {

	    if (json == null) {
	        return;
	    }

	    String redisKey = buildKey(cacheName, key);

	    try {

	        byte[] compressed = gzipCompress(json);

	        rawRedisTemplate.opsForValue().set(
	                redisKey,
	                compressed
	        );

	    } catch (Exception e) {

	        log.warn("putCompressedValue fail key=" + redisKey
	                + " , reason=" + e.getMessage(), e);
	    }
	}

	public byte[] gzipCompress(String data) {

	    try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
	         GZIPOutputStream gzip = new GZIPOutputStream(bos)) {

	        gzip.write(data.getBytes(StandardCharsets.UTF_8));
	        gzip.finish();

	        return bos.toByteArray();

	    } catch (Exception e) {
	        throw new RuntimeException("gzip compress fail", e);
	    }
	}
	
	public String getCompressedValue(
	        String cacheName,
	        String key
	) {

	    String redisKey = buildKey(cacheName, key);

	    try {

	        byte[] compressed =
	                rawRedisTemplate.opsForValue().get(redisKey);

	        if (compressed == null || compressed.length == 0) {
	            return null;
	        }

	        return gzipDecompress(compressed);

	    } catch (Exception e) {

	    	log.warn("getCompressedValue fail key=" + redisKey
	                + " , reason=" + e.getMessage(), e);

	        return null;
	    }
	}
	
	public String gzipDecompress(byte[] compressed) {

	    try (ByteArrayInputStream bis = new ByteArrayInputStream(compressed);
	         GZIPInputStream gzip = new GZIPInputStream(bis);
	         InputStreamReader isr = new InputStreamReader(gzip, StandardCharsets.UTF_8);
	         BufferedReader br = new BufferedReader(isr)) {

	        StringBuilder sb = new StringBuilder();

	        String line;
	        while ((line = br.readLine()) != null) {
	            sb.append(line);
	        }

	        return sb.toString();

	    } catch (Exception e) {
	        throw new RuntimeException("gzip decompress fail", e);
	    }
	}
}