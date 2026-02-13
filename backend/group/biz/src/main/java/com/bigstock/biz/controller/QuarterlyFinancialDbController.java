package com.bigstock.biz.controller;

import com.bigstock.biz.service.QuarterlyFinancialDbService;
import com.bigstock.biz.vo.QuarterlyFinancialVo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/financial/quarterly/db")
public class QuarterlyFinancialDbController {

    private final QuarterlyFinancialDbService dbService;

    /**
     * 最新 N 筆
     * GET /api/financial/quarterly/db/latest?stockId=2330&limit=12
     */
    @GetMapping("/latest")
    public List<QuarterlyFinancialVo> latest(
            @RequestParam String stockId,
            @RequestParam(defaultValue = "12") int limit
    ) {
        return dbService.queryLatestN(stockId, limit);
    }

    /**
     * 區間查（year/quarter）
     * GET /api/financial/quarterly/db/range?stockId=2330&startYear=2022&startQuarter=1&endYear=2025&endQuarter=4
     */
    @GetMapping("/range")
    public List<QuarterlyFinancialVo> range(
            @RequestParam String stockId,
            @RequestParam int startYear,
            @RequestParam int startQuarter,
            @RequestParam int endYear,
            @RequestParam int endQuarter
    ) {
        return dbService.queryRange(stockId, startYear, startQuarter, endYear, endQuarter);
    }
}
