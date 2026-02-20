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
            String yearMonth
    ) {
		String ym = normalizeYearMonth(yearMonth); // yyyy-MM

		List<MonthlyRevenueVo> merged = new ArrayList<>();

		merged.addAll(twseClient.fetchMonthlyRevenue(ym));
		merged.addAll(tpexClient.fetchMonthlyRevenue(ym));

		return merged;
	}

    public List<MonthlyRevenueVo> readMonthlyRevenueFromRedis(String yearMonth) {
        String ym = normalizeYearMonth(yearMonth);
        return redisService.read(ym);
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
