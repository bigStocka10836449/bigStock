package com.bigstock.sharedComponent.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.bigstock.sharedComponent.dto.QuarterlyFinancialResponse;
import com.bigstock.sharedComponent.dto.QuarterlyFinancialVo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuarterlyFinancialRedisService
        implements ApplicationRunner {

    private final StringRedisTemplate redis;

    private final ObjectMapper objectMapper;

    private final StockQuarterFinancialService
            stockQuarterFinancialService;
    
    private final StockFinancialRedisService stockFinancialRedisService;
    

    private static final String KEY_PATTERN = "FIN:QTR:*";

    private static final int BATCH_SIZE = 100;

    private static final TypeReference<List<QuarterlyFinancialVo>>
            FINANCIAL_LIST_TYPE =
            new TypeReference<List<QuarterlyFinancialVo>>() {};

    /**
     * JVM Memory Cache
     *
     * Key:
     * FIN:QTR:2026Q2:TPEX
     *
     * Value:
     * Map<stockId, QuarterlyFinancialVo>
     */
    private final ConcurrentHashMap<String, FinancialSnapshot>
            memoryCache = new ConcurrentHashMap<>();


    /**
     * Single-node locking.
     */
    private final ConcurrentHashMap<String, Object>
            keyLocks = new ConcurrentHashMap<>();


    // ============================================================
    // Redis Key
    // ============================================================

    private static String key(
            int year,
            int quarter,
            String market) {

        return "FIN:QTR:"
                + year
                + "Q"
                + quarter
                + ":"
                + market.trim().toUpperCase(Locale.ROOT);
    }


    // ============================================================
    // Snapshot
    // ============================================================

    private static final class FinancialSnapshot {

        private final Map<String, QuarterlyFinancialVo> stockMap;

        private FinancialSnapshot(
                Map<String, QuarterlyFinancialVo> stockMap) {

            this.stockMap = Collections.unmodifiableMap(
                    new HashMap<>(stockMap)
            );
        }



        private List<QuarterlyFinancialVo> toList() {
            return new ArrayList<>(stockMap.values());
        }

        private int size() {
            return stockMap.size();
        }

    }


    // ============================================================
    // Build Snapshot
    // ============================================================

    private FinancialSnapshot buildSnapshot(
            List<QuarterlyFinancialVo> list) {

        Map<String, QuarterlyFinancialVo> stockMap =
                new HashMap<>(
                        Math.max(
                                16,
                                list == null ? 16 : list.size() * 2
                        )
                );

        if (list != null) {

            for (QuarterlyFinancialVo vo : list) {

                if (vo == null || vo.getStockId() == null) {
                    continue;
                }

                String stockId = vo.getStockId().trim();

                if (!stockId.isEmpty()) {
                    stockMap.put(stockId, vo);
                }
            }
        }

        return new FinancialSnapshot(stockMap);
    }


    // ============================================================
    // Application Startup
    // ============================================================

    @Override
    public void run(ApplicationArguments args) {

        log.info(
                "[FINANCIAL] Starting quarterly financial preload"
        );

        long start = System.currentTimeMillis();

        preload();

        log.info(
                "[FINANCIAL] Preload completed, quarters={}, elapsed={}ms",
                memoryCache.size(),
                System.currentTimeMillis() - start
        );
    }


    // ============================================================
    // PRELOAD
    //
    // 1. SCAN Redis keys
    // 2. Batch MGET
    // 3. Deserialize
    // 4. Build snapshots
    // 5. Publish into JVM
    // ============================================================

    public void preload() {

        List<String> batch = new ArrayList<>(BATCH_SIZE);

        ScanOptions options = ScanOptions.scanOptions()
                .match(KEY_PATTERN)
                .count(200)
                .build();

        int totalStocks = 0;

        /*
         * Redis SCAN.
         *
         * Avoid Redis KEYS in production.
         */
        try (Cursor<String> cursor = redis.scan(options)) {

            while (cursor.hasNext()) {

                String redisKey = cursor.next();

                batch.add(redisKey);

                if (batch.size() >= BATCH_SIZE) {

                    totalStocks += preloadBatch(batch);

                    batch.clear();
                }
            }

            /*
             * Process remaining keys.
             */
            if (!batch.isEmpty()) {

                totalStocks += preloadBatch(batch);

                batch.clear();
            }

        } catch (Exception e) {

            log.error(
                    "[FINANCIAL] Preload failed",
                    e
            );

            /*
             * Fail startup rather than silently
             * serving an incompletely initialized cache.
             */
            throw new IllegalStateException(
                    "Quarterly financial preload failed",
                    e
            );
        }

        log.info(
                "[FINANCIAL] Preloaded quarterly financial records={}",
                totalStocks
        );
    }


    // ============================================================
    // PRELOAD BATCH
    // ============================================================

    private int preloadBatch(List<String> keys) {

        if (keys.isEmpty()) {
            return 0;
        }

        /*
         * MGET:
         *
         * One Redis call for multiple quarterly keys.
         */
        List<String> jsonList =
                redis.opsForValue().multiGet(keys);

        if (jsonList == null ||
                jsonList.size() != keys.size()) {

            throw new IllegalStateException(
                    "Redis MGET returned unexpected result"
            );
        }

        int total = 0;

        for (int i = 0; i < keys.size(); i++) {

            String redisKey = keys.get(i);

            String json = jsonList.get(i);

            if (json == null || json.isBlank()) {
                continue;
            }

            try {

                List<QuarterlyFinancialVo> list =
                        objectMapper.readValue(
                                json,
                                FINANCIAL_LIST_TYPE
                        );

                FinancialSnapshot snapshot =
                        buildSnapshot(list);

                /*
                 * Publish entire snapshot.
                 *
                 * Synchronize with write() and
                 * cache-miss loading for this key.
                 */
                Object lock = keyLocks.computeIfAbsent(
                        redisKey,
                        k -> new Object()
                );

                synchronized (lock) {
                    memoryCache.putIfAbsent(
                            redisKey,
                            snapshot
                    );
                }

                total += snapshot.size();

                log.debug(
                        "[FINANCIAL] Loaded key={}, stocks={}",
                        redisKey,
                        snapshot.size()
                );

            } catch (Exception e) {

                throw new IllegalStateException(
                        "Failed to preload key: " + redisKey,
                        e
                );
            }
        }

        return total;
    }


    // ============================================================
    // WRITE
    //
    // Redis + JVM + PostgreSQL
    // ============================================================

    public void write(
            int year,
            int quarter,
            String market,
            List<QuarterlyFinancialVo> list) {

        String redisKey = key(year, quarter, market);

        List<QuarterlyFinancialVo> data =
                list == null
                        ? Collections.emptyList()
                        : new ArrayList<>(list);

        FinancialSnapshot newSnapshot =
                buildSnapshot(data);

        Object lock = keyLocks.computeIfAbsent(
                redisKey,
                k -> new Object()
        );

        try {

            String json =
                    objectMapper.writeValueAsString(data);

            synchronized (lock) {

                /*
                 * Redis SET without TTL.
                 *
                 * Overwrites the old quarterly snapshot.
                 */
                redis.opsForValue().set(
                        redisKey,
                        json
                );

                /*
                 * Replace entire JVM snapshot.
                 */
                memoryCache.put(
                        redisKey,
                        newSnapshot
                );
            }

            /*
             * Keep original PostgreSQL upsert.
             */
            if (!data.isEmpty()) {

                List<QuarterlyFinancialResponse> dtoList =
                        data.stream()
                                .map(v -> toResponse(
                                        year,
                                        quarter,
                                        market,
                                        v
                                ))
                                .collect(Collectors.toList());

                stockQuarterFinancialService
                        .upsertFromNormList(dtoList);
            }

            log.info(
                    "[FINANCIAL] Updated key={}, stocks={}",
                    redisKey,
                    newSnapshot.size()
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "write quarterly financial failed: "
                            + redisKey,
                    e
            );
        }
    }


    public QuarterlyFinancialVo readOne(
            int year,
            int quarter,
            String market,
            String stockId
    ) {

        return stockFinancialRedisService.readOne(
                year,
                quarter,
                market,
                stockId
        );
    }

    


    // ============================================================
    // READ ALL
    // ============================================================

    public List<QuarterlyFinancialVo> read(
            int year,
            int quarter,
            String market) {

        FinancialSnapshot snapshot =
                getOrLoadSnapshot(
                        key(year, quarter, market)
                );

        return snapshot.toList();
    }


 // ============================================================
 // Memory First, Redis Fallback
 // ============================================================

	private FinancialSnapshot getOrLoadSnapshot(String redisKey) {

		// =====================================================
		// 1. Fast Path：JVM Memory
		// =====================================================

		FinancialSnapshot snapshot = memoryCache.get(redisKey);

		if (snapshot != null) {
			return snapshot;
		}

		// =====================================================
		// 2. Slow Path：Redis
		// =====================================================

		Object lock = keyLocks.computeIfAbsent(redisKey, k -> new Object());

		synchronized (lock) {

			// Double-check
			snapshot = memoryCache.get(redisKey);

			if (snapshot != null) {
				return snapshot;
			}

			try {

				String json = redis.opsForValue().get(redisKey);

				if (json == null || json.isBlank()) {

					/*
					 * Redis 沒資料：
					 *
					 * 暫時保存 Empty Snapshot， 避免每次查詢都重新讀 Redis。
					 */
					snapshot = buildSnapshot(Collections.emptyList());

					memoryCache.put(redisKey, snapshot);

					return snapshot;
				}

				List<QuarterlyFinancialVo> list = objectMapper.readValue(json, FINANCIAL_LIST_TYPE);

				snapshot = buildSnapshot(list);

				memoryCache.put(redisKey, snapshot);

				return snapshot;

			} catch (Exception e) {

				throw new RuntimeException("load quarterly financial failed: " + redisKey, e);
			}
		}
	}


    // ============================================================
    // Manual Evict
    // ============================================================

    public void evict(
            int year,
            int quarter,
            String market) {

        String redisKey = key(year, quarter, market);

        Object lock = keyLocks.computeIfAbsent(
                redisKey,
                k -> new Object()
        );

        synchronized (lock) {
            memoryCache.remove(redisKey);
        }
    }

    // ============================================================
    // Keep your existing toResponse() below this line.
    // ============================================================
    // ============================================================
    // VO -> Response
    // ============================================================

    private static QuarterlyFinancialResponse toResponse(
            int year,
            int quarter,
            String market,
            QuarterlyFinancialVo v) {

        QuarterlyFinancialResponse d =
                new QuarterlyFinancialResponse();

        d.setStockId(v.getStockId());
        d.setStockName(v.getStockName());

        d.setMarket(
                market == null
                        ? null
                        : market.trim().toUpperCase(Locale.ROOT)
        );

        d.setYear(year);
        d.setQuarter(quarter);

        d.setPeriodStartMonth(v.getPeriodStartMonth());
        d.setPeriodEndMonth(v.getPeriodEndMonth());

        d.setUnit(
                v.getUnit() == null
                        ? "TWD"
                        : v.getUnit()
        );

        d.setOperatingRevenue(v.getOperatingRevenue());
        d.setOperatingProfit(v.getOperatingProfit());

        d.setNonOperatingIncomeExpense(
                v.getNonOperatingIncomeExpense()
        );

        d.setNetProfitAfterTax(v.getNetProfitAfterTax());

        d.setCapitalStockEndPeriod(
                v.getCapitalStockEndPeriod()
        );

        d.setEarningsPerShare(v.getEarningsPerShare());

        d.setNetAssetValuePerShare(
                v.getNetAssetValuePerShare()
        );

        d.setQuickRatio(v.getQuickRatio());
        d.setCurrentRatio(v.getCurrentRatio());
        d.setDepn(v.getDepn());

        return d;
    }
    
}