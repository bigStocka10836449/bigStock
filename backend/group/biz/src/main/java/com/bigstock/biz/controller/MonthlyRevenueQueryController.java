package com.bigstock.biz.controller;

import com.bigstock.biz.service.MonthlyRevenueQueryService;
import com.bigstock.biz.vo.MonthlyRevenueVo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
}
