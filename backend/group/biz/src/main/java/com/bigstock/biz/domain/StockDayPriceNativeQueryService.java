package com.bigstock.biz.domain;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.bigstock.sharedComponent.dto.DynamicFilterStockPriceCondition;
import com.google.common.collect.Maps;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StockDayPriceNativeQueryService {

    private final EntityManager em;

    private static final String SAFE_NUMERIC =
            "CASE WHEN btrim(%s) ~ '^-?\\d+(\\.\\d+)?$' THEN CAST(btrim(%s) AS NUMERIC) ELSE NULL END";

    private static final String FIND_K_VALUE_CONTINUE_UNDER_TEWNTY_BY_RANGE = "SELECT  "
            + "    ranked_data.stock_code, " + "    MAX(ranked_data.trading_day) AS start_day, "
            + "    MIN(ranked_data.trading_day) AS end_day, " + "    COUNT(*) AS total_days, "
            + "    SUM(CASE WHEN ranked_data.line_k_value <= 20 THEN 1 ELSE 0 END) AS required_count "
            + "FROM ( " + "    SELECT  " + "        sdp.stock_code, " + "        sdp.trading_day, "
            + "        " + String.format(SAFE_NUMERIC, "sdp.line_k_value", "sdp.line_k_value") + " AS line_k_value, "
            + "        ROW_NUMBER() OVER (PARTITION BY sdp.stock_code ORDER BY sdp.trading_day DESC) AS rn "
            + "    FROM  " + "        bstock.bstock.stock_day_price sdp " + "    WHERE  "
            + "        sdp.trading_day <= :tradingDate " + "        AND sdp.closing_price NOT LIKE '%-%' "
            + "        AND sdp.closing_price != '' " + ") AS ranked_data "
            + "WHERE rn <= :limit AND ranked_data.line_k_value IS NOT NULL "
            + "GROUP BY ranked_data.stock_code " + "HAVING COUNT(*) = :limit "
            + "AND SUM(CASE WHEN line_k_value <= 20 THEN 1 ELSE 0 END) = COUNT(*) "
            + "ORDER BY ranked_data.stock_code, start_day ";

    private static final String FIND_K_VALUE_CONTINUE_UPPER_EIGHTY_TEWNTY_BY_RANGE = "SELECT  " + "    stock_code, "
            + "    MAX(trading_day) AS start_day, " + "    MIN(trading_day) AS end_day, "
            + "    COUNT(*) AS total_days, "
            + "    SUM(CASE WHEN line_k_value >= 80 THEN 1 ELSE 0 END) AS required_count " + "FROM ( "
            + "    SELECT  " + "        stock_code, " + "        trading_day, "
            + "        " + String.format(SAFE_NUMERIC, "sdp.line_k_value", "sdp.line_k_value") + " AS line_k_value, "
            + "        ROW_NUMBER() OVER (PARTITION BY stock_code ORDER BY trading_day DESC) AS rn " + "    FROM  "
            + "        bstock.bstock.stock_day_price sdp " + "    WHERE  " + "        sdp.trading_day <= :tradingDay "
            + "        AND sdp.closing_price NOT LIKE '%-%' " + "        AND sdp.closing_price != '' "
            + ") AS ranked_data "
            + "WHERE rn <= :limit AND ranked_data.line_k_value IS NOT NULL "
            + "GROUP BY stock_code " + "HAVING COUNT(*) = :limit "
            + "AND SUM(CASE WHEN line_k_value >= 80 THEN 1 ELSE 0 END) = COUNT(*) "
            + "ORDER BY stock_code, start_day ";

    private static final String FIND_DATE_RANGE_SUM_CHANGE_RATE_QUERY = " select stock_code, total_change_rate from ( "
            + "    SELECT  "
            + "        stock_code, "
            + "        SUM(change_rate) AS total_change_rate  "
            + "    FROM ( "
            + "        SELECT  "
            + "            stock_code, "
            + "            trading_day, "
            + "            ROW_NUMBER() OVER (PARTITION BY stock_code ORDER BY trading_day DESC) AS rn, "
            + "            " + String.format(SAFE_NUMERIC, "sdp.change_rate", "sdp.change_rate") + " AS change_rate  "
            + "        FROM  "
            + "            bstock.bstock.stock_day_price sdp "
            + "        WHERE  "
            + "            sdp.trading_day <= :tradingDay "
            + "            AND sdp.closing_price NOT LIKE '%-%' "
            + "            AND sdp.closing_price != '' "
            + "    ) AS ranked_data "
            + "    WHERE rn <= :limit AND ranked_data.change_rate IS NOT NULL "
            + "    GROUP BY stock_code HAVING COUNT(*) = :limit "
            + "    ORDER BY stock_code"
            + ") result_stock_day_price "
            + "where result_stock_day_price.total_change_rate >= :totalRate  ";

    private static final String FIND_DATE_RANGE_MA_TREND_QUERY = "SELECT *  " + "FROM ( " + "    SELECT   "
            + "        stock_code,  " + "        MIN(trading_day) AS first_day,  "
            + "        MAX(trading_day) AS last_day,  " + "        CASE   "
            + "            WHEN MIN(CASE WHEN rn = 1 THEN CAST(five_ma AS NUMERIC) END) = 0 OR MIN(CASE WHEN rn = :fiveDaysSlopeLimit THEN CAST(five_ma AS NUMERIC) END) = 0 THEN 'flat' "
            + "            WHEN MIN(CASE WHEN rn = 1 THEN CAST(five_ma AS NUMERIC) END) / MIN(CASE WHEN rn = :fiveDaysSlopeLimit THEN CAST(five_ma AS NUMERIC) END) > 1 THEN 'up' "
            + "            WHEN MIN(CASE WHEN rn = 1 THEN CAST(five_ma AS NUMERIC) END) / MIN(CASE WHEN rn = :fiveDaysSlopeLimit THEN CAST(five_ma AS NUMERIC) END) < 1 THEN 'down' "
            + "            ELSE 'flat' " + "        END AS five_ma_slope,  " + "        CASE   "
            + "            WHEN MIN(CASE WHEN rn = 1 THEN CAST(ten_ma AS NUMERIC) END) = 0 OR MIN(CASE WHEN rn = :tenDaysSlopeLimit THEN CAST(ten_ma AS NUMERIC) END) = 0 THEN 'flat' "
            + "            WHEN MIN(CASE WHEN rn = 1 THEN CAST(ten_ma AS NUMERIC) END) / MIN(CASE WHEN rn = :tenDaysSlopeLimit THEN CAST(ten_ma AS NUMERIC) END) > 1 THEN 'up' "
            + "            WHEN MIN(CASE WHEN rn = 1 THEN CAST(ten_ma AS NUMERIC) END) / MIN(CASE WHEN rn = :tenDaysSlopeLimit THEN CAST(ten_ma AS NUMERIC) END) < 1 THEN 'down' "
            + "            ELSE 'flat' " + "        END AS ten_ma_slope, " + "        CASE   "
            + "            WHEN MIN(CASE WHEN rn = 1 THEN CAST(twenty_ma AS NUMERIC) END) = 0 OR MIN(CASE WHEN rn = :twentyDaysSlopeLimit THEN CAST(twenty_ma AS NUMERIC) END) = 0 THEN 'flat' "
            + "            WHEN MIN(CASE WHEN rn = 1 THEN CAST(twenty_ma AS NUMERIC) END) / MIN(CASE WHEN rn = :twentyDaysSlopeLimit THEN CAST(twenty_ma AS NUMERIC) END) > 1 THEN 'up' "
            + "            WHEN MIN(CASE WHEN rn = 1 THEN CAST(twenty_ma AS NUMERIC) END) / MIN(CASE WHEN rn = :twentyDaysSlopeLimit THEN CAST(twenty_ma AS NUMERIC) END) < 1 THEN 'down' "
            + "            ELSE 'flat' " + "        END AS twenty_ma_slope, " + "        CASE   "
            + "            WHEN MIN(CASE WHEN rn = 1 THEN CAST(sixty_ma AS NUMERIC) END) = 0 OR MIN(CASE WHEN rn = :sixtyDaysSlopeLimit THEN CAST(sixty_ma AS NUMERIC) END) = 0 THEN 'flat' "
            + "            WHEN MIN(CASE WHEN rn = 1 THEN CAST(sixty_ma AS NUMERIC) END) / MIN(CASE WHEN rn = :sixtyDaysSlopeLimit THEN CAST(sixty_ma AS NUMERIC) END) > 1 THEN 'up' "
            + "            WHEN MIN(CASE WHEN rn = 1 THEN CAST(sixty_ma AS NUMERIC) END) / MIN(CASE WHEN rn = :sixtyDaysSlopeLimit THEN CAST(sixty_ma AS NUMERIC) END) < 1 THEN 'down' "
            + "            ELSE 'flat' " + "        END AS sixty_ma_slope, " + "        CASE   "
            + "            WHEN MIN(CASE WHEN rn = 1 THEN CAST(one_twenty_ma AS NUMERIC) END) = 0 OR MIN(CASE WHEN rn = :oneTwentyDaysSlopeLimit THEN CAST(one_twenty_ma AS NUMERIC) END) = 0 THEN 'flat' "
            + "            WHEN MIN(CASE WHEN rn = 1 THEN CAST(one_twenty_ma AS NUMERIC) END) / MIN(CASE WHEN rn = :oneTwentyDaysSlopeLimit THEN CAST(one_twenty_ma AS NUMERIC) END) > 1 THEN 'up' "
            + "            WHEN MIN(CASE WHEN rn = 1 THEN CAST(one_twenty_ma AS NUMERIC) END) / MIN(CASE WHEN rn = :oneTwentyDaysSlopeLimit THEN CAST(one_twenty_ma AS NUMERIC) END) < 1 THEN 'down' "
            + "            ELSE 'down' " + "        END AS one_twenty_ma_slope, " + "        CASE   "
            + "            WHEN MIN(CASE WHEN rn = 1 THEN CAST(two_fourty_ma AS NUMERIC) END) = 0 OR MIN(CASE WHEN rn = :twoFourtyDaysSlopeLimit THEN CAST(two_fourty_ma AS NUMERIC) END) = 0 THEN 'flat' "
            + "            WHEN MIN(CASE WHEN rn = 1 THEN CAST(two_fourty_ma AS NUMERIC) END) / MIN(CASE WHEN rn = :twoFourtyDaysSlopeLimit THEN CAST(two_fourty_ma AS NUMERIC) END) > 1 THEN 'up' "
            + "            WHEN MIN(CASE WHEN rn = 1 THEN CAST(two_fourty_ma AS NUMERIC) END) / MIN(CASE WHEN rn = :twoFourtyDaysSlopeLimit THEN CAST(two_fourty_ma AS NUMERIC) END) < 1 THEN 'down' "
            + "            ELSE 'flat' " + "        END AS two_fourty_ma_slope " + "    FROM (  "
            + "        SELECT   " + "            stock_code,  " + "            trading_day,  "
            + "            five_ma,  " + "            ten_ma,  " + "            twenty_ma,  "
            + "            sixty_ma,  " + "            one_twenty_ma,  " + "            two_fourty_ma,  "
            + "            ROW_NUMBER() OVER (PARTITION BY stock_code ORDER BY trading_day DESC) AS rn  "
            + "        FROM   " + "            bstock.bstock.stock_day_price sdp  " + "        WHERE   "
            + "            sdp.trading_day <= :tradingDay " + "            AND sdp.closing_price NOT LIKE '%-%'  "
            + "            AND sdp.closing_price != ''  " + "    ) AS ranked_data  " + "    WHERE 1 = 1  "
            + "    GROUP BY stock_code  " + ") AS ma_results  " + "WHERE 1 = 1 %dynanicCondition "
            + "ORDER BY stock_code ";

    private void ensureUtf8ClientEncoding() {
        em.createNativeQuery("SET client_encoding TO 'UTF8'").executeUpdate();
    }

    @Transactional
    public List<String> findKvalueUnderTwentyByDateRange(Date startDate, Integer limit) {
        ensureUtf8ClientEncoding();

        Query query = em.createNativeQuery(FIND_K_VALUE_CONTINUE_UNDER_TEWNTY_BY_RANGE, Tuple.class);
        query.setParameter("limit", limit);
        query.setParameter("tradingDate", startDate);
        @SuppressWarnings("unchecked")
        List<Tuple> tuples = (List<Tuple>) query.getResultList();
        return tuples.stream().map(tuple -> tuple.get("stock_code").toString()).toList();
    }

    @Transactional
    public List<String> findKvalueUpperEightByDateRange(Date startDate, Integer limit) {
        ensureUtf8ClientEncoding();

        Query query = em.createNativeQuery(FIND_K_VALUE_CONTINUE_UPPER_EIGHTY_TEWNTY_BY_RANGE, Tuple.class);
        query.setParameter("limit", limit);
        query.setParameter("tradingDay", startDate);
        @SuppressWarnings("unchecked")
        List<Tuple> tuples = (List<Tuple>) query.getResultList();
        return tuples.stream().map(tuple -> tuple.get("stock_code").toString()).toList();
    }

    @SuppressWarnings("unchecked")
    @Transactional
    public List<String> findByDateRangeChangeRateOverFilter(Date startDate, Integer limit, String totalChangeRate) {
        ensureUtf8ClientEncoding();

        Query query = em.createNativeQuery(FIND_DATE_RANGE_SUM_CHANGE_RATE_QUERY, Tuple.class);
        query.setParameter("tradingDay", startDate);
        query.setParameter("limit", limit);
        query.setParameter("totalRate", Integer.valueOf(totalChangeRate));

        List<Tuple> tuples = (List<Tuple>) query.getResultList();
        return tuples.stream().map(tuple -> tuple.get("stock_code").toString()).toList();
    }

    @Transactional
    public List<String> findByDateRangeMaChangeFilter(List<DynamicFilterStockPriceCondition> maConditions,
            Date startDate, Integer fiveDaysSlopeLimit, Integer tenDaysSlopeLimit, Integer twentyDaysSlopeLimit,
            Integer sixtyDaysSlopeLimit, Integer oneTwentyDaysSlopeLimit, Integer twoFourtyDaysSlopeLimit) {

        ensureUtf8ClientEncoding();

        StringBuilder sb = new StringBuilder(FIND_DATE_RANGE_MA_TREND_QUERY);
        StringBuilder dynamicConditionSb = new StringBuilder();
        Map<String, Object> queryConditionMap = Maps.newHashMap();

        maConditions.forEach(maCondition -> {
            String clause = buildConditionClause(StringUtils.EMPTY, maCondition, queryConditionMap);
            if (StringUtils.isNotBlank(clause)) {
                dynamicConditionSb.append(clause);
            }
        });

        int start = sb.indexOf("%dynanicCondition");
        if (start != -1) {
            sb.replace(start, start + "%dynanicCondition".length(), dynamicConditionSb.toString());
        }

        Query query = em.createNativeQuery(sb.toString(), Tuple.class);

        queryConditionMap.forEach((k, v) -> query.setParameter(k, v));

        query.setParameter("tradingDay", startDate);
        query.setParameter("fiveDaysSlopeLimit", fiveDaysSlopeLimit);
        query.setParameter("tenDaysSlopeLimit", tenDaysSlopeLimit);
        query.setParameter("twentyDaysSlopeLimit", twentyDaysSlopeLimit);
        query.setParameter("sixtyDaysSlopeLimit", sixtyDaysSlopeLimit);
        query.setParameter("oneTwentyDaysSlopeLimit", oneTwentyDaysSlopeLimit);
        query.setParameter("twoFourtyDaysSlopeLimit", twoFourtyDaysSlopeLimit);

        @SuppressWarnings("unchecked")
        List<Tuple> tuples = (List<Tuple>) query.getResultList();
        return tuples.stream().map(tuple -> tuple.get("stock_code").toString()).toList();
    }

    private static String buildConditionClause(String aliasPrefix, DynamicFilterStockPriceCondition condition,
            Map<String, Object> parameterMap) {

        String column = condition.getName();
        List<String> values = condition.getValue();
        String operator = condition.getOperator();

        if (column == null || column.isEmpty() || operator == null || operator.isEmpty()) {
            return null;
        }

        switch (operator.toLowerCase()) {
            case "notconcerned":
            case "eq":
                parameterMap.put(column, values.get(0));
                if (StringUtils.isBlank(aliasPrefix)) return " and " + column + " = :" + column;
                return " and " + aliasPrefix + "." + column + " = :" + column;

            case "lte":
                parameterMap.put(column, values.get(0));
                if (StringUtils.isBlank(aliasPrefix)) return " and " + column + " <= :" + column;
                return " and " + aliasPrefix + "." + column + " <= :" + column;

            case "gte":
                parameterMap.put(column, values.get(0));
                if (StringUtils.isBlank(aliasPrefix)) return " and " + column + " >= :" + column;
                return " and " + aliasPrefix + "." + column + " >= :" + column;

            case "lt":
                parameterMap.put(column, values.get(0));
                if (StringUtils.isBlank(aliasPrefix)) return " and " + column + " < :" + column;
                return " and " + aliasPrefix + "." + column + " < :" + column;

            case "gt":
                parameterMap.put(column, values.get(0));
                if (StringUtils.isBlank(aliasPrefix)) return " and " + column + " > :" + column;
                return " and " + aliasPrefix + "." + column + " > :" + column;

            case "in":
                parameterMap.put(column, values);
                if (StringUtils.isBlank(aliasPrefix)) return " and " + column + " IN (:" + column + ")";
                return " and " + aliasPrefix + "." + column + " IN (:" + column + ")";

            case "between":
                if (values.size() == 2) {
                    parameterMap.put(column + "_start", values.get(0));
                    parameterMap.put(column + "_end", values.get(1));
                    if (StringUtils.isBlank(aliasPrefix))
                        return " and " + column + " BETWEEN :" + column + "_start AND :" + column + "_end";
                    return " "
                    		+ "and " + aliasPrefix + "." + column + " BETWEEN :" + column + "_start AND :" + column + "_end";
                }
                break;

            default:
                throw new IllegalArgumentException("Unsupported operator: " + operator);
        }

        return null;
    }
}