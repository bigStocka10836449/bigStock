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

    /**
     * 每批股票數。
     * 100 是目前比較平衡的設定。
     * 不建議一開始就拉太大，避免單次 SQL 太重。
     */
    private static final int STOCK_BATCH_SIZE = 100;

    /**
     * 每批查詢後稍微暫停，避免連續打 DB。
     * 若你要測純速度，可以先改成 0。
     * 若正式環境想保守，可以維持 20。
     */
    private static final long BATCH_SLEEP_MILLIS = 20L;

    private final SecuritiesFirmsDayOperateService securitiesFirmsDayOperateService;

    private final SecuritiesFirmsDayOperateRankService rankService;

    private final SecuritiesFirmsRankRedisService rankRedisService;

    private final StockInfoService stockInfoService;

    private final RedissonClient redissonClient;

    @Async("securitiesFirmsRankTaskExecutor")
    public void precomputeAsync(String triggerSource) {
        precompute(triggerSource);
    }

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

            List<String> stockCodes = stockInfoService.getAllFourDigitStockCodes();

            if (stockCodes == null || stockCodes.isEmpty()) {
                log.warn("Securities firms rank precompute skipped, no stock codes found. triggerSource={}, endDate={}",
                        triggerSource, endDateText);
                return;
            }

            List<java.util.Date> maxTradingDates = rankService.findLatestTradingDates(
                    latestAvailableTradingDate,
                    120
            );

            if (maxTradingDates == null || maxTradingDates.isEmpty()) {
                log.warn("Securities firms rank precompute skipped, no trading dates found. triggerSource={}, endDate={}",
                        triggerSource, endDateText);
                return;
            }

            log.info("Securities firms rank precompute start. triggerSource={}, endDate={}, stockCount={}, rangeDays={}, maxTradingDays={}, batchSize={}, batchSleepMillis={}",
                    triggerSource,
                    endDateText,
                    stockCodes.size(),
                    RANGE_DAYS_LIST,
                    maxTradingDates.size(),
                    STOCK_BATCH_SIZE,
                    BATCH_SLEEP_MILLIS);

            int totalWriteCount = 0;

            for (int start = 0; start < stockCodes.size(); start += STOCK_BATCH_SIZE) {
                long batchStartMillis = System.currentTimeMillis();

                int end = Math.min(start + STOCK_BATCH_SIZE, stockCodes.size());
                List<String> batchStockCodes = new ArrayList<>(stockCodes.subList(start, end));

                List<SecuritiesFirmsRankResult> results = rankService.calculateFixedRankBatchAllRangesByTradingDates(
                        batchStockCodes,
                        RANGE_DAYS_LIST,
                        maxTradingDates
                );

                int batchWriteCount = 0;

                for (SecuritiesFirmsRankResult result : results) {
                    if ("DONE".equals(result.getStatus())) {
                        rankRedisService.putFixedRank(
                                result.getStockCode(),
                                result.getRangeDays(),
                                endDateText,
                                result
                        );
                        batchWriteCount++;
                    }
                }

                totalWriteCount += batchWriteCount;

                long batchCostMillis = System.currentTimeMillis() - batchStartMillis;

                log.info("Securities firms rank precompute batch finished. triggerSource={}, endDate={}, batchStart={}, batchEnd={}, batchSize={}, resultCount={}, writeCount={}, costMillis={}",
                        triggerSource,
                        endDateText,
                        start,
                        end,
                        batchStockCodes.size(),
                        results == null ? 0 : results.size(),
                        batchWriteCount,
                        batchCostMillis);

                sleepQuietly(BATCH_SLEEP_MILLIS);
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

    private void sleepQuietly(long millis) {
        if (millis <= 0) {
            return;
        }

        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Securities firms rank precompute interrupted", e);
        }
    }
}