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

	private static final String FIND_K_VALUE_CONTINUE_UNDER_TEWNTY_BY_RANGE ="""
			SELECT
			    sdp.stock_code,
			    MAX(sdp.trading_day) AS start_day,
			    MIN(sdp.trading_day) AS end_day,
			    COUNT(*) AS total_days,
			    SUM(CASE WHEN CAST(sdp.line_k_value AS NUMERIC) <= 20 THEN 1 ELSE 0 END) AS required_count
			FROM bstock.stock_day_price_rank sdp
			WHERE sdp.rank_no <= :limit
			  AND sdp.trading_day <= :tradingDate
			GROUP BY sdp.stock_code
			HAVING COUNT(*) = :limit
			   AND SUM(CASE WHEN CAST(sdp.line_k_value AS NUMERIC) <= 20 THEN 1 ELSE 0 END) = COUNT(*)
			ORDER BY sdp.stock_code
			""";

	private static final String FIND_K_VALUE_CONTINUE_UPPER_EIGHTY_TEWNTY_BY_RANGE = """
		    SELECT
		        sdp.stock_code,
		        MAX(sdp.trading_day) AS start_day,
		        MIN(sdp.trading_day) AS end_day,
		        COUNT(*) AS total_days,
		        SUM(
		            CASE 
		                WHEN CAST(sdp.line_k_value AS NUMERIC) >= 80 
		                THEN 1 ELSE 0 
		            END
		        ) AS required_count
		    FROM bstock.stock_day_price_rank sdp
		    WHERE sdp.rank_no <= :limit
		      AND sdp.trading_day <= :tradingDay
		      AND sdp.closing_price ~ '^[0-9]+(\\.[0-9]+)?$'
		    GROUP BY sdp.stock_code
		    HAVING COUNT(*) = :limit
		       AND SUM(
		            CASE 
		                WHEN CAST(sdp.line_k_value AS NUMERIC) >= 80 
		                THEN 1 ELSE 0 
		            END
		       ) = COUNT(*)
		    ORDER BY sdp.stock_code, start_day
		    """;

	private static final String FIND_DATE_RANGE_SUM_CHANGE_RATE_QUERY = """
		    SELECT
		        sdp.stock_code,
		        SUM(CAST(sdp.change_rate as NUMERIC)) AS total_change_rate
		    FROM bstock.stock_day_price_rank sdp
		    WHERE sdp.rank_no <= :limit
		      AND sdp.trading_day <= :tradingDay
		      AND sdp.closing_price ~ '^[0-9]+(\\.[0-9]+)?$'
		    GROUP BY sdp.stock_code
		    HAVING SUM(CAST(sdp.change_rate as NUMERIC)) >= :totalRate
		    ORDER BY sdp.stock_code
		    """;

	private static final String FIND_DATE_RANGE_MA_TREND_QUERY = """
		    SELECT *
		    FROM (
		        SELECT
		            stock_code,
		            MIN(trading_day) AS first_day,
		            MAX(trading_day) AS last_day,

		            /* 5 MA */
		            CASE
		                WHEN MAX(CASE WHEN rank_no = 1 THEN five_ma_num END) = 0
		                  OR MAX(CASE WHEN rank_no = :fiveDaysSlopeLimit THEN five_ma_num END) = 0
		                THEN 'flat'
		                WHEN MAX(CASE WHEN rank_no = 1 THEN five_ma_num END)
		                     / MAX(CASE WHEN rank_no = :fiveDaysSlopeLimit THEN five_ma_num END) > 1
		                THEN 'up'
		                WHEN MAX(CASE WHEN rank_no = 1 THEN five_ma_num END)
		                     / MAX(CASE WHEN rank_no = :fiveDaysSlopeLimit THEN five_ma_num END) < 1
		                THEN 'down'
		                ELSE 'flat'
		            END AS five_ma_slope,

		            /* 10 MA */
		            CASE
		                WHEN MAX(CASE WHEN rank_no = 1 THEN ten_ma_num END) = 0
		                  OR MAX(CASE WHEN rank_no = :tenDaysSlopeLimit THEN ten_ma_num END) = 0
		                THEN 'flat'
		                WHEN MAX(CASE WHEN rank_no = 1 THEN ten_ma_num END)
		                     / MAX(CASE WHEN rank_no = :tenDaysSlopeLimit THEN ten_ma_num END) > 1
		                THEN 'up'
		                WHEN MAX(CASE WHEN rank_no = 1 THEN ten_ma_num END)
		                     / MAX(CASE WHEN rank_no = :tenDaysSlopeLimit THEN ten_ma_num END) < 1
		                THEN 'down'
		                ELSE 'flat'
		            END AS ten_ma_slope,

		            /* 20 MA */
		            CASE
		                WHEN MAX(CASE WHEN rank_no = 1 THEN twenty_ma_num END) = 0
		                  OR MAX(CASE WHEN rank_no = :twentyDaysSlopeLimit THEN twenty_ma_num END) = 0
		                THEN 'flat'
		                WHEN MAX(CASE WHEN rank_no = 1 THEN twenty_ma_num END)
		                     / MAX(CASE WHEN rank_no = :twentyDaysSlopeLimit THEN twenty_ma_num END) > 1
		                THEN 'up'
		                WHEN MAX(CASE WHEN rank_no = 1 THEN twenty_ma_num END)
		                     / MAX(CASE WHEN rank_no = :twentyDaysSlopeLimit THEN twenty_ma_num END) < 1
		                THEN 'down'
		                ELSE 'flat'
		            END AS twenty_ma_slope,

		            /* 60 MA */
		            CASE
		                WHEN MAX(CASE WHEN rank_no = 1 THEN sixty_ma_num END) = 0
		                  OR MAX(CASE WHEN rank_no = :sixtyDaysSlopeLimit THEN sixty_ma_num END) = 0
		                THEN 'flat'
		                WHEN MAX(CASE WHEN rank_no = 1 THEN sixty_ma_num END)
		                     / MAX(CASE WHEN rank_no = :sixtyDaysSlopeLimit THEN sixty_ma_num END) > 1
		                THEN 'up'
		                WHEN MAX(CASE WHEN rank_no = 1 THEN sixty_ma_num END)
		                     / MAX(CASE WHEN rank_no = :sixtyDaysSlopeLimit THEN sixty_ma_num END) < 1
		                THEN 'down'
		                ELSE 'flat'
		            END AS sixty_ma_slope,

		            /* 120 MA */
		            CASE
		                WHEN MAX(CASE WHEN rank_no = 1 THEN one_twenty_ma_num END) = 0
		                  OR MAX(CASE WHEN rank_no = :oneTwentyDaysSlopeLimit THEN one_twenty_ma_num END) = 0
		                THEN 'flat'
		                WHEN MAX(CASE WHEN rank_no = 1 THEN one_twenty_ma_num END)
		                     / MAX(CASE WHEN rank_no = :oneTwentyDaysSlopeLimit THEN one_twenty_ma_num END) > 1
		                THEN 'up'
		                WHEN MAX(CASE WHEN rank_no = 1 THEN one_twenty_ma_num END)
		                     / MAX(CASE WHEN rank_no = :oneTwentyDaysSlopeLimit THEN one_twenty_ma_num END) < 1
		                THEN 'down'
		                ELSE 'flat'
		            END AS one_twenty_ma_slope,

		            /* 240 MA */
		            CASE
		                WHEN MAX(CASE WHEN rank_no = 1 THEN two_fourty_ma_num END) = 0
		                  OR MAX(CASE WHEN rank_no = :twoFourtyDaysSlopeLimit THEN two_fourty_ma_num END) = 0
		                THEN 'flat'
		                WHEN MAX(CASE WHEN rank_no = 1 THEN two_fourty_ma_num END)
		                     / MAX(CASE WHEN rank_no = :twoFourtyDaysSlopeLimit THEN two_fourty_ma_num END) > 1
		                THEN 'up'
		                WHEN MAX(CASE WHEN rank_no = 1 THEN two_fourty_ma_num END)
		                     / MAX(CASE WHEN rank_no = :twoFourtyDaysSlopeLimit THEN two_fourty_ma_num END) < 1
		                THEN 'down'
		                ELSE 'flat'
		            END AS two_fourty_ma_slope

		        FROM (
		            SELECT
		                stock_code,
		                trading_day,
		                rank_no,

		                CASE WHEN five_ma ~ '^-?[0-9]+(\\.[0-9]+)?$'
		                     THEN CAST(five_ma AS NUMERIC) END AS five_ma_num,
		                CASE WHEN ten_ma ~ '^-?[0-9]+(\\.[0-9]+)?$'
		                     THEN CAST(ten_ma AS NUMERIC) END AS ten_ma_num,
		                CASE WHEN twenty_ma ~ '^-?[0-9]+(\\.[0-9]+)?$'
		                     THEN CAST(twenty_ma AS NUMERIC) END AS twenty_ma_num,
		                CASE WHEN sixty_ma ~ '^-?[0-9]+(\\.[0-9]+)?$'
		                     THEN CAST(sixty_ma AS NUMERIC) END AS sixty_ma_num,
		                CASE WHEN one_twenty_ma ~ '^-?[0-9]+(\\.[0-9]+)?$'
		                     THEN CAST(one_twenty_ma AS NUMERIC) END AS one_twenty_ma_num,
		                CASE WHEN two_fourty_ma ~ '^-?[0-9]+(\\.[0-9]+)?$'
		                     THEN CAST(two_fourty_ma AS NUMERIC) END AS two_fourty_ma_num

		            FROM bstock.stock_day_price_rank
		            WHERE trading_day <= :tradingDay
		              AND rank_no <= :twoFourtyDaysSlopeLimit
		              AND closing_price ~ '^-?[0-9]+(\\.[0-9]+)?$'
		        ) base_data
		        GROUP BY stock_code
		    ) ma_results
		    WHERE 1 = 1
		    %dynanicCondition
		    ORDER BY stock_code
		    """;

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