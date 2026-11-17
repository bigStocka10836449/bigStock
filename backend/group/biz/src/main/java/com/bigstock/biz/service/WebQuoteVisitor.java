package com.bigstock.biz.service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Pattern;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebQuoteVisitor {

    public static final String COOKIE_NAME = "__Host-bstock_visitor";

    private static final String KEY_PREFIX = "marketQuotes:webVisitor:";
    private static final Pattern TOKEN_FORMAT = Pattern.compile("[A-Za-z0-9_-]{43}");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redis;

    public Visitor find(String token) {
        if (token == null || !TOKEN_FORMAT.matcher(token).matches()) {
            return null;
        }
        String key = key(token);
        String owner = redis.opsForValue().get(key);
        return owner == null ? null : new Visitor(owner, key);
    }

    public Visitor findForSocket(Cookie[] cookies) {
        if (cookies == null) {
            return null;
        }
        String token = null;
        for (Cookie cookie : cookies) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                if (token != null) {
                    return null;
                }
                token = cookie.getValue();
            }
        }
        try {
            return find(token);
        } catch (RuntimeException ex) {
            log.warn("Could not load web quote visitor for WebSocket", ex);
            return null;
        }
    }

    public boolean isCurrent(Visitor visitor) {
        return visitor.owner().equals(redis.opsForValue().get(visitor.redisKey()));
    }

    public IssuedVisitor issue() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        String key = key(token);
        String owner = "web:" + UUID.randomUUID();
        if (!Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(key, owner, MarketQuoteSubscriptions.LIFETIME))) {
            throw new IllegalStateException("Could not create a web quote visitor");
        }
        return new IssuedVisitor(new Visitor(owner, key), cookie(token));
    }

    public ResponseCookie cookie(String token) {
        return ResponseCookie.from(COOKIE_NAME, token)
                .httpOnly(true).secure(true).sameSite("Strict").path("/")
                .maxAge(MarketQuoteSubscriptions.LIFETIME).build();
    }

    private String key(String token) {
        return KEY_PREFIX + DigestUtils.sha256Hex(token);
    }

    public record Visitor(String owner, String redisKey) {}

    public record SocketIdentity(Visitor visitor, Instant expiresAt) {}

    public record IssuedVisitor(Visitor visitor, ResponseCookie cookie) {}
}
