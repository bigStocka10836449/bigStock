package com.bigstock.sharedComponent.service;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import com.bigstock.sharedComponent.dto.SecuritiesFirmsRankItem;
import com.bigstock.sharedComponent.dto.SecuritiesFirmsRankResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SecuritiesFirmsDayOperateRankService {

    private static final String TABLE_NAME = "bstock.securities_firms_day_operate";

    private static final Set<Integer> SUPPORTED_RANGE_DAYS =
            Set.of(1, 3, 5, 10, 20, 60, 120);

    private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT = ThreadLocal.withInitial(
            () -> new SimpleDateFormat("yyyy-MM-dd")
    );

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public boolean isSupportedRangeDays(Integer rangeDays) {
        return rangeDays != null && SUPPORTED_RANGE_DAYS.contains(rangeDays);
    }

    public SecuritiesFirmsRankResult calculateFixedRank(
            String stockCode,
            Integer rangeDays,
            java.util.Date endDate
    ) {
        SecuritiesFirmsRankResult result = new SecuritiesFirmsRankResult();
        result.setStatus("DONE");
        result.setStockCode(stockCode);
        result.setRangeType("FIXED");
        result.setRangeDays(rangeDays);
        result.setSource("DB");

        if (stockCode == null || stockCode.isBlank()) {
            result.setStatus("FAILED");
            result.setMessage("stockCode is blank");
            result.setBuyTop15(Collections.emptyList());
            result.setSellTop15(Collections.emptyList());
            return result;
        }

        if (!isSupportedRangeDays(rangeDays)) {
            result.setStatus("FAILED");
            result.setMessage("Unsupported rangeDays: " + rangeDays);
            result.setBuyTop15(Collections.emptyList());
            result.setSellTop15(Collections.emptyList());
            return result;
        }

        List<java.util.Date> tradingDates = findLatestTradingDates(endDate, rangeDays);
        result.setActualTradingDays(tradingDates.size());

        if (tradingDates.isEmpty()) {
            result.setStatus("NOT_READY");
            result.setMessage("No trading dates found");
            result.setBuyTop15(Collections.emptyList());
            result.setSellTop15(Collections.emptyList());
            return result;
        }

        List<java.util.Date> sortedAsc = new ArrayList<>(tradingDates);
        sortedAsc.sort(java.util.Date::compareTo);

        result.setStartDate(formatDate(sortedAsc.get(0)));
        result.setEndDate(formatDate(sortedAsc.get(sortedAsc.size() - 1)));

        result.setBuyTop15(queryTop15(stockCode.trim(), tradingDates, true));
        result.setSellTop15(queryTop15(stockCode.trim(), tradingDates, false));

        return result;
    }

    public List<SecuritiesFirmsRankResult> calculateFixedRankBatch(
            List<String> stockCodes,
            Integer rangeDays,
            java.util.Date endDate
    ) {
        List<java.util.Date> tradingDates = findLatestTradingDates(endDate, rangeDays);
        return calculateFixedRankBatchByTradingDates(stockCodes, rangeDays, tradingDates);
    }

    public List<SecuritiesFirmsRankResult> calculateFixedRankBatchByTradingDates(
            List<String> stockCodes,
            Integer rangeDays,
            List<java.util.Date> tradingDates
    ) {
        if (stockCodes == null || stockCodes.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> normalizedStockCodes = normalizeStockCodes(stockCodes);

        if (normalizedStockCodes.isEmpty()) {
            return Collections.emptyList();
        }

        if (!isSupportedRangeDays(rangeDays)) {
            return normalizedStockCodes.stream()
                    .map(stockCode -> buildFailedResult(
                            stockCode,
                            rangeDays,
                            "Unsupported rangeDays: " + rangeDays
                    ))
                    .toList();
        }

        if (tradingDates == null || tradingDates.isEmpty()) {
            return normalizedStockCodes.stream()
                    .map(stockCode -> buildNotReadyResult(
                            stockCode,
                            rangeDays,
                            null,
                            null,
                            0,
                            "No trading dates found"
                    ))
                    .toList();
        }

        List<java.util.Date> rangeTradingDates = limitTradingDates(tradingDates, rangeDays);
        DateRangeInfo dateRangeInfo = buildDateRangeInfo(rangeTradingDates);

        List<RankRow> rankRows = queryRankRows(normalizedStockCodes, rangeTradingDates);

        Map<String, List<RankRow>> rowsByStockCode = rankRows.stream()
                .collect(Collectors.groupingBy(
                        RankRow::getStockCode,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<SecuritiesFirmsRankResult> results = new ArrayList<>(normalizedStockCodes.size());

        for (String stockCode : normalizedStockCodes) {
            List<RankRow> stockRows = rowsByStockCode.getOrDefault(stockCode, Collections.emptyList());

            SecuritiesFirmsRankResult result = new SecuritiesFirmsRankResult();
            result.setStatus("DONE");
            result.setStockCode(stockCode);
            result.setRangeType("FIXED");
            result.setRangeDays(rangeDays);
            result.setStartDate(dateRangeInfo.getStartDate());
            result.setEndDate(dateRangeInfo.getEndDate());
            result.setActualTradingDays(rangeTradingDates.size());
            result.setSource("SCHEDULE");
            result.setBuyTop15(buildTop15(stockRows, true));
            result.setSellTop15(buildTop15(stockRows, false));

            results.add(result);
        }

        return results;
    }

    /**
     * Java range 計算版：
     *
     * 一批股票只查一次 SQL，DB 先依「每檔股票自己的最新可用交易日」往前取 120 個交易日，
     * 並彙總到「股票 + 日期 + 券商」。
     *
     * 1 / 3 / 5 / 10 / 20 / 60 / 120 天的累加與 Top15 排名交給 Java 計算。
     *
     * 重點：
     * 1. 不把原始價位明細整批拉回 Java。
     * 2. 不把 120 天直接壓成每個券商一筆，仍保留 trading_date。
     * 3. 每檔股票各自用自己的最近 120 個交易日，不依賴 2330 的交易日清單。
     * 4. SQL 端先剔除零股，stock_buy_amount / stock_sell_amount 未滿 1000 股不納入計算。
     */
    public List<SecuritiesFirmsRankResult> calculateFixedRankBatchAllRangesByLatestTradingDate(
            List<String> stockCodes,
            List<Integer> rangeDaysList,
            java.util.Date endDate
    ) {
        if (stockCodes == null || stockCodes.isEmpty()) {
            return Collections.emptyList();
        }

        if (rangeDaysList == null || rangeDaysList.isEmpty()) {
            return Collections.emptyList();
        }

        if (endDate == null) {
            return Collections.emptyList();
        }

        List<String> normalizedStockCodes = normalizeStockCodes(stockCodes);

        if (normalizedStockCodes.isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> normalizedRangeDaysList = rangeDaysList.stream()
                .filter(this::isSupportedRangeDays)
                .distinct()
                .sorted()
                .toList();

        if (normalizedRangeDaysList.isEmpty()) {
            return Collections.emptyList();
        }

        int maxRangeDays = normalizedRangeDaysList.stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(120);

        long queryStartMillis = System.currentTimeMillis();

        List<DailyAggRankRow> dailyAggRows = queryDailyAggRankRows(
                normalizedStockCodes,
                endDate,
                maxRangeDays
        );

        long queryCostMillis = System.currentTimeMillis() - queryStartMillis;
        long calculateStartMillis = System.currentTimeMillis();

        List<TradingDateRow> tradingDateRows = buildTradingDateRowsFromDailyAggRows(dailyAggRows);

        Map<String, List<TradingDateRow>> tradingDatesByStockCode = tradingDateRows.stream()
                .collect(Collectors.groupingBy(
                        TradingDateRow::getStockCode,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        Map<String, Map<Integer, DateRangeInfo>> dateRangeInfoByStockCodeAndRange = buildDateRangeInfoByStockCodeAndRange(
                normalizedStockCodes,
                normalizedRangeDaysList,
                tradingDatesByStockCode
        );

        Map<String, Map<Integer, Integer>> actualTradingDaysByStockCodeAndRange = buildActualTradingDaysByStockCodeAndRange(
                normalizedStockCodes,
                normalizedRangeDaysList,
                tradingDatesByStockCode
        );

        Map<String, Map<Integer, Map<String, FirmAmountAccumulator>>> accumulatorMap = new HashMap<>();

        for (String stockCode : normalizedStockCodes) {
            Map<Integer, Map<String, FirmAmountAccumulator>> rangeMap = new HashMap<>();

            for (Integer rangeDays : normalizedRangeDaysList) {
                rangeMap.put(rangeDays, new HashMap<>());
            }

            accumulatorMap.put(stockCode, rangeMap);
        }

        for (DailyAggRankRow row : dailyAggRows) {
            if (row == null || row.getStockCode() == null || row.getSecuritiesFirms() == null) {
                continue;
            }

            int tradingDayIndex = row.getTradingDayIndex();

            if (tradingDayIndex <= 0) {
                continue;
            }

            Map<Integer, Map<String, FirmAmountAccumulator>> rangeMap = accumulatorMap.get(row.getStockCode());

            if (rangeMap == null) {
                continue;
            }

            for (Integer rangeDays : normalizedRangeDaysList) {
                if (tradingDayIndex > rangeDays) {
                    continue;
                }

                Map<String, FirmAmountAccumulator> firmMap = rangeMap.get(rangeDays);

                if (firmMap == null) {
                    firmMap = new HashMap<>();
                    rangeMap.put(rangeDays, firmMap);
                }

                FirmAmountAccumulator accumulator = firmMap.computeIfAbsent(
                        row.getSecuritiesFirms(),
                        ignored -> new FirmAmountAccumulator()
                );

                accumulator.add(row.getBuyAmount(), row.getSellAmount());
            }
        }

        List<SecuritiesFirmsRankResult> results = new ArrayList<>(
                normalizedStockCodes.size() * normalizedRangeDaysList.size()
        );

        for (String stockCode : normalizedStockCodes) {
            Map<Integer, Map<String, FirmAmountAccumulator>> rangeMap = accumulatorMap.getOrDefault(
                    stockCode,
                    Collections.emptyMap()
            );

            Map<Integer, DateRangeInfo> dateRangeInfoByRange = dateRangeInfoByStockCodeAndRange.getOrDefault(
                    stockCode,
                    Collections.emptyMap()
            );

            Map<Integer, Integer> actualTradingDaysByRange = actualTradingDaysByStockCodeAndRange.getOrDefault(
                    stockCode,
                    Collections.emptyMap()
            );

            for (Integer rangeDays : normalizedRangeDaysList) {
                DateRangeInfo dateRangeInfo = dateRangeInfoByRange.get(rangeDays);
                int actualTradingDays = actualTradingDaysByRange.getOrDefault(rangeDays, 0);

                if (actualTradingDays <= 0) {
                    results.add(buildNotReadyResult(
                            stockCode,
                            rangeDays,
                            null,
                            null,
                            0,
                            "No trading dates found"
                    ));
                    continue;
                }

                Map<String, FirmAmountAccumulator> firmMap = rangeMap.getOrDefault(
                        rangeDays,
                        Collections.emptyMap()
                );

                List<RankRow> rangeRows = toRankRows(stockCode, firmMap);

                SecuritiesFirmsRankResult result = new SecuritiesFirmsRankResult();
                result.setStatus("DONE");
                result.setStockCode(stockCode);
                result.setRangeType("FIXED");
                result.setRangeDays(rangeDays);
                result.setStartDate(dateRangeInfo == null ? null : dateRangeInfo.getStartDate());
                result.setEndDate(dateRangeInfo == null ? null : dateRangeInfo.getEndDate());
                result.setActualTradingDays(actualTradingDays);
                result.setSource("SCHEDULE");
                result.setBuyTop15(buildTop15(rangeRows, true));
                result.setSellTop15(buildTop15(rangeRows, false));

                results.add(result);
            }
        }

        long calculateCostMillis = System.currentTimeMillis() - calculateStartMillis;
        long usedMemoryMb = getUsedMemoryMb();

        log.info("Securities firms rank java range calculation finished. stockCount={}, rangeDays={}, tradingDateRowCount={}, dailyAggRowCount={}, resultCount={}, dailyAggQueryCostMillis={}, calculateCostMillis={}, usedMemoryMb={}",
                normalizedStockCodes.size(),
                normalizedRangeDaysList,
                tradingDateRows.size(),
                dailyAggRows.size(),
                results.size(),
                queryCostMillis,
                calculateCostMillis,
                usedMemoryMb);

        return results;
    }

    /**
     * 保留舊方法名稱，避免其他呼叫點尚未調整時編譯失敗。
     * 新邏輯只需要 maxTradingDates 的第一天作為 endDate。
     */
    public List<SecuritiesFirmsRankResult> calculateFixedRankBatchAllRangesByTradingDates(
            List<String> stockCodes,
            List<Integer> rangeDaysList,
            List<java.util.Date> maxTradingDates
    ) {
        if (maxTradingDates == null || maxTradingDates.isEmpty()) {
            return Collections.emptyList();
        }

        return calculateFixedRankBatchAllRangesByLatestTradingDate(
                stockCodes,
                rangeDaysList,
                maxTradingDates.get(0)
        );
    }

    public List<java.util.Date> findLatestTradingDates(java.util.Date endDate, int limit) {
        String sql = """
                SELECT DISTINCT trading_date
                FROM %s
                WHERE stock_code = :referenceStockCode
                  AND trading_date <= :endDate
                ORDER BY trading_date DESC
                LIMIT :limit
                """.formatted(TABLE_NAME);

        Map<String, Object> params = Map.of(
                "referenceStockCode", "2330",
                "endDate", new Date(endDate.getTime()),
                "limit", limit
        );

        List<java.util.Date> dates = namedParameterJdbcTemplate.query(
                sql,
                params,
                (rs, rowNum) -> rs.getDate("trading_date")
        );

        return dates == null ? Collections.emptyList() : dates;
    }

    private List<SecuritiesFirmsRankItem> queryTop15(
            String stockCode,
            List<java.util.Date> tradingDates,
            boolean buyTop
    ) {
        if (tradingDates == null || tradingDates.isEmpty()) {
            return Collections.emptyList();
        }

        String order = buyTop ? "DESC" : "ASC";
        String havingOperator = buyTop ? ">" : "<";

        String sql = """
                SELECT
                    securities_firms,
                    SUM(CASE WHEN stock_buy_amount >= 1000 THEN stock_buy_amount ELSE 0 END) AS buy_amount,
                    SUM(CASE WHEN stock_sell_amount >= 1000 THEN stock_sell_amount ELSE 0 END) AS sell_amount,
                    (
                        SUM(CASE WHEN stock_buy_amount >= 1000 THEN stock_buy_amount ELSE 0 END)
                        -
                        SUM(CASE WHEN stock_sell_amount >= 1000 THEN stock_sell_amount ELSE 0 END)
                    ) AS net_amount
                FROM %s
                WHERE stock_code = :stockCode
                  AND trading_date BETWEEN :startDate AND :endDate
                GROUP BY securities_firms
                HAVING (
                    SUM(CASE WHEN stock_buy_amount >= 1000 THEN stock_buy_amount ELSE 0 END)
                    -
                    SUM(CASE WHEN stock_sell_amount >= 1000 THEN stock_sell_amount ELSE 0 END)
                ) %s 0
                ORDER BY net_amount %s
                LIMIT 15
                """.formatted(TABLE_NAME, havingOperator, order);

        DateRangeInfo dateRangeInfo = buildDateRangeInfo(tradingDates);

        Map<String, Object> params = Map.of(
                "stockCode", stockCode,
                "startDate", toSqlDate(dateRangeInfo.getStartDate()),
                "endDate", toSqlDate(dateRangeInfo.getEndDate())
        );

        List<SecuritiesFirmsRankItem> rows = namedParameterJdbcTemplate.query(
                sql,
                params,
                (rs, rowNum) -> {
                    long buyAmount = rs.getLong("buy_amount");
                    long sellAmount = rs.getLong("sell_amount");
                    long netAmount = rs.getLong("net_amount");

                    SecuritiesFirmsRankItem item = new SecuritiesFirmsRankItem();
                    item.setRank(rowNum + 1);
                    item.setSecuritiesFirms(rs.getString("securities_firms"));
                    item.setBuyAmount(buyAmount);
                    item.setSellAmount(sellAmount);
                    item.setNetAmount(netAmount);
                    item.setBuyLots(buyAmount / 1000);
                    item.setSellLots(sellAmount / 1000);
                    item.setNetLots(netAmount / 1000);
                    return item;
                }
        );

        return rows == null ? Collections.emptyList() : rows;
    }

    private List<RankRow> queryRankRows(List<String> stockCodes, List<java.util.Date> tradingDates) {
        String sql = """
                SELECT
                    stock_code,
                    securities_firms,
                    SUM(CASE WHEN stock_buy_amount >= 1000 THEN stock_buy_amount ELSE 0 END) AS buy_amount,
                    SUM(CASE WHEN stock_sell_amount >= 1000 THEN stock_sell_amount ELSE 0 END) AS sell_amount,
                    (
                        SUM(CASE WHEN stock_buy_amount >= 1000 THEN stock_buy_amount ELSE 0 END)
                        -
                        SUM(CASE WHEN stock_sell_amount >= 1000 THEN stock_sell_amount ELSE 0 END)
                    ) AS net_amount
                FROM %s
                WHERE stock_code IN (:stockCodes)
                  AND trading_date BETWEEN :startDate AND :endDate
                GROUP BY stock_code, securities_firms
                HAVING (
                    SUM(CASE WHEN stock_buy_amount >= 1000 THEN stock_buy_amount ELSE 0 END)
                    -
                    SUM(CASE WHEN stock_sell_amount >= 1000 THEN stock_sell_amount ELSE 0 END)
                ) <> 0
                ORDER BY stock_code ASC, net_amount DESC
                """.formatted(TABLE_NAME);

        DateRangeInfo dateRangeInfo = buildDateRangeInfo(tradingDates);

        Map<String, Object> params = new HashMap<>();
        params.put("stockCodes", stockCodes);
        params.put("startDate", toSqlDate(dateRangeInfo.getStartDate()));
        params.put("endDate", toSqlDate(dateRangeInfo.getEndDate()));

        List<RankRow> rows = namedParameterJdbcTemplate.query(
                sql,
                params,
                (rs, rowNum) -> new RankRow(
                        rs.getString("stock_code"),
                        rs.getString("securities_firms"),
                        rs.getLong("buy_amount"),
                        rs.getLong("sell_amount"),
                        rs.getLong("net_amount")
                )
        );

        return rows == null ? Collections.emptyList() : rows;
    }

    private List<TradingDateRow> queryLatestTradingDateRows(
            List<String> stockCodes,
            java.util.Date endDate,
            int maxRangeDays
    ) {
        if (stockCodes == null || stockCodes.isEmpty() || endDate == null || maxRangeDays <= 0) {
            return Collections.emptyList();
        }

        String sql = """
                WITH latest_dates AS (
                    SELECT
                        stock_code,
                        MAX(trading_date) AS end_date
                    FROM %s
                    WHERE stock_code IN (:stockCodes)
                      AND trading_date <= :endDate
                    GROUP BY stock_code
                ),
                distinct_trading_dates AS (
                    SELECT DISTINCT
                        sfdo.stock_code,
                        sfdo.trading_date
                    FROM %s sfdo
                    JOIN latest_dates ld
                      ON ld.stock_code = sfdo.stock_code
                     AND sfdo.trading_date <= ld.end_date
                ),
                ranked_trading_dates AS (
                    SELECT
                        stock_code,
                        trading_date,
                        ROW_NUMBER() OVER (
                            PARTITION BY stock_code
                            ORDER BY trading_date DESC
                        ) AS trading_day_index
                    FROM distinct_trading_dates
                )
                SELECT
                    stock_code,
                    trading_date,
                    trading_day_index
                FROM ranked_trading_dates
                WHERE trading_day_index <= :maxRangeDays
                ORDER BY stock_code ASC, trading_day_index ASC
                """.formatted(TABLE_NAME, TABLE_NAME);

        Map<String, Object> params = new HashMap<>();
        params.put("stockCodes", stockCodes);
        params.put("endDate", new Date(endDate.getTime()));
        params.put("maxRangeDays", maxRangeDays);

        List<TradingDateRow> rows = namedParameterJdbcTemplate.query(
                sql,
                params,
                (rs, rowNum) -> new TradingDateRow(
                        rs.getString("stock_code"),
                        rs.getDate("trading_date"),
                        rs.getInt("trading_day_index")
                )
        );

        return rows == null ? Collections.emptyList() : rows;
    }

    private List<DailyAggRankRow> queryDailyAggRankRows(
            List<String> stockCodes,
            java.util.Date endDate,
            int maxRangeDays
    ) {
        if (stockCodes == null || stockCodes.isEmpty() || endDate == null || maxRangeDays <= 0) {
            return Collections.emptyList();
        }

        Map<String, Object> params = new HashMap<>();
        String targetStockValuesSql = buildTargetStockValuesSql(stockCodes, params);

        String sql = """
                WITH target_stocks(stock_code) AS (
                    VALUES
                    %s
                ),
                last_trading_dates AS (
                    SELECT
                        s.stock_code,
                        d.trading_date,
                        d.trading_day_index
                    FROM target_stocks s
                    CROSS JOIN LATERAL (
                        SELECT
                            trading_date,
                            ROW_NUMBER() OVER (ORDER BY trading_date DESC) AS trading_day_index
                        FROM (
                            SELECT DISTINCT
                                sfdo.trading_date
                            FROM %s sfdo
                            WHERE sfdo.stock_code = s.stock_code
                              AND sfdo.trading_date <= :endDate
                            ORDER BY sfdo.trading_date DESC
                            LIMIT :maxRangeDays
                        ) limited_dates
                    ) d
                ),
                daily_agg AS (
                    SELECT
                        d.stock_code,
                        d.trading_date,
                        d.trading_day_index,
                        sfdo.securities_firms,
                        SUM(CASE WHEN sfdo.stock_buy_amount >= 1000 THEN sfdo.stock_buy_amount ELSE 0 END) AS buy_amount,
                        SUM(CASE WHEN sfdo.stock_sell_amount >= 1000 THEN sfdo.stock_sell_amount ELSE 0 END) AS sell_amount
                    FROM last_trading_dates d
                    JOIN %s sfdo
                      ON sfdo.stock_code = d.stock_code
                     AND sfdo.trading_date = d.trading_date
                    WHERE sfdo.trading_date <= :endDate
                      AND (sfdo.stock_buy_amount >= 1000 OR sfdo.stock_sell_amount >= 1000)
                    GROUP BY
                        d.stock_code,
                        d.trading_date,
                        d.trading_day_index,
                        sfdo.securities_firms
                    HAVING
                        SUM(CASE WHEN sfdo.stock_buy_amount >= 1000 THEN sfdo.stock_buy_amount ELSE 0 END) > 0
                        OR
                        SUM(CASE WHEN sfdo.stock_sell_amount >= 1000 THEN sfdo.stock_sell_amount ELSE 0 END) > 0
                )
                SELECT
                    d.stock_code,
                    d.trading_date,
                    d.trading_day_index,
                    daily_agg.securities_firms,
                    COALESCE(daily_agg.buy_amount, 0) AS buy_amount,
                    COALESCE(daily_agg.sell_amount, 0) AS sell_amount
                FROM last_trading_dates d
                LEFT JOIN daily_agg
                  ON daily_agg.stock_code = d.stock_code
                 AND daily_agg.trading_date = d.trading_date
                 AND daily_agg.trading_day_index = d.trading_day_index
                ORDER BY
                    d.stock_code ASC,
                    d.trading_day_index ASC,
                    daily_agg.securities_firms ASC
                """.formatted(targetStockValuesSql, TABLE_NAME, TABLE_NAME);

        params.put("endDate", new Date(endDate.getTime()));
        params.put("maxRangeDays", maxRangeDays);

        List<DailyAggRankRow> rows = namedParameterJdbcTemplate.query(
                sql,
                params,
                (rs, rowNum) -> {
                    java.util.Date tradingDate = rs.getDate("trading_date");

                    return new DailyAggRankRow(
                            rs.getString("stock_code"),
                            tradingDate,
                            tradingDate == null ? null : formatDate(tradingDate),
                            rs.getInt("trading_day_index"),
                            rs.getString("securities_firms"),
                            rs.getLong("buy_amount"),
                            rs.getLong("sell_amount")
                    );
                }
        );

        return rows == null ? Collections.emptyList() : rows;
    }

    private String buildTargetStockValuesSql(List<String> stockCodes, Map<String, Object> params) {
        List<String> values = new ArrayList<>(stockCodes.size());

        for (int i = 0; i < stockCodes.size(); i++) {
            String paramName = "targetStockCode" + i;
            params.put(paramName, stockCodes.get(i));
            values.add("(CAST(:" + paramName + " AS text))");
        }

        return String.join(",\n", values);
    }

    private List<TradingDateRow> buildTradingDateRowsFromDailyAggRows(List<DailyAggRankRow> dailyAggRows) {
        if (dailyAggRows == null || dailyAggRows.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, TradingDateRow> tradingDateRowMap = new LinkedHashMap<>();

        for (DailyAggRankRow row : dailyAggRows) {
            if (row == null || row.getStockCode() == null || row.getTradingDate() == null || row.getTradingDayIndex() <= 0) {
                continue;
            }

            String key = row.getStockCode() + "|" + formatDate(row.getTradingDate()) + "|" + row.getTradingDayIndex();
            tradingDateRowMap.putIfAbsent(
                    key,
                    new TradingDateRow(
                            row.getStockCode(),
                            row.getTradingDate(),
                            row.getTradingDayIndex()
                    )
            );
        }

        return new ArrayList<>(tradingDateRowMap.values());
    }

    private Map<String, Map<Integer, DateRangeInfo>> buildDateRangeInfoByStockCodeAndRange(
            List<String> stockCodes,
            List<Integer> rangeDaysList,
            Map<String, List<TradingDateRow>> tradingDatesByStockCode
    ) {
        if (stockCodes == null || stockCodes.isEmpty() || rangeDaysList == null || rangeDaysList.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Map<Integer, DateRangeInfo>> result = new HashMap<>();

        for (String stockCode : stockCodes) {
            List<TradingDateRow> tradingDateRows = tradingDatesByStockCode.getOrDefault(
                    stockCode,
                    Collections.emptyList()
            );

            Map<Integer, DateRangeInfo> dateRangeInfoByRange = new HashMap<>();

            for (Integer rangeDays : rangeDaysList) {
                List<java.util.Date> dates = tradingDateRows.stream()
                        .filter(row -> row.getTradingDayIndex() <= rangeDays)
                        .map(TradingDateRow::getTradingDate)
                        .filter(date -> date != null)
                        .toList();

                dateRangeInfoByRange.put(rangeDays, buildDateRangeInfo(dates));
            }

            result.put(stockCode, dateRangeInfoByRange);
        }

        return result;
    }

    private Map<String, Map<Integer, Integer>> buildActualTradingDaysByStockCodeAndRange(
            List<String> stockCodes,
            List<Integer> rangeDaysList,
            Map<String, List<TradingDateRow>> tradingDatesByStockCode
    ) {
        if (stockCodes == null || stockCodes.isEmpty() || rangeDaysList == null || rangeDaysList.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Map<Integer, Integer>> result = new HashMap<>();

        for (String stockCode : stockCodes) {
            List<TradingDateRow> tradingDateRows = tradingDatesByStockCode.getOrDefault(
                    stockCode,
                    Collections.emptyList()
            );

            Map<Integer, Integer> actualTradingDaysByRange = new HashMap<>();

            for (Integer rangeDays : rangeDaysList) {
                long count = tradingDateRows.stream()
                        .filter(row -> row.getTradingDayIndex() <= rangeDays)
                        .count();

                actualTradingDaysByRange.put(rangeDays, Math.toIntExact(count));
            }

            result.put(stockCode, actualTradingDaysByRange);
        }

        return result;
    }

    private Map<String, Integer> buildTradingDayIndexByDateText(List<java.util.Date> maxTradingDates) {
        if (maxTradingDates == null || maxTradingDates.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Integer> result = new HashMap<>();

        for (int i = 0; i < maxTradingDates.size(); i++) {
            java.util.Date tradingDate = maxTradingDates.get(i);

            if (tradingDate == null) {
                continue;
            }

            result.put(formatDate(tradingDate), i + 1);
        }

        return result;
    }

    private List<RankRow> toRankRows(
            String stockCode,
            Map<String, FirmAmountAccumulator> firmMap
    ) {
        if (firmMap == null || firmMap.isEmpty()) {
            return Collections.emptyList();
        }

        List<RankRow> rows = new ArrayList<>(firmMap.size());

        for (Map.Entry<String, FirmAmountAccumulator> entry : firmMap.entrySet()) {
            FirmAmountAccumulator accumulator = entry.getValue();

            if (accumulator == null) {
                continue;
            }

            long buyAmount = accumulator.getBuyAmount();
            long sellAmount = accumulator.getSellAmount();
            long netAmount = buyAmount - sellAmount;

            if (netAmount == 0) {
                continue;
            }

            rows.add(new RankRow(
                    stockCode,
                    entry.getKey(),
                    buyAmount,
                    sellAmount,
                    netAmount
            ));
        }

        return rows;
    }

    private String formatDate(java.util.Date date) {
        if (date == null) {
            return null;
        }

        return DATE_FORMAT.get().format(date);
    }

    private long getUsedMemoryMb() {
        Runtime runtime = Runtime.getRuntime();
        return (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
    }

    private List<MultiRangeRankRow> queryMultiRangeRankRows(
            List<String> stockCodes,
            List<Integer> rangeDaysList,
            Map<Integer, List<java.util.Date>> tradingDatesByRangeDays
    ) {
        List<java.util.Date> maxTradingDates = tradingDatesByRangeDays.getOrDefault(
                120,
                Collections.emptyList()
        );

        if (maxTradingDates.isEmpty()) {
            int maxRangeDays = rangeDaysList.stream()
                    .mapToInt(Integer::intValue)
                    .max()
                    .orElse(120);

            maxTradingDates = tradingDatesByRangeDays.getOrDefault(
                    maxRangeDays,
                    Collections.emptyList()
            );
        }

        if (maxTradingDates.isEmpty()) {
            return Collections.emptyList();
        }

        DateRangeInfo maxDateRangeInfo = buildDateRangeInfo(maxTradingDates);

        if (maxDateRangeInfo.getStartDate() == null || maxDateRangeInfo.getEndDate() == null) {
            return Collections.emptyList();
        }

        StringBuilder selectBuilder = new StringBuilder();
        Map<String, Object> params = new HashMap<>();

        params.put("stockCodes", stockCodes);
        params.put("maxStartDate", toSqlDate(maxDateRangeInfo.getStartDate()));
        params.put("maxEndDate", toSqlDate(maxDateRangeInfo.getEndDate()));

        for (Integer rangeDays : rangeDaysList) {
            List<java.util.Date> dates = tradingDatesByRangeDays.getOrDefault(
                    rangeDays,
                    Collections.emptyList()
            );
            DateRangeInfo dateRangeInfo = buildDateRangeInfo(dates);

            if (dateRangeInfo.getStartDate() == null || dateRangeInfo.getEndDate() == null) {
                continue;
            }

            String startDateParam = "startDate" + rangeDays;
            String endDateParam = "endDate" + rangeDays;

            params.put(startDateParam, toSqlDate(dateRangeInfo.getStartDate()));
            params.put(endDateParam, toSqlDate(dateRangeInfo.getEndDate()));

            selectBuilder.append("""
                    ,
                    SUM(CASE WHEN trading_date BETWEEN :%s AND :%s AND stock_buy_amount >= 1000 THEN stock_buy_amount ELSE 0 END) AS buy_amount_%s,
                    SUM(CASE WHEN trading_date BETWEEN :%s AND :%s AND stock_sell_amount >= 1000 THEN stock_sell_amount ELSE 0 END) AS sell_amount_%s
                    """.formatted(
                    startDateParam,
                    endDateParam,
                    rangeDays,
                    startDateParam,
                    endDateParam,
                    rangeDays
            ));
        }

        String sql = """
                SELECT
                    stock_code,
                    securities_firms
                    %s
                FROM %s
                WHERE stock_code IN (:stockCodes)
                  AND trading_date BETWEEN :maxStartDate AND :maxEndDate
                  AND (stock_buy_amount >= 1000 OR stock_sell_amount >= 1000)
                GROUP BY stock_code, securities_firms
                HAVING
                    SUM(CASE WHEN stock_buy_amount >= 1000 THEN stock_buy_amount ELSE 0 END) <> 0
                    OR
                    SUM(CASE WHEN stock_sell_amount >= 1000 THEN stock_sell_amount ELSE 0 END) <> 0
                """.formatted(selectBuilder, TABLE_NAME);

        List<MultiRangeRankRow> rows = namedParameterJdbcTemplate.query(
                sql,
                params,
                (rs, rowNum) -> {
                    Map<Integer, RangeAmount> amountByRangeDays = new HashMap<>();

                    for (Integer rangeDays : rangeDaysList) {
                        long buyAmount = rs.getLong("buy_amount_" + rangeDays);
                        long sellAmount = rs.getLong("sell_amount_" + rangeDays);

                        amountByRangeDays.put(
                                rangeDays,
                                new RangeAmount(buyAmount, sellAmount)
                        );
                    }

                    return new MultiRangeRankRow(
                            rs.getString("stock_code"),
                            rs.getString("securities_firms"),
                            amountByRangeDays
                    );
                }
        );

        return rows == null ? Collections.emptyList() : rows;
    }

    private Map<Integer, List<java.util.Date>> buildTradingDatesByRangeDays(
            List<Integer> rangeDaysList,
            List<java.util.Date> maxTradingDates
    ) {
        Map<Integer, List<java.util.Date>> result = new LinkedHashMap<>();

        for (Integer rangeDays : rangeDaysList) {
            int toIndex = Math.min(rangeDays, maxTradingDates.size());
            result.put(rangeDays, new ArrayList<>(maxTradingDates.subList(0, toIndex)));
        }

        return result;
    }

    private List<RankRow> toRangeRows(List<MultiRangeRankRow> stockRows, Integer rangeDays) {
        if (stockRows == null || stockRows.isEmpty()) {
            return Collections.emptyList();
        }

        List<RankRow> rows = new ArrayList<>();

        for (MultiRangeRankRow row : stockRows) {
            RangeAmount amount = row.getAmountByRangeDays().get(rangeDays);

            if (amount == null) {
                continue;
            }

            long buyAmount = amount.getBuyAmount();
            long sellAmount = amount.getSellAmount();
            long netAmount = buyAmount - sellAmount;

            if (netAmount == 0) {
                continue;
            }

            rows.add(new RankRow(
                    row.getStockCode(),
                    row.getSecuritiesFirms(),
                    buyAmount,
                    sellAmount,
                    netAmount
            ));
        }

        return rows;
    }

    private List<SecuritiesFirmsRankItem> buildTop15(List<RankRow> rows, boolean buyTop) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }

        Comparator<RankRow> comparator = buyTop
                ? Comparator.comparingLong(RankRow::getNetAmount).reversed()
                : Comparator.comparingLong(RankRow::getNetAmount);

        List<RankRow> filteredRows = rows.stream()
                .filter(row -> buyTop ? row.getNetAmount() > 0 : row.getNetAmount() < 0)
                .sorted(comparator)
                .limit(15)
                .toList();

        List<SecuritiesFirmsRankItem> items = new ArrayList<>(filteredRows.size());

        for (int i = 0; i < filteredRows.size(); i++) {
            RankRow row = filteredRows.get(i);

            SecuritiesFirmsRankItem item = new SecuritiesFirmsRankItem();
            item.setRank(i + 1);
            item.setSecuritiesFirms(row.getSecuritiesFirms());
            item.setBuyAmount(row.getBuyAmount());
            item.setSellAmount(row.getSellAmount());
            item.setNetAmount(row.getNetAmount());
            item.setBuyLots(row.getBuyAmount() / 1000);
            item.setSellLots(row.getSellAmount() / 1000);
            item.setNetLots(row.getNetAmount() / 1000);

            items.add(item);
        }

        return items;
    }

    private List<String> normalizeStockCodes(List<String> stockCodes) {
        if (stockCodes == null || stockCodes.isEmpty()) {
            return Collections.emptyList();
        }

        return stockCodes.stream()
                .filter(stockCode -> stockCode != null && !stockCode.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }

    private List<java.util.Date> limitTradingDates(List<java.util.Date> tradingDates, int limit) {
        if (tradingDates == null || tradingDates.isEmpty()) {
            return Collections.emptyList();
        }

        int toIndex = Math.min(limit, tradingDates.size());
        return new ArrayList<>(tradingDates.subList(0, toIndex));
    }

    private DateRangeInfo buildDateRangeInfo(List<java.util.Date> tradingDates) {
        if (tradingDates == null || tradingDates.isEmpty()) {
            return new DateRangeInfo(null, null);
        }

        List<java.util.Date> sortedAsc = new ArrayList<>(tradingDates);
        sortedAsc.sort(java.util.Date::compareTo);

        return new DateRangeInfo(
                formatDate(sortedAsc.get(0)),
                formatDate(sortedAsc.get(sortedAsc.size() - 1))
        );
    }

    private List<Date> toSqlDates(List<java.util.Date> tradingDates) {
        if (tradingDates == null || tradingDates.isEmpty()) {
            return Collections.emptyList();
        }

        return tradingDates.stream()
                .map(d -> new Date(d.getTime()))
                .toList();
    }

    private Date toSqlDate(String dateText) {
        if (dateText == null || dateText.trim().isEmpty()) {
            return null;
        }

        return Date.valueOf(dateText.trim());
    }

    private SecuritiesFirmsRankResult buildFailedResult(
            String stockCode,
            Integer rangeDays,
            String message
    ) {
        SecuritiesFirmsRankResult result = new SecuritiesFirmsRankResult();
        result.setStatus("FAILED");
        result.setStockCode(stockCode);
        result.setRangeType("FIXED");
        result.setRangeDays(rangeDays);
        result.setActualTradingDays(0);
        result.setSource("SCHEDULE");
        result.setMessage(message);
        result.setBuyTop15(Collections.emptyList());
        result.setSellTop15(Collections.emptyList());
        return result;
    }

    private SecuritiesFirmsRankResult buildNotReadyResult(
            String stockCode,
            Integer rangeDays,
            String startDate,
            String endDate,
            Integer actualTradingDays,
            String message
    ) {
        SecuritiesFirmsRankResult result = new SecuritiesFirmsRankResult();
        result.setStatus("NOT_READY");
        result.setStockCode(stockCode);
        result.setRangeType("FIXED");
        result.setRangeDays(rangeDays);
        result.setStartDate(startDate);
        result.setEndDate(endDate);
        result.setActualTradingDays(actualTradingDays);
        result.setSource("SCHEDULE");
        result.setMessage(message);
        result.setBuyTop15(Collections.emptyList());
        result.setSellTop15(Collections.emptyList());
        return result;
    }

    private static class RankRow {
        private final String stockCode;
        private final String securitiesFirms;
        private final long buyAmount;
        private final long sellAmount;
        private final long netAmount;

        private RankRow(
                String stockCode,
                String securitiesFirms,
                long buyAmount,
                long sellAmount,
                long netAmount
        ) {
            this.stockCode = stockCode;
            this.securitiesFirms = securitiesFirms;
            this.buyAmount = buyAmount;
            this.sellAmount = sellAmount;
            this.netAmount = netAmount;
        }

        private String getStockCode() {
            return stockCode;
        }

        private String getSecuritiesFirms() {
            return securitiesFirms;
        }

        private long getBuyAmount() {
            return buyAmount;
        }

        private long getSellAmount() {
            return sellAmount;
        }

        private long getNetAmount() {
            return netAmount;
        }
    }

    private static class DailyAggRankRow {
        private final String stockCode;
        private final java.util.Date tradingDate;
        private final String tradingDateText;
        private final int tradingDayIndex;
        private final String securitiesFirms;
        private final long buyAmount;
        private final long sellAmount;

        private DailyAggRankRow(
                String stockCode,
                java.util.Date tradingDate,
                String tradingDateText,
                int tradingDayIndex,
                String securitiesFirms,
                long buyAmount,
                long sellAmount
        ) {
            this.stockCode = stockCode;
            this.tradingDate = tradingDate;
            this.tradingDateText = tradingDateText;
            this.tradingDayIndex = tradingDayIndex;
            this.securitiesFirms = securitiesFirms;
            this.buyAmount = buyAmount;
            this.sellAmount = sellAmount;
        }

        private String getStockCode() {
            return stockCode;
        }

        @SuppressWarnings("unused")
        private java.util.Date getTradingDate() {
            return tradingDate;
        }

        @SuppressWarnings("unused")
        private String getTradingDateText() {
            return tradingDateText;
        }

        private int getTradingDayIndex() {
            return tradingDayIndex;
        }

        private String getSecuritiesFirms() {
            return securitiesFirms;
        }

        private long getBuyAmount() {
            return buyAmount;
        }

        private long getSellAmount() {
            return sellAmount;
        }
    }

    private static class TradingDateRow {
        private final String stockCode;
        private final java.util.Date tradingDate;
        private final int tradingDayIndex;

        private TradingDateRow(
                String stockCode,
                java.util.Date tradingDate,
                int tradingDayIndex
        ) {
            this.stockCode = stockCode;
            this.tradingDate = tradingDate;
            this.tradingDayIndex = tradingDayIndex;
        }

        private String getStockCode() {
            return stockCode;
        }

        private java.util.Date getTradingDate() {
            return tradingDate;
        }

        private int getTradingDayIndex() {
            return tradingDayIndex;
        }
    }

    private static class FirmAmountAccumulator {
        private long buyAmount;
        private long sellAmount;

        private void add(long buyAmount, long sellAmount) {
            this.buyAmount += buyAmount;
            this.sellAmount += sellAmount;
        }

        private long getBuyAmount() {
            return buyAmount;
        }

        private long getSellAmount() {
            return sellAmount;
        }
    }

    private static class MultiRangeRankRow {
        private final String stockCode;
        private final String securitiesFirms;
        private final Map<Integer, RangeAmount> amountByRangeDays;

        private MultiRangeRankRow(
                String stockCode,
                String securitiesFirms,
                Map<Integer, RangeAmount> amountByRangeDays
        ) {
            this.stockCode = stockCode;
            this.securitiesFirms = securitiesFirms;
            this.amountByRangeDays = amountByRangeDays;
        }

        private String getStockCode() {
            return stockCode;
        }

        private String getSecuritiesFirms() {
            return securitiesFirms;
        }

        private Map<Integer, RangeAmount> getAmountByRangeDays() {
            return amountByRangeDays;
        }
    }

    private static class RangeAmount {
        private final long buyAmount;
        private final long sellAmount;

        private RangeAmount(long buyAmount, long sellAmount) {
            this.buyAmount = buyAmount;
            this.sellAmount = sellAmount;
        }

        private long getBuyAmount() {
            return buyAmount;
        }

        private long getSellAmount() {
            return sellAmount;
        }
    }

    private static class DateRangeInfo {
        private final String startDate;
        private final String endDate;

        private DateRangeInfo(String startDate, String endDate) {
            this.startDate = startDate;
            this.endDate = endDate;
        }

        private String getStartDate() {
            return startDate;
        }

        private String getEndDate() {
            return endDate;
        }
    }
}
