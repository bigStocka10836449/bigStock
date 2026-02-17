package com.bigstock.biz.domain;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.bigstock.sharedComponent.dto.DynamicFilterStockPriceCondition;
import com.google.common.collect.Maps;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StockWeekPriceNativeQueryService {

	private final EntityManagerFactory entityManagerFactory;

	private static final String FIND_K_VALUE_CONTINUE_UNDER_TEWNTY_BY_RANGE = """
		    SELECT
		        swpr.stock_code,
		        MAX(swpr.first_trading_day) AS start_day,
		        MIN(swpr.first_trading_day) AS end_day,
		        COUNT(*) AS total_days,
		        COUNT(*) FILTER (
		            WHERE case(swpr.line_k_value as NUMERIC) <= 20
		        ) AS required_count
		    FROM bstock.stock_week_price_rank swpr
		    WHERE swpr.first_trading_day <= :tradingDay
		      AND swpr.rank_no <= :limit
		    GROUP BY swpr.stock_code
		    HAVING COUNT(*) = :limit
		       AND COUNT(*) FILTER (
		               WHERE cast(swpr.line_k_value as NUMERIC) <= 20
		           ) = :limit
		    ORDER BY swpr.stock_code, start_day
		    """;

	private static final String FIND_K_VALUE_CONTINUE_UPPER_EIGHTY_TEWNTY_BY_RANGE = """
		    SELECT
		        swpr.stock_code,
		        MAX(swpr.first_trading_day) AS start_day,
		        MIN(swpr.first_trading_day) AS end_day,
		        COUNT(*) AS total_days,
		        COUNT(*) FILTER (
		            WHERE cast(swpr.line_k_value as NUMERIC) >= 80
		        ) AS required_count
		    FROM bstock.stock_week_price_rank swpr
		    WHERE swpr.first_trading_day <= :tradingDay
		      AND swpr.rank_no <= :limit
		    GROUP BY swpr.stock_code
		    HAVING COUNT(*) = :limit
		       AND COUNT(*) FILTER (
		               WHERE cast(swpr.line_k_value as NUMERIC) >= 80
		           ) = :limit
		    ORDER BY swpr.stock_code, start_day
		    """;

	private static final String FIND_DATE_RANGE_MA_TREND_QUERY = """
		    SELECT *
		    FROM (
		        SELECT
		            swpr.stock_code,
		            MIN(swpr.first_trading_day) AS first_day,
		            MAX(swpr.first_trading_day) AS last_day,

		            /* 5 MA */
		            CASE
		                WHEN MAX(CASE WHEN swpr.rank_no = 1 
		                              THEN CAST(swpr.five_ma AS NUMERIC) END) = 0
		                  OR MAX(CASE WHEN swpr.rank_no = :fiveDaysSlopeLimit 
		                              THEN CAST(swpr.five_ma AS NUMERIC) END) = 0
		                THEN 'flat'
		                WHEN MAX(CASE WHEN swpr.rank_no = 1 
		                              THEN CAST(swpr.five_ma AS NUMERIC) END)
		                   / MAX(CASE WHEN swpr.rank_no = :fiveDaysSlopeLimit 
		                              THEN CAST(swpr.five_ma AS NUMERIC) END) > 1
		                THEN 'up'
		                WHEN MAX(CASE WHEN swpr.rank_no = 1 
		                              THEN CAST(swpr.five_ma AS NUMERIC) END)
		                   / MAX(CASE WHEN swpr.rank_no = :fiveDaysSlopeLimit 
		                              THEN CAST(swpr.five_ma AS NUMERIC) END) < 1
		                THEN 'down'
		                ELSE 'flat'
		            END AS five_ma_slope,

		            /* 10 MA */
		            CASE
		                WHEN MAX(CASE WHEN swpr.rank_no = 1 
		                              THEN CAST(swpr.ten_ma AS NUMERIC) END) = 0
		                  OR MAX(CASE WHEN swpr.rank_no = :tenDaysSlopeLimit 
		                              THEN CAST(swpr.ten_ma AS NUMERIC) END) = 0
		                THEN 'flat'
		                WHEN MAX(CASE WHEN swpr.rank_no = 1 
		                              THEN CAST(swpr.ten_ma AS NUMERIC) END)
		                   / MAX(CASE WHEN swpr.rank_no = :tenDaysSlopeLimit 
		                              THEN CAST(swpr.ten_ma AS NUMERIC) END) > 1
		                THEN 'up'
		                WHEN MAX(CASE WHEN swpr.rank_no = 1 
		                              THEN CAST(swpr.ten_ma AS NUMERIC) END)
		                   / MAX(CASE WHEN swpr.rank_no = :tenDaysSlopeLimit 
		                              THEN CAST(swpr.ten_ma AS NUMERIC) END) < 1
		                THEN 'down'
		                ELSE 'flat'
		            END AS ten_ma_slope,

		            /* 20 MA */
		            CASE
		                WHEN MAX(CASE WHEN swpr.rank_no = 1 
		                              THEN CAST(swpr.twenty_ma AS NUMERIC) END) = 0
		                  OR MAX(CASE WHEN swpr.rank_no = :twentyDaysSlopeLimit 
		                              THEN CAST(swpr.twenty_ma AS NUMERIC) END) = 0
		                THEN 'flat'
		                WHEN MAX(CASE WHEN swpr.rank_no = 1 
		                              THEN CAST(swpr.twenty_ma AS NUMERIC) END)
		                   / MAX(CASE WHEN swpr.rank_no = :twentyDaysSlopeLimit 
		                              THEN CAST(swpr.twenty_ma AS NUMERIC) END) > 1
		                THEN 'up'
		                WHEN MAX(CASE WHEN swpr.rank_no = 1 
		                              THEN CAST(swpr.twenty_ma AS NUMERIC) END)
		                   / MAX(CASE WHEN swpr.rank_no = :twentyDaysSlopeLimit 
		                              THEN CAST(swpr.twenty_ma AS NUMERIC) END) < 1
		                THEN 'down'
		                ELSE 'flat'
		            END AS twenty_ma_slope,

		            /* 60 MA */
		            CASE
		                WHEN MAX(CASE WHEN swpr.rank_no = 1 
		                              THEN CAST(swpr.sixty_ma AS NUMERIC) END) = 0
		                  OR MAX(CASE WHEN swpr.rank_no = :sixtyDaysSlopeLimit 
		                              THEN CAST(swpr.sixty_ma AS NUMERIC) END) = 0
		                THEN 'flat'
		                WHEN MAX(CASE WHEN swpr.rank_no = 1 
		                              THEN CAST(swpr.sixty_ma AS NUMERIC) END)
		                   / MAX(CASE WHEN swpr.rank_no = :sixtyDaysSlopeLimit 
		                              THEN CAST(swpr.sixty_ma AS NUMERIC) END) > 1
		                THEN 'up'
		                WHEN MAX(CASE WHEN swpr.rank_no = 1 
		                              THEN CAST(swpr.sixty_ma AS NUMERIC) END)
		                   / MAX(CASE WHEN swpr.rank_no = :sixtyDaysSlopeLimit 
		                              THEN CAST(swpr.sixty_ma AS NUMERIC) END) < 1
		                THEN 'down'
		                ELSE 'flat'
		            END AS sixty_ma_slope,

		            /* 120 MA */
		            CASE
		                WHEN MAX(CASE WHEN swpr.rank_no = 1 
		                              THEN CAST(swpr.one_twenty_ma AS NUMERIC) END) = 0
		                  OR MAX(CASE WHEN swpr.rank_no = :oneTwentyDaysSlopeLimit 
		                              THEN CAST(swpr.one_twenty_ma AS NUMERIC) END) = 0
		                THEN 'flat'
		                WHEN MAX(CASE WHEN swpr.rank_no = 1 
		                              THEN CAST(swpr.one_twenty_ma AS NUMERIC) END)
		                   / MAX(CASE WHEN swpr.rank_no = :oneTwentyDaysSlopeLimit 
		                              THEN CAST(swpr.one_twenty_ma AS NUMERIC) END) > 1
		                THEN 'up'
		                WHEN MAX(CASE WHEN swpr.rank_no = 1 
		                              THEN CAST(swpr.one_twenty_ma AS NUMERIC) END)
		                   / MAX(CASE WHEN swpr.rank_no = :oneTwentyDaysSlopeLimit 
		                              THEN CAST(swpr.one_twenty_ma AS NUMERIC) END) < 1
		                THEN 'down'
		                ELSE 'flat'
		            END AS one_twenty_ma_slope,

		            /* 240 MA */
		            CASE
		                WHEN MAX(CASE WHEN swpr.rank_no = 1 
		                              THEN CAST(swpr.two_fourty_ma AS NUMERIC) END) = 0
		                  OR MAX(CASE WHEN swpr.rank_no = :twoFourtyDaysSlopeLimit 
		                              THEN CAST(swpr.two_fourty_ma AS NUMERIC) END) = 0
		                THEN 'flat'
		                WHEN MAX(CASE WHEN swpr.rank_no = 1 
		                              THEN CAST(swpr.two_fourty_ma AS NUMERIC) END)
		                   / MAX(CASE WHEN swpr.rank_no = :twoFourtyDaysSlopeLimit 
		                              THEN CAST(swpr.two_fourty_ma AS NUMERIC) END) > 1
		                THEN 'up'
		                WHEN MAX(CASE WHEN swpr.rank_no = 1 
		                              THEN CAST(swpr.two_fourty_ma AS NUMERIC) END)
		                   / MAX(CASE WHEN swpr.rank_no = :twoFourtyDaysSlopeLimit 
		                              THEN CAST(swpr.two_fourty_ma AS NUMERIC) END) < 1
		                THEN 'down'
		                ELSE 'flat'
		            END AS two_fourty_ma_slope

		        FROM bstock.stock_week_price_rank swpr
		        WHERE swpr.first_trading_day <= :tradingDay
		          AND swpr.rank_no IN (
		                1,
		                :fiveDaysSlopeLimit,
		                :tenDaysSlopeLimit,
		                :twentyDaysSlopeLimit,
		                :sixtyDaysSlopeLimit,
		                :oneTwentyDaysSlopeLimit,
		                :twoFourtyDaysSlopeLimit
		          )
		        GROUP BY swpr.stock_code
		    ) AS ma_results
		    WHERE 1 = 1
		    %dynamicCondition
		    ORDER BY stock_code
		    """;
	public List<String> findKvalueUnderTwentyByDateRange(Date startDate, Integer limit) {
		StringBuilder sb = new StringBuilder(FIND_K_VALUE_CONTINUE_UNDER_TEWNTY_BY_RANGE);
		EntityManager em = entityManagerFactory.createEntityManager();
		Query query = em.createNativeQuery(sb.toString(), Tuple.class);
		query.setParameter("limit", limit);
		query.setParameter("tradingDay", startDate);
		List<Tuple> tuples = (List<Tuple>) query.getResultList();
		return tuples.stream().map(tuple -> tuple.get("stock_code").toString()).toList();
	}

	public List<String> findKvalueUpperEightByDateRange(Date startDate, Integer limit) {
		StringBuilder sb = new StringBuilder(FIND_K_VALUE_CONTINUE_UPPER_EIGHTY_TEWNTY_BY_RANGE);
		EntityManager em = entityManagerFactory.createEntityManager();
		Query query = em.createNativeQuery(sb.toString(), Tuple.class);
		query.setParameter("limit", limit);
		query.setParameter("tradingDay", startDate);
		List<Tuple> tuples = (List<Tuple>) query.getResultList();
		return tuples.stream().map(tuple -> tuple.get("stock_code").toString()).toList();
	}

	@Transactional
	public List<String> findByDateRangeMaChangeFilter(List<DynamicFilterStockPriceCondition> maConditions,
			Date startDate, Integer fiveDaysSlopeLimit, Integer tenDaysSlopeLimit, Integer twentyDaysSlopeLimit,
			Integer sixtyDaysSlopeLimit, Integer oneTwentyDaysSlopeLimit, Integer twoFourtyDaysSlopeLimit) {
		StringBuilder sb = new StringBuilder(FIND_DATE_RANGE_MA_TREND_QUERY);
		StringBuilder dynamicConditionSb = new StringBuilder();
		Map<String, Object> queryConditionMap = Maps.newHashMap();
		maConditions.stream().forEach(maCondition -> {
			dynamicConditionSb.append(buildConditionClause(StringUtils.EMPTY, maCondition, queryConditionMap));
		});
		int start = sb.indexOf("%dynamicCondition");
		if (start != -1) {
			sb.replace(start, start + "%dynamicCondition".length(), dynamicConditionSb.toString());
		}
		EntityManager em = entityManagerFactory.createEntityManager();
		Query query = em.createNativeQuery(sb.toString(), Tuple.class);
		queryConditionMap.entrySet().forEach(entry -> {
			String key = entry.getKey();
			String value = entry.getValue().toString();
			query.setParameter(key, value);
		});
		query.setParameter("tradingDay", startDate);
		query.setParameter("fiveDaysSlopeLimit", fiveDaysSlopeLimit);
		query.setParameter("tenDaysSlopeLimit", tenDaysSlopeLimit);
		query.setParameter("twentyDaysSlopeLimit", twentyDaysSlopeLimit);
		query.setParameter("sixtyDaysSlopeLimit", sixtyDaysSlopeLimit);
		query.setParameter("oneTwentyDaysSlopeLimit", oneTwentyDaysSlopeLimit);
		query.setParameter("twoFourtyDaysSlopeLimit", twoFourtyDaysSlopeLimit);
		List<Tuple> tuples = (List<Tuple>) query.getResultList();
		return tuples.stream().map(tuple -> tuple.get("stock_code").toString()).toList();
	}

	private static String buildConditionClause(String aliasPrefix, DynamicFilterStockPriceCondition condition,
			Map<String, Object> parameterMap) {
		String column = condition.getName();
		List<String> values = condition.getValue();
		String operator = condition.getOperator();

		if (column == null || column.isEmpty() || operator == null || operator.isEmpty()) {
			return null; // 无效条件，忽略
		}

		switch (operator.toLowerCase()) {
		// 這個變數不用考慮operator
		case "notconcerned":
			parameterMap.put(column, values.get(0)); // 将值放入 Map
			if (StringUtils.isBlank(aliasPrefix)) {
				return " and " + column + " = :" + column;
			}
			return " and " + aliasPrefix + "." + column + " = :" + column;
		case "eq":
			parameterMap.put(column, values.get(0)); // 将值放入 Map
			if (StringUtils.isBlank(aliasPrefix)) {
				return " and " + column + " = :" + column;
			}
			return " and " + aliasPrefix + "." + column + " = :" + column;
		case "lte":
			parameterMap.put(column, values.get(0));
			if (StringUtils.isBlank(aliasPrefix)) {
				return " and " + column + " <= :" + column;
			}
			return " and " + aliasPrefix + "." + column + " <= :" + column;
		case "gte":
			parameterMap.put(column, values.get(0));
			if (StringUtils.isBlank(aliasPrefix)) {
				return " and " + column + " >= :" + column;
			}
			return " and " + aliasPrefix + "." + column + " >= :" + column;
		case "lt":
			parameterMap.put(column, values.get(0));
			if (StringUtils.isBlank(aliasPrefix)) {
				return " and " + column + " < :" + column;
			}
			return " and " + aliasPrefix + "." + column + " < :" + column;
		case "gt":
			parameterMap.put(column, values.get(0));
			if (StringUtils.isBlank(aliasPrefix)) {
				return " and " + column + " > :" + column;
			}
			return " and " + aliasPrefix + "." + column + " > :" + column;
		case "in":
			parameterMap.put(column, values); // IN 操作直接放入 List
			if (StringUtils.isBlank(aliasPrefix)) {
				return " and " + column + " IN (:" + column + ")";
			}
			return " and " + aliasPrefix + "." + column + " IN (:" + column + ")";
		case "between":
			if (values.size() == 2) {
				parameterMap.put(column + "_start", values.get(0));
				parameterMap.put(column + "_end", values.get(1));
				if (StringUtils.isBlank(aliasPrefix)) {
					return " and " + column + " BETWEEN :" + column + "_start AND :" + column + "_end";
				}
				return " and " + aliasPrefix + "." + column + " BETWEEN :" + column + "_start AND :" + column + "_end";
			}
			break;
		default:
			throw new IllegalArgumentException("Unsupported operator: " + operator);
		}

		return null;
	}
}
