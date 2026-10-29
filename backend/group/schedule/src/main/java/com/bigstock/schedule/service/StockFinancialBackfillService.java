package com.bigstock.schedule.service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.bigstock.sharedComponent.dto.QuarterlyFinancialVo;
import com.bigstock.sharedComponent.entity.StockQuarterFinancial;
import com.bigstock.sharedComponent.repository.StockQuarterFinancialRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 一次性從db撈取現有每季度eps資訊回填到Redis
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockFinancialBackfillService {

    private static final String REDIS_KEY_PREFIX =
            "FIN:STOCK:FINANCIAL:";

    /**
     * Redis Pipeline 每批寫入的股票數量。
     *
     * 避免一次送入過多命令，
     * 造成 Redis Client 累積過多回覆。
     */
    private static final int PIPELINE_BATCH_SIZE = 500;

    private final StockQuarterFinancialRepository repository;

    private final StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper;


    /**
     * 一次性補入所有股票歷史季度財報。
     *
     * PostgreSQL：
     *   只執行一次 findAll()
     *
     * Redis：
     *   FIN:STOCK:FINANCIAL:{market}:{stockId}
     *
     * Value：
     *   List<QuarterlyFinancialVo> JSON
     *
     * 注意：
     *   這是一次性 Backfill，不會自動排程。
     */
//    @PostConstruct
    public void backfillAllStockFinancials() {

        long startTime = System.currentTimeMillis();

        log.info("[FINANCIAL BACKFILL] Started");

        // ====================================================
        // STEP 1. 一次從 PostgreSQL 取得所有季度財報
        // ====================================================

        List<StockQuarterFinancial> allFinancials =
                repository.findAll();

        if (allFinancials == null || allFinancials.isEmpty()) {

            log.warn(
                    "[FINANCIAL BACKFILL] No financial records found"
            );

            return;
        }

        log.info(
                "[FINANCIAL BACKFILL] Loaded {} records",
                allFinancials.size()
        );


        // ====================================================
        // STEP 2. 依照 market + stockId 分組
        // ====================================================

        Map<String, List<StockQuarterFinancial>> grouped =
                new HashMap<>();

        int skippedRecords = 0;

        for (StockQuarterFinancial financial : allFinancials) {

            if (!isValidFinancial(financial)) {

                skippedRecords++;

                continue;
            }

            String redisKey = buildRedisKey(
                    financial.getMarket(),
                    financial.getStockId()
            );

            grouped.computeIfAbsent(
                    redisKey,
                    key -> new ArrayList<>()
            ).add(financial);
        }

        log.info(
                "[FINANCIAL BACKFILL] Grouped stocks={}, skippedRecords={}",
                grouped.size(),
                skippedRecords
        );


        // ====================================================
        // STEP 3. 轉換為 QuarterlyFinancialVo
        // ====================================================

        Map<String, String> redisData =
                new HashMap<>(grouped.size());

        int totalQuarters = 0;

        for (Map.Entry<String, List<StockQuarterFinancial>> entry
                : grouped.entrySet()) {

            String redisKey = entry.getKey();

            List<StockQuarterFinancial> records =
                    entry.getValue();

            // 依照年份、季度由舊到新排序
            records.sort(
                    Comparator
                            .comparing(
                                    StockQuarterFinancial::getYear
                            )
                            .thenComparing(
                                    StockQuarterFinancial::getQuarter
                            )
            );

            List<QuarterlyFinancialVo> financialVoList =
                    new ArrayList<>(records.size());

            for (StockQuarterFinancial financial : records) {

                financialVoList.add(
                        toVo(financial)
                );
            }

            try {

                String json =
                        objectMapper.writeValueAsString(
                                financialVoList
                        );

                redisData.put(redisKey, json);

                totalQuarters += financialVoList.size();

            } catch (JsonProcessingException e) {

                throw new IllegalStateException(
                        "Failed to serialize financial data: "
                                + redisKey,
                        e
                );
            }
        }


        // ====================================================
        // STEP 4. Redis Pipeline 分批寫入
        // ====================================================

        List<Map.Entry<String, String>> entries =
                new ArrayList<>(redisData.entrySet());

        int successCount = 0;

        for (int start = 0;
             start < entries.size();
             start += PIPELINE_BATCH_SIZE) {

            int end = Math.min(
                    start + PIPELINE_BATCH_SIZE,
                    entries.size()
            );

            List<Map.Entry<String, String>> batch =
                    entries.subList(start, end);

            writePipeline(batch);

            successCount += batch.size();

            log.info(
                    "[FINANCIAL BACKFILL] Redis written {}/{} stocks",
                    successCount,
                    entries.size()
            );
        }


        // ====================================================
        // STEP 5. 完成
        // ====================================================

        long elapsed =
                System.currentTimeMillis() - startTime;

        log.info(
                "[FINANCIAL BACKFILL] Completed: "
                        + "records={}, stocks={}, quarters={}, "
                        + "skippedRecords={}, elapsed={}ms",
                allFinancials.size(),
                successCount,
                totalQuarters,
                skippedRecords,
                elapsed
        );
    }


    /**
     * 分批 Pipeline 寫入 Redis。
     *
     * 每一支股票透過單次 SET 更新 JSON。
     * 不設定 TTL，長期保存。
     */
    private void writePipeline(
            List<Map.Entry<String, String>> batch
    ) {

        redisTemplate.executePipelined(
                (RedisCallback<Object>) connection -> {

                    for (Map.Entry<String, String> entry : batch) {

                        byte[] key =
                                entry.getKey().getBytes(
                                        StandardCharsets.UTF_8
                                );

                        byte[] value =
                                entry.getValue().getBytes(
                                        StandardCharsets.UTF_8
                                );

                        connection.stringCommands().set(
                                key,
                                value
                        );
                    }

                    return null;
                }
        );
    }


    /**
     * 判斷季度財報的必要資料是否完整。
     */
    private boolean isValidFinancial(
            StockQuarterFinancial financial
    ) {

        if (financial == null) {
            return false;
        }

        if (financial.getStockId() == null
                || financial.getStockId().isBlank()) {
            return false;
        }

        if (financial.getMarket() == null
                || financial.getMarket().isBlank()) {
            return false;
        }

        if (financial.getYear() == null
                || financial.getQuarter() == null) {
            return false;
        }

        return financial.getQuarter() >= 1
                && financial.getQuarter() <= 4;
    }


    /**
     * 建立 Redis Key。
     */
    private String buildRedisKey(
            String market,
            String stockId
    ) {

        return REDIS_KEY_PREFIX
                + market.trim().toUpperCase(Locale.ROOT)
                + ":"
                + stockId.trim();
    }


    /**
     * Entity -> QuarterlyFinancialVo
     *
     * 保留原本所有財務欄位。
     *
     * earningsPerShare 保持 PostgreSQL 原始值，
     * 不在 Backfill 時轉換成單季 EPS。
     */
    private QuarterlyFinancialVo toVo(
            StockQuarterFinancial entity
    ) {

        QuarterlyFinancialVo vo =
                new QuarterlyFinancialVo();

        vo.setStockId(entity.getStockId());

        vo.setStockName(entity.getStockName());

        vo.setMarket(entity.getMarket());

        vo.setYear(entity.getYear());

        vo.setQuarter(entity.getQuarter());

        vo.setPeriodStartMonth(
                entity.getPeriodStartMonth()
        );

        vo.setPeriodEndMonth(
                entity.getPeriodEndMonth()
        );

        vo.setUnit(entity.getUnit());

        vo.setOperatingRevenue(
                entity.getOperatingRevenue()
        );

        vo.setOperatingProfit(
                entity.getOperatingProfit()
        );

        vo.setNonOperatingIncomeExpense(
                entity.getNonOperatingIncomeExpense()
        );

        vo.setNetProfitAfterTax(
                entity.getNetProfitAfterTax()
        );

        vo.setCapitalStockEndPeriod(
                entity.getCapitalStockEndPeriod()
        );

        vo.setEarningsPerShare(
                toDouble(entity.getEarningsPerShare())
        );

        vo.setNetAssetValuePerShare(
                toDouble(entity.getNetAssetValuePerShare())
        );

        vo.setQuickRatio(
                toDouble(entity.getQuickRatio())
        );

        vo.setCurrentRatio(
                toDouble(entity.getCurrentRatio())
        );

        vo.setDepn(
                toDouble(entity.getDepn())
        );

        return vo;
    }


    /**
     * BigDecimal -> Double
     */
    private Double toDouble(BigDecimal value) {

        return value == null
                ? null
                : value.doubleValue();
    }
}
