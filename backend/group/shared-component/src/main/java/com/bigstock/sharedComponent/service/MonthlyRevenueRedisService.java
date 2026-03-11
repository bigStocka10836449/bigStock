package com.bigstock.sharedComponent.service;

import com.bigstock.sharedComponent.dto.MonthlyRevenueVo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MonthlyRevenueRedisService {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 你要看的 key 格式
    private static String key(String yearMonth) {
        return "REVENUE:MONTHLY:" + yearMonth;
    }

    public boolean exists(String yearMonth) {
        Boolean hasKey = redis.hasKey(key(yearMonth));
        return hasKey != null && hasKey;
    }

    public void write(String yearMonth, List<MonthlyRevenueVo> list) {
        try {
            String json = objectMapper.writeValueAsString(list);
            redis.opsForValue().set(key(yearMonth), json);

            // 你若想要自動過期（可自行調整/拿掉）
            redis.expire(key(yearMonth), Duration.ofDays(45));
        } catch (Exception e) {
            throw new RuntimeException("write redis failed: " + e.getMessage(), e);
        }
    }

    public List<MonthlyRevenueVo> read(String yearMonth) {
        try {
            String json = redis.opsForValue().get(key(yearMonth));
            if (json == null || json.isBlank()) return Collections.emptyList();
            return objectMapper.readValue(json, new TypeReference<List<MonthlyRevenueVo>>() {});
        } catch (Exception e) {
            throw new RuntimeException("read redis failed: " + e.getMessage(), e);
        }
    }
}
