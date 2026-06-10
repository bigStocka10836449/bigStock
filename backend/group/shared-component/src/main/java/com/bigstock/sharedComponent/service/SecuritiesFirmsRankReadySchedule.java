package com.bigstock.schedule.service;

import java.text.SimpleDateFormat;
import java.util.concurrent.TimeUnit;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.bigstock.sharedComponent.service.SecuritiesFirmsDayOperateService;
import com.bigstock.sharedComponent.service.SecuritiesFirmsRankRedisService;
import com.bigstock.sharedComponent.service.StockDayPriceService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SecuritiesFirmsRankReadySchedule {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    private final StockDayPriceService stockDayPriceService;

    private final SecuritiesFirmsDayOperateService securitiesFirmsDayOperateService;

    private final SecuritiesFirmsRankRedisService rankRedisService;

    private final RedissonClient redissonClient;

    /**
     * 每天晚上 21:30 確認買賣日報表是否已經匯入完成。
     *
     * 注意：
     * 這支不會預先計算所有股票。
     * 它只負責標記今天資料 ready。
     *
     * App 查詢時：
     * Redis 有資料 → 直接回
     * Redis 沒資料 → 只計算 app 指定的 stockId + rangeDays
     */
    @Scheduled(cron = "0 30 21 * * ?", zone = "Asia/Taipei")
    public void markSecuritiesFirmsRankReady() {
        java.util.Date currentTradeDate = stockDayPriceService.getCurrentTradeDate();
        String endDateText = DATE_FORMAT.format(currentTradeDate);

        String lockKey = "sfdo:rank:ready:lock:" + endDateText;
        RLock lock = redissonClient.getLock(lockKey);

        boolean locked = false;

        try {
            locked = lock.tryLock(0, 5, TimeUnit.MINUTES);

            if (!locked) {
                log.info("Securities firms rank ready schedule skipped, lock exists. endDate={}", endDateText);
                return;
            }

            boolean importFinished = securitiesFirmsDayOperateService
                    .chechIsFinishedWithTradingDate("2330", currentTradeDate);

            if (!importFinished) {
                log.warn("Securities firms day operate not ready at 21:30. endDate={}", endDateText);
                return;
            }

            rankRedisService.markDailyReady(endDateText);

            log.info("Securities firms rank daily ready marked. endDate={}", endDateText);

        } catch (Exception e) {
            log.error("Securities firms rank ready schedule failed. endDate={}", endDateText, e);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}