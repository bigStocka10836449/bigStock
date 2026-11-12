package com.bigstock.biz.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.bigstock.sharedComponent.redis.CacheOperatorService;
import com.fasterxml.jackson.core.JsonProcessingException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MarketQuotesService {

    private static final Set<String> SOURCES = Set.of("tradingview", "emerging");
    private final CacheOperatorService cache;

    public void importQuotes(String source, QuoteBatch request) throws JsonProcessingException {
        if (!SOURCES.contains(source)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown market quote source");
        }
        if (request.fetchedAt() == null || request.batch() == null || request.batch().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Expected fetchedAt and a non-empty batch");
        }
        try {
            OffsetDateTime.parse(request.fetchedAt());
        } catch (DateTimeParseException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid fetchedAt", ex);
        }

        cache.putObject("ultraLongLivedCache", "marketQuotes:" + source,
                new Snapshot(request.fetchedAt(), request.batch()));
    }

    public record Quote(String name, String description, BigDecimal price) {}

    public record QuoteBatch(String fetchedAt, List<Quote> batch) {}

    public record Snapshot(String fetchedAt, List<Quote> items) {}
}
