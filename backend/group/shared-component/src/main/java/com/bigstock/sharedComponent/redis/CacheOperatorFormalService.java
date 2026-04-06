package com.bigstock.sharedComponent.redis;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CacheOperatorFormalService {

    private final StringRedisTemplate stringRedisTemplate;

    // ---------- SET ----------
    public void putSet(String key, Collection<String> values) {
        if (values == null || values.isEmpty()) return;

        stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            byte[] k = key.getBytes();
            for (String v : values) {
                connection.sAdd(k, v.getBytes());
            }
            return null;
        });
    }

    public Set<String> getSet(String key) {
        Set<String> result = stringRedisTemplate.opsForSet().members(key);
        return result != null ? result : Collections.emptySet();
    }

    public void addToSet(String key, String value) {
    	stringRedisTemplate.opsForSet().add(key, value);
    }

    // ---------- HASH ----------
    public void putHash(String key, Map<String, String> map) {
    	stringRedisTemplate.opsForHash().putAll(key, map);
    }

    public Map<Object, Object> getHash(String key) {
        return stringRedisTemplate.opsForHash().entries(key);
    }

    // ---------- STRING ----------
    public void putValue(String key, String value) {
    	stringRedisTemplate.opsForValue().set(key, value);
    }

    public String getValue(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    // ---------- COMMON ----------
    public void delete(String key) {
    	stringRedisTemplate.delete(key);
    }

    public boolean exists(String key) {
        Boolean exist = stringRedisTemplate.hasKey(key);
        return Boolean.TRUE.equals(exist);
    }
}
