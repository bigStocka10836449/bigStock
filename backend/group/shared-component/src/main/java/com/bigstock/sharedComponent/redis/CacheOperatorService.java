package com.bigstock.sharedComponent.redis;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CacheOperatorService {

	private static final int PIPELINE_CHUNK = 200;
	private static final int GZIP_BUFFER_SIZE = 8192;

	@Autowired
	private RedisTemplate<String, Object> redisTemplate;

	@Autowired
	@Qualifier("rawRedisTemplate")
	private RedisTemplate<String, byte[]> rawRedisTemplate;

	public static final int DEFAULT_SERIES_MAX_SIZE = 600;

	private final Map<String, Duration> slidingTtlMap = Map.of("shortLivedCache", Duration.ofMinutes(120),
			"middleLivedCache", Duration.ofDays(1), "longLivedCache", Duration.ofDays(7), "ultraLongLivedCache",
			Duration.ofDays(999999));

	private final Map<String, Duration> hardTtlMap = Map.of("shortLivedCache", Duration.ofHours(6), "middleLivedCache",
			Duration.ofDays(3), "longLivedCache", Duration.ofDays(14));

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
	// LIST SERIES CACHE
	// =========================

	public void appendListSeries(String cacheName, String key, Object value, int maxSize) {

		String redisKey = buildKey(cacheName, key);

		redisTemplate.opsForList().rightPush(redisKey, value);

		redisTemplate.opsForList().trim(redisKey, -maxSize, -1);

		touch(redisKey, cacheName);
	}

	public <T> List<T> getListSeries(String cacheName, String key, Class<T> clazz) {

		String redisKey = buildKey(cacheName, key);

		List<Object> raw = redisTemplate.opsForList().range(redisKey, 0, -1);

		if (raw == null)
			return Collections.emptyList();

		touch(redisKey, cacheName);

		return raw.stream().map(clazz::cast).toList();
	}

	public <T> List<T> getZSetSeries(String cacheName, String key, Class<T> clazz) {

		String redisKey = buildKey(cacheName, key);

		Set<Object> raw = redisTemplate.opsForZSet().range(redisKey, 0, -1);

		if (raw == null || raw.isEmpty()) {
			return Collections.emptyList();
		}

		touch(redisKey, cacheName);

		return raw.stream().map(clazz::cast).toList();
	}
	// =========================
	// ZSET SERIES CACHE
	// =========================

	public void appendZSetSeries(String cacheName, String key, Object value, double score, int maxSize) {

		String redisKey = buildKey(cacheName, key);

		redisTemplate.opsForZSet().add(redisKey, value, score);

		Long size = redisTemplate.opsForZSet().size(redisKey);

		if (size != null && size > maxSize) {

			redisTemplate.opsForZSet().removeRange(redisKey, 0, size - maxSize - 1);
		}

		touch(redisKey, cacheName);
	}

	public <T> List<T> getLatestZSetSeries(String cacheName, String key, Class<T> clazz, int limit) {

		String redisKey = buildKey(cacheName, key);

		Set<Object> raw = redisTemplate.opsForZSet().reverseRange(redisKey, 0, limit - 1);

		if (raw == null)
			return Collections.emptyList();

		touch(redisKey, cacheName);

		return raw.stream().map(clazz::cast).toList();
	}

	public void upsertZSetSeries(String cacheName, String key, Object value, double score, int maxSize) {

		String redisKey = buildKey(cacheName, key);

		// remove old record with same score (same trading day)
		redisTemplate.opsForZSet().removeRangeByScore(redisKey, score, score);

		// add new one
		redisTemplate.opsForZSet().add(redisKey, value, score);

		// maintain max size
		Long size = redisTemplate.opsForZSet().size(redisKey);

		if (size != null && size > maxSize) {
			redisTemplate.opsForZSet().removeRange(redisKey, 0, size - maxSize - 1);
		}

		touch(redisKey, cacheName);
	}

	public <T> void upsertCompressedZSetSeries(String cacheName, String key, T value, double score, int maxSize) {

		String redisKey = buildKey(cacheName, key);

		ObjectMapper objectMapper = new ObjectMapper();
		try {

			byte[] json = objectMapper.writeValueAsBytes(value);
			byte[] compressed = gzipCompress(json);

			rawRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {

				RedisSerializer<String> keySer = rawRedisTemplate.getStringSerializer();

				byte[] rawKey = keySer.serialize(redisKey);

				// remove duplicate timestamp candle
				connection.zRemRangeByScore(rawKey, score, score);

				// insert new candle
				connection.zAdd(rawKey, score, compressed);

				return null;
			});

		} catch (Exception e) {
			throw new RuntimeException("Redis compressed upsert failed", e);
		}

		// maintain max size
		Long size = rawRedisTemplate.opsForZSet().size(redisKey);

		if (size != null && size > maxSize) {
			rawRedisTemplate.opsForZSet().removeRange(redisKey, 0, size - maxSize - 1);
		}

		touch(redisKey, cacheName);
	}

	public <T> void batchUpsertCompressedZSetSeries(String cacheName, String key, List<T> values,
			ToDoubleFunction<T> scoreExtractor, int maxSize) {

		if (values == null || values.isEmpty())
			return;

		String redisKey = buildKey(cacheName, key);
		ObjectMapper objectMapper = new ObjectMapper();
		// optional but recommended
		values.sort(Comparator.comparingDouble(scoreExtractor::applyAsDouble));

		for (int i = 0; i < values.size(); i += PIPELINE_CHUNK) {

			List<T> chunk = values.subList(i, Math.min(i + PIPELINE_CHUNK, values.size()));

			rawRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {

				RedisSerializer<String> keySer = rawRedisTemplate.getStringSerializer();

				byte[] rawKey = keySer.serialize(redisKey);

				for (T v : chunk) {

					double score = scoreExtractor.applyAsDouble(v);

					try {

						byte[] json = objectMapper.writeValueAsBytes(v);
						byte[] compressed = gzipCompress(json);

						// overwrite same timestamp candle
						connection.zRemRangeByScore(rawKey, score, score);

						connection.zAdd(rawKey, score, compressed);

					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}

				return null;

			});
		}

		// trim series length
		Long size = rawRedisTemplate.opsForZSet().size(redisKey);

		if (size != null && size > maxSize) {

			rawRedisTemplate.opsForZSet().removeRange(redisKey, 0, size - maxSize - 1);
		}

		touch(redisKey, cacheName);
	}

	public void putCompressedValue(String cacheName, String key, String json) {

		if (json == null) {
			return;
		}

		String redisKey = buildKey(cacheName, key);

		try {
			ObjectMapper objectMapper = new ObjectMapper();
			byte[] jsonByte = objectMapper.writeValueAsBytes(json);
			byte[] compressed = gzipCompress(jsonByte);

			rawRedisTemplate.opsForValue().set(redisKey, compressed);

		} catch (Exception e) {

			log.warn("putCompressedValue fail key=" + redisKey + " , reason=" + e.getMessage(), e);
		}
	}

	public byte[] gzipCompress(byte[] data) {
		try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
				GZIPOutputStream gzip = new GZIPOutputStream(bos)) {

			gzip.write(data);
			gzip.finish();

			return bos.toByteArray();

		} catch (IOException e) {
			throw new RuntimeException("Compress failed", e);
		}
	}

	public byte[] gzipDecompressBytes(byte[] compressed) {

		if (compressed == null || compressed.length == 0) {
			return null;
		}

		try (ByteArrayInputStream bis = new ByteArrayInputStream(compressed);
				GZIPInputStream gzip = new GZIPInputStream(bis, 8192);
				ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(1024, compressed.length * 3))) {

			byte[] buffer = new byte[8192];
			int n;

			while ((n = gzip.read(buffer)) >= 0) {
				if (n > 0) {
					out.write(buffer, 0, n);
				}
			}

			return out.toByteArray();

		} catch (IOException e) {
			throw new IllegalStateException("gzip decompress failed", e);
		}
	}

	public <T> List<T> getCompressedZSetByScore(String cacheName, String key,
			Class<T> clazz) {

		String redisKey = buildKey(cacheName, key);

		Set<byte[]> raw = rawRedisTemplate.opsForZSet().rangeByScore(redisKey, 0, -1);

		if (raw == null || raw.isEmpty())
			return Collections.emptyList();
		ObjectMapper objectMapper = new ObjectMapper();
		List<T> result = new ArrayList<>(raw.size());

		for (byte[] compressed : raw) {

			try {

				byte[] json = gzipDecompressBytes(compressed);

				result.add(objectMapper.readValue(json, clazz));

			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		return result;
	}

	public <T> T getCompressedValue(String cacheName, String key, Class<T> clazz) {

		String redisKey = buildKey(cacheName, key);

		byte[] compressed = rawRedisTemplate.opsForValue().get(redisKey);

		if (compressed == null)
			return null;

		try {
			ObjectMapper objectMapper = new ObjectMapper();
			byte[] json = gzipDecompressBytes(compressed);

			T obj = objectMapper.readValue(json, clazz);

			touch(redisKey, cacheName);

			return obj;

		} catch (Exception e) {
			throw new RuntimeException("decompress read fail", e);
		}
	}
}