package com.bigstock.biz.service;

import java.time.Duration;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bigstock.sharedComponent.dto.ThreeInstitutionalTradingResponse;
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
     * @throws Exception 
     */
    @Transactional(rollbackFor = { Exception.class })
    public StockThreeInstitutionalTradingService.SyncResult fetchToRedisAndSyncDb(String yyyyMMdd, Duration ttl) throws Exception {

        // ① 先抓資料 
        List<ThreeInstitutionalTradingResponse> twseThreeInstitutionalTradingResponses = ChromeDriverUtils.grabThreeInstiTwse(yyyyMMdd);
        List<ThreeInstitutionalTradingResponse> tpexThreeInstitutionalTradingResponses = ChromeDriverUtils.grabThreeInstiTpex(yyyyMMdd);
        // ② 同步寫入 DB（新增功能）
        return stockThreeInstiService.syncFromRedisToDb(yyyyMMdd, twseThreeInstitutionalTradingResponses, tpexThreeInstitutionalTradingResponses);
    }
    
//    public StockThreeInstitutionalTradingService.SyncResult fetchAndUpsertDb(
//            String yyyyMMdd,
//            boolean debugToRedis
//    ) {
//        // 1) 抓 + 解析成 norm list
//        List<ThreeInstitutionalTradingResponse> twseNorm =
//                ChromeDriverUtils.fetchThreeInstiTwseNorm(yyyyMMdd);
//
//        List<ThreeInstitutionalTradingResponse> tpexNorm =
//                ChromeDriverUtils.fetchThreeInstiTpexNorm(yyyyMMdd);
//
//        // 2) 直接寫 DB（upsert）
//        StockThreeInstitutionalTradingService.SyncResult result =
//        		stockThreeInstiService.upsertFromNormList(yyyyMMdd, twseNorm, tpexNorm);
//
//        // 3) Redis 旁路：debug / 重放
//        if (debugToRedis) {
//            // 你可以選擇只存 norm，或 raw+norm
//            // redisRepository.saveNormList("twse", yyyyMMdd, twseNorm, ttl);
//            // redisRepository.saveNormList("tpex", yyyyMMdd, tpexNorm, ttl);
//        }
//
//        return result;
//    }
}