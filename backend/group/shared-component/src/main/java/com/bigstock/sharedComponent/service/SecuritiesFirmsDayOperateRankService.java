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

    private static final Set<Integer> SUPPORTED_RANGE_DAYS = Set.of(1, 3, 5, 10, 20, 60, 120);

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public boolean isSupportedRangeDays(Integer rangeDays) {
        return rangeDays != null && SUPPORTED_RANGE_DAYS.contains(rangeDays);
    }

    public List<SecuritiesFirmsRankResult> calculateFixedRankBatch(
            List<String> stockCodes,
            Integer rangeDays,
            java.util.Date endDate
    ) {
        if (stockCodes == null || stockCodes.isEmpty()) {
            return Collections.emptyList();
        }

        if (!isSupportedRangeDays(rangeDays)) {
            throw new IllegalArgumentException("Unsupported rangeDays: " + rangeDays);
        }

        List<String> normalizedStockCodes = stockCodes.stream()
                .filter(code -> code != null && !code.isBlank())
                .map(String::trim)
                .distinct()
                .toList();

        if (normalizedStockCodes.isEmpty()) {
            return Collections.emptyList();
        }

        List<java.util.Date> tradingDates = findLatestTradingDates(endDate, rangeDays);
        DateRangeInfo dateRangeInfo = buildDateRangeInfo(tradingDates);

        if (tradingDates.isEmpty()) {
            return normalizedStockCodes.stream()
                    .map(stockCode -> buildNotReadyResult(stockCode, rangeDays, null, null, 0,
                            "No trading dates found"))
                    .toList();
        }

        List<RankRow> rows = queryRankRows(normalizedStockCodes, tradingDates);

        Map<String, List<RankRow>> rowsByStockCode = rows.stream()
                .collect(Collectors.groupingBy(RankRow::getStockCode, LinkedHashMap::new, Collectors.toList()));

        List<SecuritiesFirmsRankResult> results = new ArrayList<>(normalizedStockCodes.size());

        for (String stockCode : normalizedStockCodes) {
            List<RankRow> stockRows = rowsByStockCode.getOrDefault(stockCode, Collections.emptyList());

            SecuritiesFirmsRankResult result = new SecuritiesFirmsRankResult();
            result.setStatus("DONE");
            result.setStockCode(stockCode);
            result.setRangeType("FIXED");
            result.setRangeDays(rangeDays);
            result.setStartDate(dateRangeInfo.getStartDateText());
            result.setEndDate(dateRangeInfo.getEndDateText());
            result.setActualTradingDays(tradingDates.size());
            result.setSource("SCHEDULE");
            result.setBuyTop15(buildTop15(stockRows, true));
            result.setSellTop15(buildTop15(stockRows, false));

            results.add(result);
        }

        return results;
    }

    public SecuritiesFirmsRankResult calculateFixedRank(
            String stockCode,
            Integer rangeDays,
            java.util.Date endDate
    ) {
        List<SecuritiesFirmsRankResult> results = calculateFixedRankBatch(List.of(stockCode), rangeDays, endDate);

        if (results.isEmpty()) {
            SecuritiesFirmsRankResult result = new SecuritiesFirmsRankResult();
            result.setStatus("FAILED");
            result.setStockCode(stockCode);
            result.setRangeType("FIXED");
            result.setRangeDays(rangeDays);
            result.setSource("DB");
            result.setMessage("No result");
            return result;
        }

        SecuritiesFirmsRankResult result = results.get(0);
        result.setSource("DB");
        return result;
    }

    private List<java.util.Date> findLatestTradingDates(java.util.Date endDate, int limit) {
        String sql = """
                SELECT DISTINCT trading_date
                FROM %s
                WHERE trading_date <= :endDate
                ORDER BY trading_date DESC
                LIMIT :limit
                """.formatted(TABLE_NAME);

        Map<String, Object> params = Map.of(
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

        List<Date> sqlDates = tradingDates.stream()
                .map(d -> new Date(d.getTime()))
                .toList();

        Map<String, Object> params = new HashMap<>();
        params.put("stockCodes", stockCodes);
        params.put("tradingDates", sqlDates);

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
        return result;
    }

    private static class RankRow {
        private final String stockCode;
        private final String securitiesFirms;
        private final long buyAmount;
        private final long sellAmount;
        private final long netAmount;

        private RankRow(String stockCode, String securitiesFirms, long buyAmount, long sellAmount, long netAmount) {
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

    private static class DateRangeInfo {
        private final String startDateText;
        private final String endDateText;

        private DateRangeInfo(String startDateText, String endDateText) {
            this.startDateText = startDateText;
            this.endDateText = endDateText;
        }

        private String getStartDateText() {
            return startDateText;
        }

        private String getEndDateText() {
            return endDateText;
        }
    }
}
