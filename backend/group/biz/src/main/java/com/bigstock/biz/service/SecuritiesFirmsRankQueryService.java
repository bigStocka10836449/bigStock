package com.bigstock.biz.service;

import java.util.Optional;

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

    private final SecuritiesFirmsRankRedisService rankRedisService;

    private final SecuritiesFirmsDayOperateRankService rankService;

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
                    "latestEndDate not found. Daily precompute schedule may not be executed yet.");
        }

        String endDate = latestEndDateOptional.get();

        if (!rankRedisService.isDailyReady(endDate)) {
            return buildNotReadyResult(normalizedStockCode, rangeDays, endDate,
                    "Securities firms rank precompute is not ready.");
        }

        Optional<SecuritiesFirmsRankResult> cached =
                rankRedisService.getFixedRank(normalizedStockCode, rangeDays, endDate);

        if (cached.isPresent()) {
            return cached.get();
        }

        return buildNotReadyResult(normalizedStockCode, rangeDays, endDate,
                "Securities firms rank cache not found.");
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
