package com.bigstock.biz.controller;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.bigstock.sharedComponent.client.TpexQuarterlyFinancialClient;
import com.bigstock.sharedComponent.client.TwseQuarterlyFinancialClient;
import com.bigstock.sharedComponent.dto.QuarterlyFinancialVo;
import com.bigstock.sharedComponent.service.QuarterlyFinancialRedisService;
import com.bigstock.sharedComponent.service.StockFinancialRedisService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/financial/quarterly")
public class QuarterlyFinancialController {

    private final TpexQuarterlyFinancialClient tpexClient;

    private final TwseQuarterlyFinancialClient twseClient;

    private final QuarterlyFinancialRedisService redisService;

    private final StockFinancialRedisService stockFinancialRedisService;

    /**
     * ============================================================
     * 從官方取得季度財報
     *
     * 同步更新：
     * 1. Redis
     * 2. JVM Memory
     * 3. PostgreSQL
     * ============================================================
     */
    @PostMapping
    public void fetchToRedis(
            @RequestParam int year,
            @RequestParam int quarter,
            @RequestParam String market
    ) {

        String normalizedMarket =
                market.trim().toUpperCase(Locale.ROOT);

        List<QuarterlyFinancialVo> list;

        switch (normalizedMarket) {

            case "TWSE":
                list = twseClient.fetch(year, quarter);
                break;

            case "TPEX":
                list = tpexClient.fetch(year, quarter);
                break;

            default:
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Unsupported market: " + market
                );
        }

        redisService.write(
                year,
                quarter,
                normalizedMarket,
                list
        );
    }


    /**
     * ============================================================
     * 取得整個季度財報
     *
     * JVM Memory → Redis Fallback
     * ============================================================
     */
    @GetMapping
    public List<QuarterlyFinancialVo> readFromRedis(
            @RequestParam int year,
            @RequestParam int quarter,
            @RequestParam String market
    ) {

        return redisService.read(
                year,
                quarter,
                market
        );
    }


    /**
     * ============================================================
     * 取得指定公司單季財報
     *
     * JVM Memory → Redis Fallback
     *
     * Average lookup complexity: O(1)
     * ============================================================
     */
    @GetMapping("/{stockId}")
    public ResponseEntity<QuarterlyFinancialVo> getStockFinancial(
            @PathVariable String stockId,
            @RequestParam int year,
            @RequestParam int quarter,
            @RequestParam String market
    ) {

        QuarterlyFinancialVo financial =
                redisService.readOne(
                        year,
                        quarter,
                        market,
                        stockId
                );

        if (financial == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(financial);
    }
    
    @GetMapping("/stock/{market}/{stockId}")
    public ResponseEntity<List<QuarterlyFinancialVo>> getStockFinancials(
            @PathVariable String market,
            @PathVariable String stockId
    ) {

        List<QuarterlyFinancialVo> financials =
                stockFinancialRedisService.getStockFinancials(
                        market,
                        stockId
                );

        // 過濾無效季度、依照時間由新到舊排序
        List<QuarterlyFinancialVo> result = financials.stream()
                .filter(f -> f != null
                        && f.getYear() != null
                        && f.getQuarter() != null
                        && f.getQuarter() >= 1
                        && f.getQuarter() <= 4)
                .sorted(
                        Comparator
                                .comparing(
                                        QuarterlyFinancialVo::getYear
                                )
                                .thenComparing(
                                        QuarterlyFinancialVo::getQuarter
                                )
                                .reversed()
                )
                .limit(13)
                .toList();

        return ResponseEntity.ok(result);
    }
}