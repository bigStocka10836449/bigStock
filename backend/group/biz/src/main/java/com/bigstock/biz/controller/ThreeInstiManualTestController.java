package com.bigstock.biz.controller;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bigstock.biz.service.ThreeInstiService;
import com.bigstock.sharedComponent.service.StockThreeInstitutionalTradingService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/debug/threeinsti")
public class ThreeInstiManualTestController {

    private final ThreeInstiService threeInstiService;
    private final RedissonClient redissonClient;

  
    /**
     * 讀取 Redis 裡的內容（raw / norm）
     *
     * GET /debug/threeinsti/get?market=twse&type=norm&date=20260123
     */
    @GetMapping("/get")
    public Map<String, Object> get(
            @RequestParam String market,
            @RequestParam String type,
            @RequestParam(required = false) String date
    ) {
        String yyyyMMdd = (date == null || date.isBlank())
                ? LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                : date.trim();

        String key = "threeinsti:" + market + ":" + yyyyMMdd + ":" + type;

        // ✅ 用 StringCodec 避免顯示/編碼問題
        RBucket<String> bucket = redissonClient.getBucket(key, StringCodec.INSTANCE);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("key", key);
        resp.put("exists", bucket.isExists());
        resp.put("ttlMs", bucket.remainTimeToLive());
        String v = bucket.get();
        resp.put("length", v == null ? 0 : v.length());
        resp.put("value", v);
        return resp;
    }
}
