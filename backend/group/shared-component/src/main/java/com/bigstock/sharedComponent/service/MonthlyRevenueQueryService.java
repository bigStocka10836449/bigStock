package com.bigstock.sharedComponent.service;

import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.bigstock.sharedComponent.client.TpexMonthlyRevenueClient;
import com.bigstock.sharedComponent.client.TwseMonthlyRevenueClient;
import com.bigstock.sharedComponent.dto.MonthlyRevenueVo;
import com.bigstock.sharedComponent.dto.StockRevenueResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonthlyRevenueQueryService {

    private final TwseMonthlyRevenueClient twseClient;

    private final TpexMonthlyRevenueClient tpexClient;

    private final MonthlyRevenueRedisService redisService;

    private final StockRevenueService stockRevenueService;

    private static final int RECENT_MONTHS = 24;

    private static final ZoneId TAIPEI_ZONE =
            ZoneId.of("Asia/Taipei");

    // ============================================================
    // FETCH OFFICIAL MONTHLY REVENUE
    // ============================================================

    public List<MonthlyRevenueVo> fetchMonthlyRevenueToRedis(
            String yearMonth) {

        String ym = normalizeYearMonth(yearMonth);

        List<MonthlyRevenueVo> merged =
                new ArrayList<>();

        merged.addAll(
                twseClient.fetchMonthlyRevenue(ym)
        );

        merged.addAll(
                tpexClient.fetchMonthlyRevenue(ym)
        );

        /*
         * 沿用你目前的邏輯：
         * 此方法只合併官方資料。
         *
         * 若要在這個入口同步寫入 Redis，
         * 請使用 redisService.write(ym, merged)。
         */
        return merged;
    }

    // ============================================================
    // READ SINGLE MONTH
    // ============================================================

    public List<MonthlyRevenueVo> readMonthlyRevenueFromRedis(
            String yearMonth) {

        String ym = normalizeYearMonth(yearMonth);

        return redisService.read(ym);
    }
    
    /**
     * 取得指定股票最近 24 個已結束月份的營收。
     *
     * Redis:
     * - 有資料：直接從全市場清單找出指定股票
     * - 沒資料：記錄缺少的月份
     *
     * PostgreSQL:
     * - 一次查詢缺少月份的全市場營收
     * - 回填 Redis 全市場 Snapshot
     *
     * API:
     * 只回傳指定股票。
     */
    public Map<String, MonthlyRevenueVo> readRecentStockRevenue(
            String stockId) {

        if (stockId == null || stockId.trim().isEmpty()) {
            throw new IllegalArgumentException("stockId is required");
        }

        String targetStockId = stockId.trim();

        YearMonth latestMonth =
                YearMonth.now(TAIPEI_ZONE).minusMonths(1);

        Map<String, MonthlyRevenueVo> result =
                new LinkedHashMap<>();

        List<String> missingMonths =
                new ArrayList<>();

        // ========================================================
        // 1. Redis First
        // ========================================================

        for (int i = 0; i < RECENT_MONTHS; i++) {

            String yearMonth =
                    latestMonth.minusMonths(i).toString();

            String json =
                    stockRevenueService.getMonthlyJson(yearMonth);

            if (json == null) {

                result.put(yearMonth, null);
                missingMonths.add(yearMonth);

                continue;
            }

            List<StockRevenueResponse> redisList =
                    stockRevenueService.parseMonthlyJson(
                            yearMonth,
                            json
                    );

            result.put(
                    yearMonth,
                    findStockRevenue(redisList, targetStockId)
            );
        }

        // ========================================================
        // 2. PostgreSQL Fallback
        // ========================================================

        if (!missingMonths.isEmpty()) {

            /*
             * 注意：
             * 這裡仍然查詢缺少月份的全市場營收，
             * 因為 Redis 要回填完整月份 Snapshot。
             */
            Map<String, List<StockRevenueResponse>> dbData =
                    stockRevenueService.findMonthlyRevenueFromDb(
                            missingMonths
                    );

            for (String yearMonth : missingMonths) {

                List<StockRevenueResponse> dbList =
                        dbData.getOrDefault(
                                yearMonth,
                                Collections.emptyList()
                        );

                if (dbList.isEmpty()) {
                    continue;
                }

                boolean written =
                        stockRevenueService.saveMonthlyListIfAbsent(
                                yearMonth,
                                dbList
                        );

                List<StockRevenueResponse> effectiveList = dbList;

                if (!written) {

                    /*
                     * Redis 可能已經被其他 Thread 更新。
                     * 重新讀取。
                     */
                    String latestJson =
                            stockRevenueService.getMonthlyJson(
                                    yearMonth
                            );

                    if (latestJson != null) {

                        effectiveList =
                                stockRevenueService.parseMonthlyJson(
                                        yearMonth,
                                        latestJson
                                );
                    }
                }

                result.put(
                        yearMonth,
                        findStockRevenue(
                                effectiveList,
                                targetStockId
                        )
                );
            }
        }

        return result;
    }
    

    // ============================================================
    // READ RECENT 24 MONTHS
    //
    // Redis → PostgreSQL → Redis Backfill
    // ============================================================

    public Map<String, List<MonthlyRevenueVo>> readRecentRevenue() {

        YearMonth latestMonth =
                YearMonth.now(TAIPEI_ZONE)
                        .minusMonths(1);

        Map<String, List<MonthlyRevenueVo>> result =
                new LinkedHashMap<>();

        List<String> missingMonths =
                new ArrayList<>();

        int redisHit = 0;

        // ========================================================
        // 1. Redis first
        // ========================================================

        for (int i = 0; i < RECENT_MONTHS; i++) {

            String yearMonth =
                    latestMonth.minusMonths(i).toString();

            /*
             * 先讀取原始 JSON。
             *
             * null 才表示 Redis Key 不存在。
             */
            String json =
                    stockRevenueService.getMonthlyJson(
                            yearMonth
                    );

            if (json == null) {

                result.put(
                        yearMonth,
                        Collections.emptyList()
                );

                missingMonths.add(yearMonth);

                continue;
            }

            /*
             * 直接解析已取得的 JSON。
             *
             * 避免第二次 Redis GET。
             */
            List<StockRevenueResponse> redisList =
                    stockRevenueService.parseMonthlyJson(
                            yearMonth,
                            json
                    );

            result.put(
                    yearMonth,
                    redisService.toMonthlyRevenueVoList(
                            redisList
                    )
            );

            redisHit++;
        }

        // ========================================================
        // 2. All Redis Hit
        // ========================================================

        if (missingMonths.isEmpty()) {

            log.debug(
                    "[REVENUE] Recent revenue all Redis hit, months={}",
                    RECENT_MONTHS
            );

            return result;
        }

        // ========================================================
        // 3. PostgreSQL batch query
        // ========================================================

        Map<String, List<StockRevenueResponse>> dbData =
                stockRevenueService.findMonthlyRevenueFromDb(
                        missingMonths
                );

        // ========================================================
        // 4. Redis backfill
        // ========================================================

        int backfillCount = 0;

        for (String yearMonth : missingMonths) {

            List<StockRevenueResponse> dbList =
                    dbData.getOrDefault(
                            yearMonth,
                            Collections.emptyList()
                    );

            /*
             * DB 也沒有資料。
             *
             * 不回填空 Snapshot。
             */
            if (dbList.isEmpty()) {
                continue;
            }

            /*
             * SETNX:
             * Redis Key 不存在才寫入。
             */
            boolean written =
                    stockRevenueService.saveMonthlyListIfAbsent(
                            yearMonth,
                            dbList
                    );

            if (written) {
                backfillCount++;

                /*
                 * 這次回填的是 DB 資料，
                 * 因此直接用 DB 結果回傳。
                 */
                result.put(
                        yearMonth,
                        redisService.toMonthlyRevenueVoList(
                                dbList
                        )
                );

            } else {

                /*
                 * 可能有其他 Writer 已更新 Redis。
                 *
                 * 重新讀取，以 Redis 最新內容為準。
                 */
                String latestJson =
                        stockRevenueService.getMonthlyJson(
                                yearMonth
                        );

                if (latestJson != null) {

                    List<StockRevenueResponse> latestList =
                            stockRevenueService.parseMonthlyJson(
                                    yearMonth,
                                    latestJson
                            );

                    result.put(
                            yearMonth,
                            redisService.toMonthlyRevenueVoList(
                                    latestList
                            )
                    );

                } else {

                    /*
                     * Redis 仍然不存在時，
                     * 使用 DB 查詢結果回傳。
                     */
                    result.put(
                            yearMonth,
                            redisService.toMonthlyRevenueVoList(
                                    dbList
                            )
                    );
                }
            }
        }

        log.info(
                "[REVENUE] Recent revenue completed, months={}, redisHit={}, redisMiss={}, backfilled={}",
                RECENT_MONTHS,
                redisHit,
                missingMonths.size(),
                backfillCount
        );

        return result;
    }

    // ============================================================
    // NORMALIZE YEAR MONTH
    // ============================================================

    private static String normalizeYearMonth(
            String yearMonth) {

        String ym =
                yearMonth == null
                        ? ""
                        : yearMonth.trim();

        if (!ym.matches("^\\d{4}-\\d{2}$")) {

            throw new IllegalArgumentException(
                    "yearMonth must be yyyy-MM, but got: "
                            + yearMonth
            );
        }

        return YearMonth.parse(ym).toString();
    }
    
    /**
     * 從單月份全市場營收中，找出指定股票。
     */
    private MonthlyRevenueVo findStockRevenue(
            List<StockRevenueResponse> list,
            String stockId) {

        if (list == null || list.isEmpty()) {
            return null;
        }

        for (StockRevenueResponse dto : list) {

            if (dto == null || dto.getStockId() == null) {
                continue;
            }

            if (!stockId.equals(dto.getStockId().trim())) {
                continue;
            }

            MonthlyRevenueVo vo = new MonthlyRevenueVo();

            vo.setStockId(dto.getStockId());
            vo.setStockName(dto.getStockName());
            vo.setMarket(dto.getMarket());
            vo.setDate(dto.getDate());
            vo.setRevenue(dto.getRevenue());

            return vo;
        }

        return null;
    }
}
