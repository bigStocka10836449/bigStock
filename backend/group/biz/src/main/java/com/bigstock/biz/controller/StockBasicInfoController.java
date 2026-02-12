package com.bigstock.biz.controller;

import com.bigstock.biz.dto.StockBasicInfoResponse;
import com.bigstock.biz.service.StockBasicInfoAggregationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stock/basic-info")
public class StockBasicInfoController {

    private final StockBasicInfoAggregationService service;

    /**
     * GET /api/stock/basic-info?stockId=2330&market=TWSE
     * GET /api/stock/basic-info?stockId=1264&market=TPEX
     *
     * cacheOnly=true：只讀 Redis（不重新聚合）
     */
    @GetMapping
    public StockBasicInfoResponse query(
            @RequestParam String stockId,
            @RequestParam(required = false) String market,
            @RequestParam(required = false, defaultValue = "false") boolean cacheOnly
    ) {
        if (cacheOnly) return service.readCache(stockId, market);
        return service.queryAndCache(stockId, market);
    }
}
