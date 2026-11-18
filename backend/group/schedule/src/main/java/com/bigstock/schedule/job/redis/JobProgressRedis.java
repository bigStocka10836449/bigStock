package com.bigstock.schedule.job.redis;

import com.bigstock.schedule.job.model.JobRun;
import java.time.Duration;
import java.util.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class JobProgressRedis {
	private final StringRedisTemplate redis;

	public JobProgressRedis(StringRedisTemplate redis) {
		this.redis = redis;
	}

	private String key(UUID id) {
		return "bstock:job:progress:" + id;
	}

	public void put(UUID id, Map<String, String> values) {
		String k = key(id);
		redis.opsForHash().putAll(k, values);
		redis.expire(k, Duration.ofDays(7));
	}

	public Map<Object, Object> get(UUID id) {
		return redis.opsForHash().entries(key(id));
	}

	/** Bounded recent error summaries; Redis is not authoritative audit storage. */
	public void appendError(UUID id, String item, String description) {
		String k = "bstock:job:errors:" + id;
		redis.opsForList().leftPush(k, java.time.Instant.now() + " | " + item + " | " + description);
		redis.opsForList().trim(k, 0, 99);
		redis.expire(k, Duration.ofDays(7));
	}

	public List<String> errors(UUID id) {
		return redis.opsForList().range("bstock:job:errors:" + id, 0, 99);
	}

}
