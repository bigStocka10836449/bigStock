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

	private static final String FIND_K_VALUE_CONTINUE_UNDER_TEWNTY_BY_RANGE = "SELECT  "
			+ "    ranked_data.stock_code,  " + "    MAX(ranked_data.first_trading_day) AS start_day,  "
			+ "    MIN(ranked_data.first_trading_day) AS end_day,  " + "    COUNT(*) AS total_days,  "
			+ "    SUM(CASE WHEN CAST(ranked_data.line_k_value AS NUMERIC) <= 20 THEN 1 ELSE 0 END) AS required_count  "
			+ "FROM (  " + "    SELECT  " + "        swp.stock_code,  " + "        swp.first_trading_day,  "
			+ "        CAST(swp.line_k_value AS NUMERIC) AS line_k_value,  "
			+ "        ROW_NUMBER() OVER (PARTITION BY swp.stock_code ORDER BY swp.first_trading_day DESC) AS rn  "
			+ "    FROM  " + "        bstock.bstock.stock_week_price swp  " + "    WHERE  "
			+ "        swp.first_trading_day <= :tradingDay  " + ") AS ranked_data  " + "WHERE rn <= :limit  "
			+ "GROUP BY ranked_data.stock_code  " + "HAVING COUNT(*) = :limit "
			+ "AND SUM(CASE WHEN CAST(line_k_value AS NUMERIC) <= 20 THEN 1 ELSE 0 END) = COUNT(*)  "
			+ "ORDER BY ranked_data.stock_code, start_day;";

	private static final String FIND_K_VALUE_CONTINUE_UPPER_EIGHTY_TEWNTY_BY_RANGE = "SELECT  "
			+ "    ranked_data.stock_code,  " + "    MAX(ranked_data.first_trading_day) AS start_day,  "
			+ "    MIN(ranked_data.first_trading_day) AS end_day,  " + "    COUNT(*) AS total_days,  "
			+ "    SUM(CASE WHEN CAST(ranked_data.line_k_value AS NUMERIC) <= 20 THEN 1 ELSE 0 END) AS required_count  "
			+ "FROM (  " + "    SELECT  " + "        swp.stock_code,  " + "        swp.first_trading_day,  "
			+ "        CAST(swp.line_k_value AS NUMERIC) AS line_k_value,  "
			+ "        ROW_NUMBER() OVER (PARTITION BY swp.stock_code ORDER BY swp.first_trading_day DESC) AS rn  "
			+ "    FROM  " + "        bstock.bstock.stock_week_price swp  " + "    WHERE  "
			+ "        swp.first_trading_day <= :tradingDay  " + ") AS ranked_data  " + "WHERE rn <= :limit  "
			+ "GROUP BY ranked_data.stock_code  " + "HAVING COUNT(*) = :limit "
			+ "AND SUM(CASE WHEN CAST(line_k_value AS NUMERIC) >= 80 THEN 1 ELSE 0 END) = COUNT(*)  "
			+ "ORDER BY ranked_data.stock_code, start_day";


	private static final String FIND_DATE_RANGE_MA_TREND_QUERY = "SELECT *  " + "FROM ( " + "    SELECT   "
			+ "        stock_code,  " + "        MIN(first_trading_day) AS first_day,  "
			+ "        MAX(first_trading_day) AS last_day,  " + "        CASE   "
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
			+ "        SELECT   " + "            stock_code,  " + "            first_trading_day,  "
			+ "            five_ma,  " + "            ten_ma,  " + "            twenty_ma,  "
			+ "            sixty_ma,  " + "            one_twenty_ma,  " + "            two_fourty_ma,  "
			+ "            ROW_NUMBER() OVER (PARTITION BY stock_code ORDER BY first_trading_day DESC) AS rn  "
			+ "        FROM   " + "            bstock.bstock.stock_week_price sdp  " + "        WHERE   "
			+ "            sdp.first_trading_day <= :tradingDay " 
			+ "    ) AS ranked_data  " + "    WHERE 1 = 1  "
			+ "    GROUP BY stock_code  " + ") AS ma_results  " + "WHERE 1 = 1 %dynanicCondition "
			+ "ORDER BY stock_code ";
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
		int start = sb.indexOf("%dynanicCondition");
		if (start != -1) {
			sb.replace(start, start + "%dynanicCondition".length(), dynamicConditionSb.toString());
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
