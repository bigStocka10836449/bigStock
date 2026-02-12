package com.bigstock.biz.controller;

import com.bigstock.biz.service.StockBasicInfoDbService;
import com.bigstock.sharedComponent.service.StockBasicInfoService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stock/basic-info")
@RequiredArgsConstructor
public class StockBasicInfoSyncController {

    private final StockBasicInfoDbService stockBasicInfoDbService;

    @Operation(summary = "同步個股基本資料到DB（可手動/排程）")
    @PostMapping("/syncDb")
    public StockBasicInfoService.SyncResult syncDb(
            @RequestParam String stockId,
            @RequestParam(required = false) String market,           // TWSE / TPEX / null
            @RequestParam(defaultValue = "false") boolean forceRefresh,
            @RequestParam(required = false) Integer maxAgeHours      // null => 24
    ) {
        return stockBasicInfoDbService.syncToDb(stockId, market, forceRefresh, maxAgeHours);
    }

    // 你若偏好 GET（方便排程/瀏覽器），也可以保留
    @Operation(summary = "同步個股基本資料到DB（GET版）")
    @GetMapping("/syncDb")
    public StockBasicInfoService.SyncResult syncDbGet(
            @RequestParam String stockId,
            @RequestParam(required = false) String market,
            @RequestParam(defaultValue = "false") boolean forceRefresh,
            @RequestParam(required = false) Integer maxAgeHours
    ) {
        return stockBasicInfoDbService.syncToDb(stockId, market, forceRefresh, maxAgeHours);
    }
}
