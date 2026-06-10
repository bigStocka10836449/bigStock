package com.bigstock.biz.service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import com.bigstock.sharedComponent.dto.SecuritiesFirmsRankResult;
import com.bigstock.sharedComponent.service.SecuritiesFirmsDayOperateRankService;
import com.bigstock.sharedComponent.service.SecuritiesFirmsRankRedisService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SecuritiesFirmsRankQueryService {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    private final SecuritiesFirmsRankRedisService rankRedisService;

    private final SecuritiesFirmsDayOperateRankService rankService;

    private final RedissonClient redissonClient;

    public SecuritiesFirmsRankResult getFixedRank(String stockCode, Integer rangeDays) {
        String normalizedStockCode = normalizeStockCode(stockCode);

        if (normalizedStockCode == null || normalizedStockCode.isBlank()) {
            return buildFailedResult(stockCode, rangeDays, "stockCode is blank");
        }

        if (!rankService.isSupportedRangeDays(rangeDays)) {
            return buildFailedResult(normalizedStockCode, rangeDays, "Unsupported rangeDays: " + rangeDays);
        }

        Optional<String> latestEndDateOptional = rankRedisService.getLatestEndDate();

        if (latestEndDateOptional.isEmpty()) {
            return buildNotReadyResult(normalizedStockCode, rangeDays, null,
                    "latestEndDate not found. Daily ready schedule may not be executed yet.");
        }

        String endDate = latestEndDateOptional.get();

        if (!rankRedisService.isDailyReady(endDate)) {
            return buildNotReadyResult(normalizedStockCode, rangeDays, endDate,
                    "Securities firms day operate data is not ready.");
        }

        Optional<SecuritiesFirmsRankResult> cached =
                rankRedisService.getFixedRank(normalizedStockCode, rangeDays, endDate);

        if (cached.isPresent()) {
            return cached.get();
        }

        String lockKey = "sfdo:rank:lock:" + normalizedStockCode + ":" + rangeDays + ":" + endDate;
        RLock lock = redissonClient.getLock(lockKey);

        boolean locked = false;

        try {
            locked = lock.tryLock(0, 30, TimeUnit.SECONDS);

            if (!locked) {
                return waitAndGetCache(normalizedStockCode, rangeDays, endDate);
            }

            Optional<SecuritiesFirmsRankResult> cachedAfterLock =
                    rankRedisService.getFixedRank(normalizedStockCode, rangeDays, endDate);

            if (cachedAfterLock.isPresent()) {
                return cachedAfterLock.get();
            }

            java.util.Date parsedEndDate = DATE_FORMAT.parse(endDate);

            SecuritiesFirmsRankResult result = rankService.calculateFixedRank(
                    normalizedStockCode,
                    rangeDays,
                    parsedEndDate
            );

            if ("DONE".equals(result.getStatus())) {
                rankRedisService.putFixedRank(normalizedStockCode, rangeDays, endDate, result);
            }

            return result;

        } catch (ParseException e) {
            log.error("Parse endDate failed. endDate={}", endDate, e);
            return buildFailedResult(normalizedStockCode, rangeDays, "Parse endDate failed: " + endDate);
        } catch (Exception e) {
            log.error("Get securities firms rank failed. stockCode={}, rangeDays={}, endDate={}",
                    normalizedStockCode, rangeDays, endDate, e);
            return buildFailedResult(normalizedStockCode, rangeDays, e.getMessage());
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private SecuritiesFirmsRankResult waitAndGetCache(String stockCode, Integer rangeDays, String endDate) {
        try {
            for (int i = 0; i < 3; i++) {
                Thread.sleep(300);

                Optional<SecuritiesFirmsRankResult> cached =
                        rankRedisService.getFixedRank(stockCode, rangeDays, endDate);

                if (cached.isPresent()) {
                    return cached.get();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        SecuritiesFirmsRankResult processing = new SecuritiesFirmsRankResult();
        processing.setStatus("PROCESSING");
        processing.setStockCode(stockCode);
        processing.setRangeType("FIXED");
        processing.setRangeDays(rangeDays);
        processing.setEndDate(endDate);
        processing.setSource("REDIS");
        processing.setMessage("Another request is calculating this rank. Please retry later.");
        return processing;
    }

    private String normalizeStockCode(String stockCode) {
        if (stockCode == null) {
            return null;
        }

        String trimmed = stockCode.trim();

        if (trimmed.contains("_")) {
            return trimmed.split("_")[0];
        }

        return trimmed;
    }

    private SecuritiesFirmsRankResult buildNotReadyResult(
            String stockCode,
            Integer rangeDays,
            String endDate,
            String message
    ) {
        SecuritiesFirmsRankResult result = new SecuritiesFirmsRankResult();
        result.setStatus("NOT_READY");
        result.setStockCode(stockCode);
        result.setRangeType("FIXED");
        result.setRangeDays(rangeDays);
        result.setEndDate(endDate);
        result.setSource("REDIS");
        result.setMessage(message);
        return result;
    }

    private SecuritiesFirmsRankResult buildFailedResult(String stockCode, Integer rangeDays, String message) {
        SecuritiesFirmsRankResult result = new SecuritiesFirmsRankResult();
        result.setStatus("FAILED");
        result.setStockCode(stockCode);
        result.setRangeType("FIXED");
        result.setRangeDays(rangeDays);
        result.setSource("SERVER");
        result.setMessage(message);
        return result;
    }
}