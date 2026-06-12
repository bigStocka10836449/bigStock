package com.bigstock.schedule.service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.bigstock.sharedComponent.dto.SecuritiesFirmsRankResult;
import com.bigstock.sharedComponent.entity.StockInfo;
import com.bigstock.sharedComponent.service.SecuritiesFirmsDayOperateRankService;
import com.bigstock.sharedComponent.service.SecuritiesFirmsDayOperateService;
import com.bigstock.sharedComponent.service.SecuritiesFirmsRankRedisService;
import com.bigstock.sharedComponent.service.StockInfoService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SecuritiesFirmsRankPrecomputeService {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    private static final List<Integer> RANGE_DAYS_LIST = Arrays.asList(1, 3, 5, 10, 20, 60, 120);

    private static final int STOCK_BATCH_SIZE = 100;

    private final SecuritiesFirmsDayOperateService securitiesFirmsDayOperateService;

    private final SecuritiesFirmsDayOperateRankService rankService;

    private final SecuritiesFirmsRankRedisService rankRedisService;

    private final StockInfoService stockInfoService;

    private final RedissonClient redissonClient;

    @Async("securitiesFirmsRankTaskExecutor")
    public void precomputeAsync(String triggerSource) {
        precompute(triggerSource);
    }

    /**
     * 預先計算買賣日報表券商排行。
     *
     * 計算方式：
     * 1. 直接從買賣日報表資料表取得最新一個已存在資料的日期，不再要求當天資料一定完成。
     * 2. 撈出所有四碼 stockCode。
     * 3. 固定計算 1 / 3 / 5 / 10 / 20 / 60 / 120 天。
     * 4. 每批 100 支股票，用一次 SQL 批量計算該批股票的券商買賣超。
     * 5. 每支股票每個天數寫入 Redis。
     * 6. 全部完成後才標記實際計算日期 ready。
     */
    public void precompute(String triggerSource) {
        java.util.Date latestAvailableTradingDate = null;
        String endDateText = null;
        RLock lock = null;
        boolean locked = false;

        try {
            latestAvailableTradingDate = securitiesFirmsDayOperateService
                    .findLatestAvailableTradingDate("2330");

            if (latestAvailableTradingDate == null) {
                log.warn("No securities firms day operate data available. triggerSource={}", triggerSource);
                return;
            }

            endDateText = DATE_FORMAT.format(latestAvailableTradingDate);

            String lockKey = "sfdo:rank:precompute:lock:" + endDateText;
            lock = redissonClient.getLock(lockKey);

            locked = lock.tryLock(0, 120, TimeUnit.MINUTES);

            if (!locked) {
                log.info("Securities firms rank precompute skipped, lock exists. triggerSource={}, endDate={}",
                        triggerSource, endDateText);
                return;
            }

            log.info("Securities firms rank precompute use latest available trading date. triggerSource={}, endDate={}",
                    triggerSource, endDateText);

            List<String> stockCodes = stockInfoService.getAllStockInfo().stream()
                    .map(StockInfo::getStockCode)
                    .filter(stockCode -> stockCode != null && stockCode.trim().length() == 4)
                    .map(String::trim)
                    .distinct()
                    .sorted()
                    .toList();

            log.info("Securities firms rank precompute start. triggerSource={}, endDate={}, stockCount={}, rangeDays={}",
                    triggerSource, endDateText, stockCodes.size(), RANGE_DAYS_LIST);

            int totalWriteCount = 0;

            for (Integer rangeDays : RANGE_DAYS_LIST) {
                int rangeWriteCount = 0;

                for (int start = 0; start < stockCodes.size(); start += STOCK_BATCH_SIZE) {
                    int end = Math.min(start + STOCK_BATCH_SIZE, stockCodes.size());
                    List<String> batchStockCodes = new ArrayList<>(stockCodes.subList(start, end));

                    List<SecuritiesFirmsRankResult> results = rankService.calculateFixedRankBatch(
                            batchStockCodes,
                            rangeDays,
                            latestAvailableTradingDate
                    );

                    for (SecuritiesFirmsRankResult result : results) {
                        if ("DONE".equals(result.getStatus())) {
                            rankRedisService.putFixedRank(
                                    result.getStockCode(),
                                    result.getRangeDays(),
                                    endDateText,
                                    result
                            );
                            rangeWriteCount++;
                        }
                    }
                }

                totalWriteCount += rangeWriteCount;

                log.info("Securities firms rank precompute range finished. triggerSource={}, endDate={}, rangeDays={}, writeCount={}",
                        triggerSource, endDateText, rangeDays, rangeWriteCount);
            }

            rankRedisService.markDailyReady(endDateText);

            log.info("Securities firms rank precompute finished. triggerSource={}, endDate={}, totalWriteCount={}",
                    triggerSource, endDateText, totalWriteCount);

        } catch (Exception e) {
            log.error("Securities firms rank precompute failed. triggerSource={}, endDate={}",
                    triggerSource, endDateText, e);
        } finally {
            if (locked && lock != null && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
