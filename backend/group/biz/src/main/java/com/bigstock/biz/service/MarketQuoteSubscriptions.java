package com.bigstock.biz.service;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MarketQuoteSubscriptions {

    public static final List<String> DEFAULT_STOCKS = List.of("2330", "2303", "2317", "2454", "2881");
    public static final Duration LIFETIME = Duration.ofDays(10);

    private static final String KEY_PREFIX = "marketQuotes:subscriptions:";
    private static final String EMPTY_LIST_MARKER = "#";
    private static final int MAX_STOCKS = 100;

    // A marker keeps an empty list addressable until its original expiry.
    private static final DefaultRedisScript<Long> CHANGE = new DefaultRedisScript<>("""
            local key = KEYS[1]
            local code = ARGV[1]
            local ttl = tonumber(ARGV[5])
            if KEYS[2] then
                if redis.call('GET', KEYS[2]) ~= ARGV[6] then
                    return -2
                end
                ttl = redis.call('PTTL', KEYS[2])
                if ttl <= 0 then
                    return -2
                end
            end
            if ARGV[2] == 'false' then
                return redis.call('SREM', key, code)
            end
            if redis.call('EXISTS', key) == 0 then
                redis.call('SADD', key, ARGV[3], code)
                redis.call('PEXPIRE', key, ttl)
                return 1
            end
            if redis.call('SISMEMBER', key, code) == 1 then
                return 0
            end
            if redis.call('SCARD', key) - 1 >= tonumber(ARGV[4]) then
                return -1
            end
            return redis.call('SADD', key, code)
            """, Long.class);

    private static final DefaultRedisScript<Long> REFRESH_WEB = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[2]) ~= ARGV[1] then
                return 0
            end
            if redis.call('PEXPIRE', KEYS[1], ARGV[2]) == 0 then
                return 0
            end
            return redis.call('PEXPIRE', KEYS[2], ARGV[2])
            """, Long.class);

    private final StringRedisTemplate redis;

    public boolean change(String userId, String stockCode, boolean subscribed) {
        return change(List.of(key(userId)), userId, stockCode, subscribed);
    }

    public void changeWeb(WebQuoteVisitor.Visitor visitor, String stockCode, boolean subscribed) {
        change(List.of(key(visitor.owner()), visitor.redisKey()), visitor.owner(), stockCode, subscribed);
    }

    private boolean change(List<String> keys, String owner, String stockCode, boolean subscribed) {
        Long changed = redis.execute(CHANGE, keys, stockCode,
                Boolean.toString(subscribed), EMPTY_LIST_MARKER, Integer.toString(MAX_STOCKS),
                Long.toString(LIFETIME.toMillis()), owner);
        if (changed == null) {
            throw new IllegalStateException("Redis did not return a subscription result");
        }
        if (changed == -1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Subscription limit is 100 stocks");
        }
        if (changed == -2) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Web quote visitor expired");
        }
        return changed == 1;
    }

    public List<String> stockCodes(String userId) {
        Set<String> members = redis.opsForSet().members(key(userId));
        if (members == null) {
            return List.of();
        }
        return members.stream().filter(code -> !EMPTY_LIST_MARKER.equals(code)).sorted().toList();
    }

    public boolean refresh(String userId) {
        return Boolean.TRUE.equals(redis.expire(key(userId), LIFETIME));
    }

    public boolean refreshWeb(WebQuoteVisitor.Visitor visitor) {
        Long refreshed = redis.execute(REFRESH_WEB, List.of(key(visitor.owner()), visitor.redisKey()),
                visitor.owner(), Long.toString(LIFETIME.toMillis()));
        return Long.valueOf(1L).equals(refreshed);
    }

    private String key(String userId) {
        return KEY_PREFIX + userId;
    }
}
