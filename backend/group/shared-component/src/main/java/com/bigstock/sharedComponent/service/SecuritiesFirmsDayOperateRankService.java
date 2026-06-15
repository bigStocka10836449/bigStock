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

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

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

        result.setStartDate(DATE_FORMAT.format(sortedAsc.get(0)));
        result.setEndDate(DATE_FORMAT.format(sortedAsc.get(sortedAsc.size() - 1)));

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
     * 效能折衷版：
     *
     * 一批股票只查一次 SQL。
     * SQL 一次算出 1 / 3 / 5 / 10 / 20 / 60 / 120 七組買賣金額。
     * Java 端只負責依 rangeDays 拆 Top15。
     *
     * Redis 不會存 120 天明細，只存最後 Top15 結果。
     */
    public List<SecuritiesFirmsRankResult> calculateFixedRankBatchAllRangesByTradingDates(
            List<String> stockCodes,
            List<Integer> rangeDaysList,
            List<java.util.Date> maxTradingDates
    ) {
        if (stockCodes == null || stockCodes.isEmpty()) {
            return Collections.emptyList();
        }

        if (rangeDaysList == null || rangeDaysList.isEmpty()) {
            return Collections.emptyList();
        }

        if (maxTradingDates == null || maxTradingDates.isEmpty()) {
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

        Map<Integer, List<java.util.Date>> tradingDatesByRangeDays = buildTradingDatesByRangeDays(
                normalizedRangeDaysList,
                maxTradingDates
        );

        List<MultiRangeRankRow> multiRangeRows = queryMultiRangeRankRows(
                normalizedStockCodes,
                normalizedRangeDaysList,
                tradingDatesByRangeDays
        );

        Map<String, List<MultiRangeRankRow>> rowsByStockCode = multiRangeRows.stream()
                .collect(Collectors.groupingBy(
                        MultiRangeRankRow::getStockCode,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        Map<Integer, DateRangeInfo> dateRangeInfoByRangeDays = new HashMap<>();

        for (Integer rangeDays : normalizedRangeDaysList) {
            List<java.util.Date> rangeTradingDates = tradingDatesByRangeDays.getOrDefault(
                    rangeDays,
                    Collections.emptyList()
            );
            dateRangeInfoByRangeDays.put(rangeDays, buildDateRangeInfo(rangeTradingDates));
        }

        List<SecuritiesFirmsRankResult> results = new ArrayList<>(
                normalizedStockCodes.size() * normalizedRangeDaysList.size()
        );

        for (String stockCode : normalizedStockCodes) {
            List<MultiRangeRankRow> stockRows = rowsByStockCode.getOrDefault(stockCode, Collections.emptyList());

            for (Integer rangeDays : normalizedRangeDaysList) {
                List<java.util.Date> rangeTradingDates = tradingDatesByRangeDays.getOrDefault(
                        rangeDays,
                        Collections.emptyList()
                );

                DateRangeInfo dateRangeInfo = dateRangeInfoByRangeDays.get(rangeDays);
                List<RankRow> rangeRows = toRangeRows(stockRows, rangeDays);

                SecuritiesFirmsRankResult result = new SecuritiesFirmsRankResult();
                result.setStatus("DONE");
                result.setStockCode(stockCode);
                result.setRangeType("FIXED");
                result.setRangeDays(rangeDays);
                result.setStartDate(dateRangeInfo == null ? null : dateRangeInfo.getStartDate());
                result.setEndDate(dateRangeInfo == null ? null : dateRangeInfo.getEndDate());
                result.setActualTradingDays(rangeTradingDates.size());
                result.setSource("SCHEDULE");
                result.setBuyTop15(buildTop15(rangeRows, true));
                result.setSellTop15(buildTop15(rangeRows, false));

                results.add(result);
            }
        }

        return results;
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
                  AND trading_date IN (:tradingDates)
                GROUP BY securities_firms
                HAVING (
                    SUM(CASE WHEN stock_buy_amount >= 1000 THEN stock_buy_amount ELSE 0 END)
                    -
                    SUM(CASE WHEN stock_sell_amount >= 1000 THEN stock_sell_amount ELSE 0 END)
                ) %s 0
                ORDER BY net_amount %s
                LIMIT 15
                """.formatted(TABLE_NAME, havingOperator, order);

        List<Date> sqlDates = toSqlDates(tradingDates);

        Map<String, Object> params = Map.of(
                "stockCode", stockCode,
                "tradingDates", sqlDates
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
                  AND trading_date IN (:tradingDates)
                GROUP BY stock_code, securities_firms
                HAVING (
                    SUM(CASE WHEN stock_buy_amount >= 1000 THEN stock_buy_amount ELSE 0 END)
                    -
                    SUM(CASE WHEN stock_sell_amount >= 1000 THEN stock_sell_amount ELSE 0 END)
                ) <> 0
                ORDER BY stock_code ASC, net_amount DESC
                """.formatted(TABLE_NAME);

        Map<String, Object> params = new HashMap<>();
        params.put("stockCodes", stockCodes);
        params.put("tradingDates", toSqlDates(tradingDates));

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

        StringBuilder selectBuilder = new StringBuilder();

        for (Integer rangeDays : rangeDaysList) {
            String dateParam = "tradingDates" + rangeDays;

            selectBuilder.append("""
                    ,
                    SUM(CASE WHEN trading_date IN (:%s) AND stock_buy_amount >= 1000 THEN stock_buy_amount ELSE 0 END) AS buy_amount_%s,
                    SUM(CASE WHEN trading_date IN (:%s) AND stock_sell_amount >= 1000 THEN stock_sell_amount ELSE 0 END) AS sell_amount_%s
                    """.formatted(dateParam, rangeDays, dateParam, rangeDays));
        }

        String sql = """
                SELECT
                    stock_code,
                    securities_firms
                    %s
                FROM %s
                WHERE stock_code IN (:stockCodes)
                  AND trading_date IN (:maxTradingDates)
                GROUP BY stock_code, securities_firms
                HAVING
                    SUM(CASE WHEN stock_buy_amount >= 1000 THEN stock_buy_amount ELSE 0 END) <> 0
                    OR
                    SUM(CASE WHEN stock_sell_amount >= 1000 THEN stock_sell_amount ELSE 0 END) <> 0
                ORDER BY stock_code ASC
                """.formatted(selectBuilder, TABLE_NAME);

        Map<String, Object> params = new HashMap<>();
        params.put("stockCodes", stockCodes);
        params.put("maxTradingDates", toSqlDates(maxTradingDates));

        for (Integer rangeDays : rangeDaysList) {
            List<java.util.Date> dates = tradingDatesByRangeDays.getOrDefault(
                    rangeDays,
                    Collections.emptyList()
            );
            params.put("tradingDates" + rangeDays, toSqlDates(dates));
        }

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
                DATE_FORMAT.format(sortedAsc.get(0)),
                DATE_FORMAT.format(sortedAsc.get(sortedAsc.size() - 1))
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