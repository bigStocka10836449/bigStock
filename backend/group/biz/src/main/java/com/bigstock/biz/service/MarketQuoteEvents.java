package com.bigstock.biz.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.bigstock.biz.service.MarketQuotesService.Snapshot;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarketQuoteEvents {

    public static final String CHANNEL = "marketQuotes:updates";

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;

    public void quotesUpdated(String source, Snapshot snapshot) {
        publish(new Update(source, snapshot, null));
    }

    public void subscriptionsUpdated(String userId) {
        publish(new Update(null, null, userId));
    }

    private void publish(Update update) {
        try {
            redis.convertAndSend(CHANNEL, mapper.writeValueAsString(update));
        } catch (JsonProcessingException | RuntimeException ex) {
            // The Redis write already succeeded. A reconnect loads its current state.
            log.warn("Could not publish market quote update", ex);
        }
    }

    public record Update(String source, Snapshot snapshot, String userId) {}
}
