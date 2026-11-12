package com.bigstock.sharedComponent.service;

import java.net.URI;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;

@Service
public class MarketQuotesJobService {

    private static final ZoneId TAIPEI = ZoneId.of("Asia/Taipei");
    private static final String PAUSE_KEY_PREFIX = "marketQuotes:pauseUntil:";

    private final StringRedisTemplate redis;
    private final String workerUrl;
    private final RestTemplate restTemplate;

    public MarketQuotesJobService(StringRedisTemplate redis,
            @Value("${market.quotes.worker.url:}") String workerUrl) {
        this.redis = redis;
        this.workerUrl = workerUrl;
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(65_000);
        this.restTemplate = new RestTemplate(factory);
    }

    public boolean isConfigured() {
        return !workerUrl.isBlank();
    }

    public String pauseToday() {
        ZonedDateTime now = ZonedDateTime.now(TAIPEI);
        ZonedDateTime midnight = now.toLocalDate().plusDays(1).atStartOfDay(TAIPEI);
        String until = midnight.toOffsetDateTime().toString();
        redis.opsForValue().set(PAUSE_KEY_PREFIX + now.toLocalDate(), until, Duration.between(now, midnight));
        return until;
    }

    public Object runScheduled() {
        String pausedUntil = redis.opsForValue().get(PAUSE_KEY_PREFIX + ZonedDateTime.now(TAIPEI).toLocalDate());
        if (pausedUntil != null) {
            return Map.of("status", "PAUSED_TODAY", "pausedUntil", pausedUntil);
        }
        return triggerWorker(true);
    }

    public Object runNow() {
        return triggerWorker(false);
    }

    private Object triggerWorker(boolean scheduled) {
        if (!isConfigured()) {
            return Map.of("status", "NOT_CONFIGURED");
        }
        URI uri = UriComponentsBuilder.fromUriString(workerUrl)
                .replaceQueryParam("scheduled", scheduled)
                .build(true)
                .toUri();
        JsonNode response = restTemplate.postForObject(uri, null, JsonNode.class);
        return response == null ? Map.of("status", "FAILED") : response;
    }
}
