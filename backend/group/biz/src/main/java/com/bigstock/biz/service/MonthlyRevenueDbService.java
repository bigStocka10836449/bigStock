package com.bigstock.biz.service;

import com.bigstock.biz.utils.MonthlyRevenueMapper;
import com.bigstock.biz.vo.MonthlyRevenueVo;
import com.bigstock.sharedComponent.dto.StockRevenueResponse;
import com.bigstock.sharedComponent.service.StockRevenueService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MonthlyRevenueDbService {

    private final MonthlyRevenueQueryService monthlyRevenueQueryService;
    private final StockRevenueService stockRevenueService;

    /**
     * 1) 抓 TWSE/TPEX → 寫 Redis（沿用你現有的 MonthlyRevenueQueryService）
     * 2) 用回傳的 merged list 直接 upsert DB（避免再讀 Redis 一次）
     */
    @Transactional(rollbackFor = { Exception.class })
    public StockRevenueService.SyncResult fetchToRedisAndUpsertDb(
            String yearMonth,        // yyyy-MM
            String marketNullable,   // TWSE / TPEX / null(兩個)
            boolean forceRefresh
    ) {
        List<MonthlyRevenueVo> merged =
                monthlyRevenueQueryService.fetchMonthlyRevenueToRedis(yearMonth, marketNullable, forceRefresh);

        List<StockRevenueResponse> mapped =
                merged.stream().map(MonthlyRevenueMapper::toStockRevenueResponse).toList();

        // 你 shared-component 的方法是 (yearMonth, twse, tpex)
        // 但我們這裡 merged 已經混合 TWSE/TPEX，所以直接全塞到第一個 list，第二個給空即可
        // （shared service 內部會用 market 來組 PK）
        return stockRevenueService.upsertFromNormList(yearMonth, mapped, List.of());
    }
}
