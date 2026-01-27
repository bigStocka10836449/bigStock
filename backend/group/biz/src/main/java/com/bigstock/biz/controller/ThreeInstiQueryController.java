package com.bigstock.biz.controller;

import com.bigstock.biz.service.ThreeInstiQueryService;
import com.bigstock.biz.vo.ThreeInstitutionalTradingVo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/threeinsti")
public class ThreeInstiQueryController {

    private final ThreeInstiQueryService queryService;

    /**
     * 查某檔股票在日期區間（三大法人）
     * GET /api/threeinsti/stock/2330?start=2026-01-01&end=2026-01-31&market=TWSE
     */
    @GetMapping("/stock/{stockCode}")
    public List<ThreeInstitutionalTradingVo> queryStockRange(
            @PathVariable String stockCode,
            @RequestParam String start,
            @RequestParam String end,
            @RequestParam(required = false) String market
    ) {
        LocalDate startDate = LocalDate.parse(start);
        LocalDate endDate = LocalDate.parse(end);
        return queryService.queryStockRange(stockCode, market, startDate, endDate);
    }

    /**
     * 查某天資料（可分頁）
     * GET /api/threeinsti/date?date=2026-01-23&market=TPEX&page=0&size=50
     */
    @GetMapping("/date")
    public Page<ThreeInstitutionalTradingVo> queryByDate(
            @RequestParam String date,
            @RequestParam(required = false) String market,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        LocalDate tradeDate = LocalDate.parse(date);
        return queryService.queryByDate(tradeDate, market, page, size);
    }

    /**
     * 排行榜：foreign / trust / dealer（依 net buy 排序）
     * GET /api/threeinsti/rank?type=foreign&date=2026-01-23&market=TWSE&page=0&size=50
     */
    @GetMapping("/rank")
    public Page<ThreeInstitutionalTradingVo> rank(
            @RequestParam(defaultValue = "foreign") String type,
            @RequestParam String date,
            @RequestParam(required = false) String market,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        LocalDate tradeDate = LocalDate.parse(date);
        return queryService.rank(type, tradeDate, market, page, size);
    }
}