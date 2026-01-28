package com.bigstock.biz.controller;

import com.bigstock.biz.service.ThreeInstiService;
import com.bigstock.sharedComponent.service.StockThreeInstitutionalTradingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/threeinsti")
public class ThreeInstiSyncController {

    private final ThreeInstiService threeInstiService;

    /**
     * ✅ 正式同步入口：外部可帶日期，觸發
     * 1) 抓 TWSE + TPEX → 寫 Redis（raw/norm）
     * 2) Redis norm → 寫 DB（覆寫同日同股）
     *
     * 用法：
     * - GET /api/threeinsti/sync?date=20260123
     * - GET /api/threeinsti/sync?date=2026-01-23
     * - GET /api/threeinsti/sync           (不帶 date 就用今天)
     */
    @GetMapping("/sync")
    public StockThreeInstitutionalTradingService.SyncResult sync(@RequestParam(required = false) String date) {
        String yyyyMMdd = normalizeToYyyyMMdd(date);
        Duration ttl = Duration.ofHours(6);
        return threeInstiService.fetchToRedisAndSyncDb(yyyyMMdd, ttl);
    }

    private static String normalizeToYyyyMMdd(String date) {
        if (date == null || date.isBlank()) {
            return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        }
        String d = date.trim();

        // 支援 yyyy-MM-dd
        if (d.contains("-")) {
            LocalDate ld = LocalDate.parse(d);
            return ld.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        }

        // 預期 yyyyMMdd
        return d;
    }
}
