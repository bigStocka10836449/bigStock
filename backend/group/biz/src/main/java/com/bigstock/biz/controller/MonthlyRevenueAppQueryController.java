package com.bigstock.biz.controller;

import com.bigstock.sharedComponent.dto.StockRevenueResponse;
import com.bigstock.sharedComponent.service.StockRevenueQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/revenue/monthly")
public class MonthlyRevenueAppQueryController {

    private final StockRevenueQueryService stockRevenueQueryService;

    private static final DateTimeFormatter YM = DateTimeFormatter.ofPattern("yyyy-MM");

    /**
     * App 查 DB：單一個股 月營收列表
     *
     * GET /api/revenue/monthly/stock/{stockId}?start=yyyy-MM-dd&end=yyyy-MM-dd&market=TWSE|TPEX
     * - start/end 也可直接傳 yyyy-MM
     * - 若不帶 start/end：預設近 24 個月（含當月）
     * - 回傳：List<StockRevenueResponse>，date = yyyy-MM，revenue = 月營收(千元)
     */
    @GetMapping("/stock/{stockId}")
    public List<StockRevenueResponse> getStockMonthlyRevenue(
            @PathVariable("stockId") String stockId,
            @RequestParam(value = "start", required = false) String start,
            @RequestParam(value = "end", required = false) String end,
            @RequestParam(value = "market", required = false) String market
    ) {
        LocalDate endMonth = parseMonthFirstDayOrDefault(end, LocalDate.now());
        LocalDate startMonth = parseMonthFirstDayOrNull(start);

        if (startMonth == null) {
            startMonth = endMonth.minusMonths(23); // 24 個月含 endMonth
        }

        // 如果 start > end，就直接交換，避免 app 傳反
        if (startMonth.isAfter(endMonth)) {
            LocalDate tmp = startMonth;
            startMonth = endMonth;
            endMonth = tmp;
        }

        return stockRevenueQueryService.queryStockMonthlyRevenue(
                stockId,
                market,
                startMonth,
                endMonth
        );
    }

    /**
     * 允許 yyyy-MM-dd 或 yyyy-MM
     * 轉成 LocalDate(yyyy-MM-01)
     */
    private LocalDate parseMonthFirstDayOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty()) return null;
        if (t.length() >= 7) {
            String ym = t.substring(0, 7);
            return LocalDate.parse(ym + "-01");
        }
        return null;
    }

    private LocalDate parseMonthFirstDayOrDefault(String s, LocalDate fallback) {
        LocalDate parsed = parseMonthFirstDayOrNull(s);
        if (parsed != null) return parsed;
        // fallback 的「當月第一天」
        return LocalDate.of(fallback.getYear(), fallback.getMonth(), 1);
    }
}
