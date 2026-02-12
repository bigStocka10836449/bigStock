package com.bigstock.biz.controller;

import com.bigstock.sharedComponent.dto.StockBasicInfoResponse;
import com.bigstock.sharedComponent.service.StockBasicInfoQueryService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stock/basic-info")
@RequiredArgsConstructor
public class StockBasicInfoQueryController {

    private final StockBasicInfoQueryService stockBasicInfoQueryService;

    @Operation(summary = "查DB：個股基本資料（給App/Web顯示用，不會外抓）")
    @GetMapping
    public ResponseEntity<?> query(
            @RequestParam String stockId,
            @RequestParam(required = false) String market
    ) {
        if (market != null && !market.isBlank()) {
            return stockBasicInfoQueryService.queryOne(stockId, market)
                    .<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        }

        List<StockBasicInfoResponse> list = stockBasicInfoQueryService.queryAllMarkets(stockId);
        return ResponseEntity.ok(list);
    }
}
