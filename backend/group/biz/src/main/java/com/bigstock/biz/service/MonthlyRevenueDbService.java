package com.bigstock.biz.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bigstock.sharedComponent.dto.MonthlyRevenueVo;
import com.bigstock.sharedComponent.dto.StockRevenueResponse;
import com.bigstock.sharedComponent.service.MonthlyRevenueQueryService;
import com.bigstock.sharedComponent.service.StockRevenueService;
import com.bigstock.sharedComponent.utils.MonthlyRevenueMapper;

import lombok.RequiredArgsConstructor;

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
            String yearMonth

    ) {
        List<MonthlyRevenueVo> merged =
                monthlyRevenueQueryService.fetchMonthlyRevenueToRedis(yearMonth);

        List<StockRevenueResponse> mapped =
                merged.stream().map(MonthlyRevenueMapper::toStockRevenueResponse).toList();

        // 你 shared-component 的方法是 (yearMonth, twse, tpex)
        // 但我們這裡 merged 已經混合 TWSE/TPEX，所以直接全塞到第一個 list，第二個給空即可
        // （shared service 內部會用 market 來組 PK）
        return stockRevenueService.upsertFromNormList(yearMonth, mapped, List.of());
    }
}
