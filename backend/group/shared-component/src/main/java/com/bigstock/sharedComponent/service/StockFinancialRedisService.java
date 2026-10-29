package com.bigstock.sharedComponent.service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import com.bigstock.sharedComponent.dto.QuarterlyFinancialVo;
import com.bigstock.sharedComponent.entity.StockQuarterFinancial;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// 請沿用專案實際的 Entity / Service import
// import ...StockQuarterFinancial;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockFinancialRedisService {

    private static final String REDIS_PREFIX =
            "FIN:STOCK:FINANCIAL:";

    private static final TypeReference<List<QuarterlyFinancialVo>>
            FINANCIAL_LIST_TYPE = new TypeReference<>() {};

    private static final DefaultRedisScript<Long>
            MERGE_QUARTER_SCRIPT = createMergeScript();

    private final StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper;

    private final StockQuarterFinancialService
            stockQuarterFinancialService;


    // ============================================================
    // 1. 取得指定個股的完整歷史財報
    // ============================================================

    public List<QuarterlyFinancialVo> getStockFinancials(
            String market,
            String stockId
    ) {

        if (market == null || market.isBlank()
                || stockId == null || stockId.isBlank()) {

            return Collections.emptyList();
        }

        String redisKey = buildKey(market, stockId);

        String json = redisTemplate.opsForValue()
                .get(redisKey);

        if (json == null || json.isBlank()) {

            return Collections.emptyList();
        }

        try {

            List<QuarterlyFinancialVo> result =
                    objectMapper.readValue(
                            json,
                            FINANCIAL_LIST_TYPE
                    );

            if (result == null) {
                throw new IllegalStateException(
                        "Unexpected null financial JSON: " + redisKey
                );
            }

            return result;

        } catch (JsonProcessingException e) {

            throw new IllegalStateException(
                    "Failed to deserialize financial data: " + redisKey,
                    e
            );
        }
    }


    // ============================================================
    // 2. 讀取指定個股、指定季度
    //
    // Redis → PostgreSQL Fallback
    // 不使用 JVM Cache
    // ============================================================

    public QuarterlyFinancialVo readOne(
            int year,
            int quarter,
            String market,
            String stockId
    ) {

        if (quarter < 1 || quarter > 4
                || market == null || market.isBlank()
                || stockId == null || stockId.isBlank()) {

            return null;
        }

        String normalizedMarket =
                market.trim().toUpperCase(Locale.ROOT);

        String normalizedStockId =
                stockId.trim();

        // 1. Redis 個股歷史財報
        List<QuarterlyFinancialVo> history =
                getStockFinancials(
                        normalizedMarket,
                        normalizedStockId
                );

        for (QuarterlyFinancialVo financial : history) {

            if (financial == null) {
                continue;
            }

            if (Objects.equals(financial.getYear(), year)
                    && Objects.equals(financial.getQuarter(), quarter)) {

                return financial;
            }
        }

        // 2. PostgreSQL Fallback
        return stockQuarterFinancialService.findById(
                normalizedStockId,
                normalizedMarket,
                year,
                quarter
        )
        .map(this::toVo)
        .orElse(null);
    }


    // ============================================================
    // 3. 每日排程更新指定季度
    //
    // 相同季度：覆蓋
    // 不同季度：保留
    //
    // Redis Lua 確保單一 Key 的讀取、合併、寫入為原子操作
    // ============================================================

    public void mergeQuarter(
            int year,
            int quarter,
            String market,
            List<QuarterlyFinancialVo> financials
    ) {

        if (quarter < 1 || quarter > 4) {

            throw new IllegalArgumentException(
                    "Invalid quarter: " + quarter
            );
        }

        if (market == null || market.isBlank()) {

            throw new IllegalArgumentException(
                    "Market cannot be blank"
            );
        }

        if (financials == null || financials.isEmpty()) {

            log.info(
                    "[STOCK FINANCIAL] Skip empty data: market={}, year={}, quarter={}",
                    market,
                    year,
                    quarter
            );

            return;
        }

        long startTime = System.currentTimeMillis();

        String normalizedMarket =
                market.trim().toUpperCase(Locale.ROOT);

        // --------------------------------------------------------
        // STEP 1. 整理本次更新的股票
        // --------------------------------------------------------

        Map<String, QuarterlyFinancialVo> incoming =
                new LinkedHashMap<>();

        int skipped = 0;

        for (QuarterlyFinancialVo financial : financials) {

            if (!isValidIncoming(
                    financial,
                    year,
                    quarter,
                    normalizedMarket
            )) {

                skipped++;
                continue;
            }

            String redisKey = buildKey(
                    normalizedMarket,
                    financial.getStockId()
            );

            // 同一批如果重複出現相同股票，以最後一筆為準
            incoming.put(redisKey, financial);
        }

        if (incoming.isEmpty()) {
            return;
        }

        // --------------------------------------------------------
        // STEP 2. 依序原子合併每支股票
        // --------------------------------------------------------

        int updated = 0;

        for (Map.Entry<String, QuarterlyFinancialVo> entry
                : incoming.entrySet()) {

            String redisKey = entry.getKey();

            try {

                String incomingJson =
                        objectMapper.writeValueAsString(
                                entry.getValue()
                        );

                Long result = redisTemplate.execute(
                        MERGE_QUARTER_SCRIPT,
                        Collections.singletonList(redisKey),
                        incomingJson
                );

                if (result == null || result != 1L) {

                    throw new IllegalStateException(
                            "Redis merge failed: " + redisKey
                    );
                }

                updated++;

            } catch (JsonProcessingException e) {

                throw new IllegalStateException(
                        "Failed to serialize financial: " + redisKey,
                        e
                );
            }
        }

        // --------------------------------------------------------
        // STEP 3. 紀錄更新結果
        // --------------------------------------------------------

        log.info(
                "[STOCK FINANCIAL] Merge completed: market={}, year={}, quarter={}, updated={}, skipped={}, elapsed={}ms",
                normalizedMarket,
                year,
                quarter,
                updated,
                skipped,
                System.currentTimeMillis() - startTime
        );
    }


    // ============================================================
    // 4. Redis Lua Script
    //
    // 將指定季度資料合併到既有歷史 JSON Array
    // ============================================================

    private static DefaultRedisScript<Long> createMergeScript() {

        String lua = """
                local key = KEYS[1]
                local incoming = cjson.decode(ARGV[1])

                local existing = redis.call('GET', key)
                local history = {}

                if existing then
                    history = cjson.decode(existing)

                    if type(history) ~= 'table' then
                        return redis.error_reply(
                            'Invalid financial history JSON'
                        )
                    end
                end

                local updated = false

                for i = 1, #history do

                    local item = history[i]

                    if item.year == incoming.year
                        and item.quarter == incoming.quarter then

                        history[i] = incoming
                        updated = true
                        break
                    end
                end

                if not updated then
                    table.insert(history, incoming)
                end

                table.sort(history, function(a, b)

                    if a.year == b.year then
                        return a.quarter < b.quarter
                    end

                    return a.year < b.year
                end)

                redis.call(
                    'SET',
                    key,
                    cjson.encode(history)
                )

                return 1
                """;

        DefaultRedisScript<Long> script =
                new DefaultRedisScript<>();

        script.setScriptText(lua);

        script.setResultType(Long.class);

        return script;
    }


    // ============================================================
    // 5. 驗證更新資料
    // ============================================================

    private boolean isValidIncoming(
            QuarterlyFinancialVo financial,
            int year,
            int quarter,
            String market
    ) {

        return financial != null
                && financial.getStockId() != null
                && !financial.getStockId().isBlank()
                && financial.getMarket() != null
                && market.equalsIgnoreCase(
                        financial.getMarket()
                )
                && Objects.equals(
                        financial.getYear(),
                        year
                )
                && Objects.equals(
                        financial.getQuarter(),
                        quarter
                );
    }


    // ============================================================
    // 6. Redis Key
    // ============================================================

    private String buildKey(
            String market,
            String stockId
    ) {

        if (market == null || market.isBlank()
                || stockId == null || stockId.isBlank()) {

            throw new IllegalArgumentException(
                    "market and stockId are required"
            );
        }

        return REDIS_PREFIX
                + market.trim().toUpperCase(Locale.ROOT)
                + ":"
                + stockId.trim();
    }


    // ============================================================
    // 7. Entity → QuarterlyFinancialVo
    // ============================================================

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


    private Double toDouble(BigDecimal value) {

        return value == null
                ? null
                : value.doubleValue();
    }
}