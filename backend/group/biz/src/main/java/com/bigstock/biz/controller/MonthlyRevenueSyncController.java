package com.bigstock.biz.controller;

import com.bigstock.biz.service.MonthlyRevenueDbService;
import com.bigstock.sharedComponent.service.StockRevenueService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/revenue/monthly")
public class MonthlyRevenueSyncController {

    private final MonthlyRevenueDbService dbService;

    @GetMapping("/syncDb")
    public StockRevenueService.SyncResult syncDb(
            @RequestParam(required = false) String yearMonth,     // yyyy-MM
            @RequestParam(required = false) String market,        // TWSE/TPEX
            @RequestParam(defaultValue = "false") boolean forceRefresh
    ) {
        return dbService.fetchToRedisAndUpsertDb(normalizeToYyyyMm(yearMonth), market, forceRefresh);
    }

    private static String normalizeToYyyyMm(String ym) {
        if (ym == null || ym.isBlank()) {
            return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        }
        String s = ym.trim();
        if (s.matches("^\\d{6}$")) return s.substring(0, 4) + "-" + s.substring(4, 6);
        if (s.matches("^\\d{4}-\\d{2}$")) return s;
        throw new IllegalArgumentException("yearMonth must be yyyy-MM or yyyyMM, but got: " + s);
    }
}
