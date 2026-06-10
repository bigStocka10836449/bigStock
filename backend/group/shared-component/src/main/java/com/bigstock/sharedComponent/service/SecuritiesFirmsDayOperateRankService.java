package com.bigstock.sharedComponent.service;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    /**
     * 你截圖看起來是 public schema。
     * 如果正式環境目前是 bstock schema，請改成：
     * private static final String TABLE_NAME = "bstock.securities_firms_day_operate";
     */
    private static final String TABLE_NAME = "public.securities_firms_day_operate";

    private static final Set<Integer> SUPPORTED_RANGE_DAYS =
            Set.of(1, 3, 5, 10, 20, 60, 120, 240);

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
            return result;
        }

        if (!isSupportedRangeDays(rangeDays)) {
            result.setStatus("FAILED");
            result.setMessage("Unsupported rangeDays: " + rangeDays);
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

        result.setBuyTop15(queryTop15(stockCode, tradingDates, true));
        result.setSellTop15(queryTop15(stockCode, tradingDates, false));

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

        List<Date> sqlDates = tradingDates.stream()
                .map(d -> new Date(d.getTime()))
                .toList();

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
}