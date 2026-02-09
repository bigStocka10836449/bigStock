package com.bigstock.sharedComponent.redis;

import com.bigstock.sharedComponent.dto.QuarterlyFinancialResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class StockQuarterFinancialRedisRepository {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    // 你目前 TTL 看起來很長（你可依需求調）
    private static final Duration TTL = Duration.ofDays(90);

    public String key(int year, int quarter, String market) {
        String m = (market == null) ? "" : market.trim().toUpperCase();
        return "FIN:QTR:" + year + "Q" + quarter + ":" + m;
    }

    public void setQuarterList(int year, int quarter, String market, List<QuarterlyFinancialResponse> list) {
        try {
            String k = key(year, quarter, market);
            String json = objectMapper.writeValueAsString(list == null ? Collections.emptyList() : list);
            redis.opsForValue().set(k, json, TTL);
        } catch (Exception e) {
            throw new RuntimeException("redis setQuarterList failed: " + e.getMessage(), e);
        }
    }

    public List<QuarterlyFinancialResponse> getQuarterList(int year, int quarter, String market) {
        try {
            String k = key(year, quarter, market);
            String json = redis.opsForValue().get(k);
            if (json == null || json.isBlank()) return Collections.emptyList();
            return objectMapper.readValue(json, new TypeReference<List<QuarterlyFinancialResponse>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
