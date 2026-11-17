package com.bigstock.biz.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
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

    static final List<String> SOURCES = List.of("tradingview", "emerging");
    private static final String CACHE_NAME = "ultraLongLivedCache";
    private static final String CACHE_PREFIX = "marketQuotes:";
    private final CacheOperatorService cache;
    private final MarketQuoteEvents events;

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

        Snapshot snapshot = new Snapshot(request.fetchedAt(), request.batch());
        cache.putObject(CACHE_NAME, CACHE_PREFIX + source, snapshot);
        events.quotesUpdated(source, snapshot);
    }

    Snapshot readSnapshot(String source) throws IOException {
        return cache.getObject(CACHE_NAME, CACHE_PREFIX + source, Snapshot.class);
    }

    public String resolveStockCode(String stock) throws IOException {
        return resolveStock(stock, true);
    }

    public String requireStockCode(String stock) throws IOException {
        return resolveStock(stock, false);
    }

    private String resolveStock(String stock, boolean matchName) throws IOException {
        Set<String> names = new HashSet<>();
        for (String source : SOURCES) {
            Snapshot snapshot = readSnapshot(source);
            if (snapshot == null) {
                continue;
            }
            for (Quote quote : snapshot.items()) {
                if (stock.equalsIgnoreCase(quote.name())) {
                    return quote.name();
                }
                if (matchName && stock.equals(quote.description())) {
                    names.add(quote.name());
                }
            }
        }
        if (names.size() == 1) {
            return names.iterator().next();
        }
        if (names.size() > 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Stock name is ambiguous; use its code");
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Stock not found in market quotes");
    }

    public record Quote(String name, String description, BigDecimal price) {}

    public record QuoteBatch(String fetchedAt, List<Quote> batch) {}

    public record Snapshot(String fetchedAt, List<Quote> items) {}
}
