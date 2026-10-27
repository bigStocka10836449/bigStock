package com.bigstock.biz.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bigstock.sharedComponent.dto.MonthlyRevenueVo;
import com.bigstock.sharedComponent.service.MonthlyRevenueQueryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/revenue")
public class MonthlyRevenueQueryController {

    private final MonthlyRevenueQueryService queryService;

    /**
     * POST /api/revenue/monthly?yearMonth=2025-10&market=TWSE&forceRefresh=false
     */
    @PostMapping("/monthly")
    public List<MonthlyRevenueVo> fetchMonthlyRevenueToRedis(
            @RequestParam String yearMonth
    ) {
        return queryService.fetchMonthlyRevenueToRedis(yearMonth);
    }

    /**
     * 只讀 Redis（看你剛寫進去的結果）
     * GET /api/revenue/monthly?yearMonth=2025-10
     */
    @GetMapping("/monthly")
    public List<MonthlyRevenueVo> readMonthlyRevenueFromRedis(
            @RequestParam String yearMonth
    ) {
        return queryService.readMonthlyRevenueFromRedis(yearMonth);
    }
    
    /**
     * 取得最近 12 個已結束月份的營收。
     *
     * GET /api/revenue/recent
     */
    @GetMapping("/recent")
    public Map<String, List<MonthlyRevenueVo>> readRecentRevenue() {

        return queryService.readRecentRevenue();
    }
    
    /**
     * 取得指定股票最近 24 個月營收。
     *
     * GET /api/revenue/recent/2330
     */
    @GetMapping("/recent/{stockId}")
    public Map<String, MonthlyRevenueVo> readRecentStockRevenue(
            @PathVariable String stockId) {

        return queryService.readRecentStockRevenue(stockId);
    }
}
