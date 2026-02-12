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

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(StockBasicInfoQueryController.class);

    private final StockBasicInfoQueryService stockBasicInfoQueryService;

    @GetMapping("/db")
    public ResponseEntity<?> queryDb(
            @RequestParam String stockId,
            @RequestParam(required = false) String market
    ) {
        log.info("[BASIC_DB] hit controller, stockId='{}', market='{}'", stockId, market);

        if (market != null && !market.isBlank()) {
            var opt = stockBasicInfoQueryService.queryOne(stockId, market);
            log.info("[BASIC_DB] queryOne present={}", opt.isPresent());
            return opt.<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        }

        var list = stockBasicInfoQueryService.queryAllMarkets(stockId);
        log.info("[BASIC_DB] queryAllMarkets size={}", list.size());
        return ResponseEntity.ok(list);
    }
}
