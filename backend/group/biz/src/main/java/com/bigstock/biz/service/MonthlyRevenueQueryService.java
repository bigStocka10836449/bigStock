package com.bigstock.biz.service;

import com.bigstock.biz.client.TpexMonthlyRevenueClient;
import com.bigstock.biz.client.TwseMonthlyRevenueClient;
import com.bigstock.biz.service.MonthlyRevenueRedisService;
import com.bigstock.biz.vo.MonthlyRevenueVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MonthlyRevenueQueryService {

    private final TwseMonthlyRevenueClient twseClient;
    private final TpexMonthlyRevenueClient tpexClient;
    private final MonthlyRevenueRedisService redisService;

    public List<MonthlyRevenueVo> fetchMonthlyRevenueToRedis(
            String yearMonth,
            String marketNullable,
            boolean forceRefresh
    ) {
        String ym = normalizeYearMonth(yearMonth); // yyyy-MM
        String market = normalizeMarketNullable(marketNullable); // null = both

        // 若不強制刷新，且 Redis 已有，就直接回 Redis
        if (!forceRefresh && redisService.exists(ym)) {
            return redisService.read(ym);
        }

        List<MonthlyRevenueVo> merged = new ArrayList<>();

        if (market == null || "TWSE".equalsIgnoreCase(market)) {
            merged.addAll(twseClient.fetchMonthlyRevenue(ym));
        }
        if (market == null || "TPEX".equalsIgnoreCase(market)) {
            merged.addAll(tpexClient.fetchMonthlyRevenue(ym));
        }

        // 寫入 Redis（以 ym 當 key 的月份）
        redisService.write(ym, merged);

        // 回傳給你（方便你 postman 直接看）
        return merged;
    }

    public List<MonthlyRevenueVo> readMonthlyRevenueFromRedis(String yearMonth) {
        String ym = normalizeYearMonth(yearMonth);
        return redisService.read(ym);
    }

    private static String normalizeMarketNullable(String market) {
        String m = safe(market).toUpperCase();
        return m.isBlank() ? null : m; // null 表示不過濾市場（兩邊都抓）
    }

    private static String normalizeYearMonth(String yearMonth) {
        String ym = safe(yearMonth);
        if (!ym.matches("^\\d{4}-\\d{2}$")) {
            throw new IllegalArgumentException("yearMonth must be yyyy-MM, but got: " + yearMonth);
        }
        return ym;
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
