package com.bigstock.biz.controller;

import com.bigstock.biz.dto.QuarterlyProfitabilityResponse;
import com.bigstock.biz.service.QuarterlyProfitabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/financial/profitability/quarterly")
public class QuarterlyProfitabilityController {

    private final QuarterlyProfitabilityService profitabilityService;

    /**
     * 固定回最新 4 個會計年（最多 16 季），不足就回實際筆數
     *
     * Example:
     * GET /api/financial/profitability/quarterly?stockId=2330&market=TWSE
     * GET /api/financial/profitability/quarterly?stockId=2330
     */
    @GetMapping
    public List<QuarterlyProfitabilityResponse> queryLatest4Years(
            @RequestParam String stockId,
            @RequestParam(required = false) String market
    ) {
        return profitabilityService.queryLatest4Years(stockId, market);
    }
}
