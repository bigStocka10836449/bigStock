package com.bigstock.biz.service;

import java.time.Duration;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bigstock.biz.utils.ChromeDriverUtils;
import com.bigstock.sharedComponent.service.StockThreeInstitutionalTradingService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ThreeInstiService {

    private final org.redisson.api.RedissonClient redissonClient;

    // ✅ shared-component service：負責 Redis → DB
    private final StockThreeInstitutionalTradingService stockThreeInstiService;

    /**
     * 1) 抓 TWSE/TPEX 並寫入 Redis（raw/norm）
     * 2) 再從 Redis 讀 norm 寫入 DB（保留 Redis 不刪）
     *
     * 你現在想「結果保留到redis中，也順便寫入DB」就是做這件事。
     */
    @Transactional(rollbackFor = { Exception.class })
    public StockThreeInstitutionalTradingService.SyncResult fetchToRedisAndSyncDb(String yyyyMMdd, Duration ttl) {

        // ① 先照舊抓資料 + 寫 Redis（你原本功能）
        ChromeDriverUtils.cacheThreeInstiTwseAndTpex(redissonClient, yyyyMMdd, ttl);

        // ② 再把 Redis norm 同步寫入 DB（新增功能）
        return stockThreeInstiService.syncFromRedisToDb(yyyyMMdd);
    }
}