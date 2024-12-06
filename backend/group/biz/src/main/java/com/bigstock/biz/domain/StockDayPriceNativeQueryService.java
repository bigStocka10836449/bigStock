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
public class StockDayPriceNativeQueryService {
	
	private final EntityManagerFactory entityManagerFactory;
	
	
	private static final String FIND_K_VALUE_CONTINUE_UNDER_TEWNTY_BY_RANGE =  "SELECT  "
			+ "    stock_code, "
			+ "    MAX(trading_day) AS start_day, "
			+ "    MIN(trading_day) AS end_day, "
			+ "    COUNT(*) AS total_days, "
			+ "    SUM(CASE WHEN CAST(line_k_value AS NUMERIC) <= 20 THEN 1 ELSE 0 END) AS required_count "
			+ "FROM ( "
			+ "    SELECT  "
			+ "        stock_code, "
			+ "        trading_day, "
			+ "        CAST(line_k_value AS NUMERIC) AS line_k_value, "
			+ "        ROW_NUMBER() OVER (PARTITION BY stock_code ORDER BY trading_day DESC) AS rn "
			+ "    FROM  "
			+ "        bstock.bstock.stock_day_price sdp "
			+ "    WHERE  "
			+ "        sdp.trading_day <= :tradingDate "
			+ "        AND sdp.change NOT LIKE '%X%' "
			+ "        AND sdp.change NOT LIKE '%--%' "
			+ "        AND sdp.change NOT LIKE '%除息%' "
			+ "        AND sdp.change NOT LIKE '%除權%' "
			+ "        AND sdp.closing_price NOT LIKE '%-%' "
			+ "        AND sdp.closing_price != '' "
			+ ") AS ranked_data "
			+ "WHERE rn <= :limit "
			+ "GROUP BY stock_code "
			+ "HAVING COUNT(*) = :limit "
			+ "AND SUM(CASE WHEN CAST(line_k_value AS NUMERIC) <= 20 THEN 1 ELSE 0 END) = COUNT(*) "
			+ "ORDER BY stock_code, start_day ";
	
	private static final String FIND_K_VALUE_CONTINUE_UNDER_EIGHTY_TEWNTY_BY_RANGE =  "SELECT  "
			+ "    stock_code, "
			+ "    MAX(trading_day) AS start_day, "
			+ "    MIN(trading_day) AS end_day, "
			+ "    COUNT(*) AS total_days, "
			+ "    SUM(CASE WHEN CAST(line_k_value AS NUMERIC) >= 80 THEN 1 ELSE 0 END) AS required_count "
			+ "FROM ( "
			+ "    SELECT  "
			+ "        stock_code, "
			+ "        trading_day, "
			+ "        CAST(line_k_value AS NUMERIC) AS line_k_value, "
			+ "        ROW_NUMBER() OVER (PARTITION BY stock_code ORDER BY trading_day DESC) AS rn "
			+ "    FROM  "
			+ "        bstock.bstock.stock_day_price sdp "
			+ "    WHERE  "
			+ "        sdp.trading_day <= :tradingDate "
			+ "        AND sdp.change NOT LIKE '%X%' "
			+ "        AND sdp.change NOT LIKE '%--%' "
			+ "        AND sdp.change NOT LIKE '%除息%' "
			+ "        AND sdp.change NOT LIKE '%除權%' "
			+ "        AND sdp.closing_price NOT LIKE '%-%' "
			+ "        AND sdp.closing_price != '' "
			+ ") AS ranked_data "
			+ "WHERE rn <= :limit "
			+ "GROUP BY stock_code "
			+ "HAVING COUNT(*) = :limit "
			+ "AND SUM(CASE WHEN CAST(line_k_value AS NUMERIC) <= 20 THEN 1 ELSE 0 END) = COUNT(*) "
			+ "ORDER BY stock_code, start_day ";
	
	
	
	private static final String FIND_DATE_RANGE_SUM_CHANGE_RATE_QUERY = " select stock_code, total_change_rate from ( "
			+ "	SELECT  "
			+ "    stock_code as stockCode, "
			+ "    SUM(change_rate::NUMERIC) AS total_change_rate  "
			+ "FROM ( "
			+ "    SELECT  "
			+ "        stock_code, "
			+ "        trading_day, "
			+ "        ROW_NUMBER() OVER (PARTITION BY stock_code ORDER BY trading_day DESC) AS rn, "
			+ "        change_rate::NUMERIC AS change_rate  "
			+ "    FROM  "
			+ "        bstock.bstock.stock_day_price sdp "
			+ "    WHERE  "
			+ "        sdp.trading_day <= :tradingDay "
			+ "        AND sdp.change NOT LIKE '%X%' "
			+ "        AND sdp.change NOT LIKE '%--%' "
			+ "        AND sdp.change NOT LIKE '%除息%' "
			+ "        AND sdp.change NOT LIKE '%除權%' "
			+ "        AND sdp.closing_price NOT LIKE '%-%' "
			+ "        AND sdp.closing_price != '' "
			+ ") AS ranked_data "
			+ "WHERE rn <= :limit "
			+ "GROUP BY stock_code "
			+ "ORDER BY stock_code) result_stock_day_price "
			+ "where result_stock_day_price.total_change_rate >= :totalRate  ";
	
	private static final String FIND_DATE_RANGE_MA_TREND_QUERY = "SELECT *  "
			+ "FROM ( "
			+ "    SELECT   "
			+ "        stock_code,  "
			+ "        MIN(trading_day) AS first_day,  "
			+ "        MAX(trading_day) AS last_day,  "
			+ "        -- 計算五日 MA 的斜率判斷 "
			+ "        CASE   "
			+ "            WHEN MIN(CASE WHEN rn = 1 THEN five_days_ma::NUMERIC END) = 0 OR MIN(CASE WHEN rn = 15 THEN five_days_ma::NUMERIC END) = 0 THEN 'same' "
			+ "            WHEN MIN(CASE WHEN rn = 1 THEN five_days_ma::NUMERIC END) / MIN(CASE WHEN rn = 15 THEN five_days_ma::NUMERIC END) > 1 THEN 'up' "
			+ "            WHEN MIN(CASE WHEN rn = 1 THEN five_days_ma::NUMERIC END) / MIN(CASE WHEN rn = 15 THEN five_days_ma::NUMERIC END) < 1 THEN 'down' "
			+ "            ELSE 'same' "
			+ "        END AS five_days_slope,  "
			+ "        -- 計算十日 MA 的斜率判斷 "
			+ "        CASE   "
			+ "            WHEN MIN(CASE WHEN rn = 1 THEN ten_days_ma::NUMERIC END) = 0 OR MIN(CASE WHEN rn = 15 THEN ten_days_ma::NUMERIC END) = 0 THEN 'same' "
			+ "            WHEN MIN(CASE WHEN rn = 1 THEN ten_days_ma::NUMERIC END) / MIN(CASE WHEN rn = 15 THEN ten_days_ma::NUMERIC END) > 1 THEN 'up' "
			+ "            WHEN MIN(CASE WHEN rn = 1 THEN ten_days_ma::NUMERIC END) / MIN(CASE WHEN rn = 15 THEN ten_days_ma::NUMERIC END) < 1 THEN 'down' "
			+ "            ELSE 'same' "
			+ "        END AS ten_days_slope, "
			+ "        -- 計算二十日 MA 的斜率判斷 "
			+ "        CASE   "
			+ "            WHEN MIN(CASE WHEN rn = 1 THEN twenty_days_ma::NUMERIC END) = 0 OR MIN(CASE WHEN rn = 15 THEN twenty_days_ma::NUMERIC END) = 0 THEN 'same' "
			+ "            WHEN MIN(CASE WHEN rn = 1 THEN twenty_days_ma::NUMERIC END) / MIN(CASE WHEN rn = 15 THEN twenty_days_ma::NUMERIC END) > 1 THEN 'up' "
			+ "            WHEN MIN(CASE WHEN rn = 1 THEN twenty_days_ma::NUMERIC END) / MIN(CASE WHEN rn = 15 THEN twenty_days_ma::NUMERIC END) < 1 THEN 'down' "
			+ "            ELSE 'same' "
			+ "        END AS twenty_days_slope, "
			+ "        -- 計算六十日 MA 的斜率判斷 "
			+ "        CASE   "
			+ "            WHEN MIN(CASE WHEN rn = 1 THEN sixty_days_ma::NUMERIC END) = 0 OR MIN(CASE WHEN rn = 15 THEN sixty_days_ma::NUMERIC END) = 0 THEN 'same' "
			+ "            WHEN MIN(CASE WHEN rn = 1 THEN sixty_days_ma::NUMERIC END) / MIN(CASE WHEN rn = 15 THEN sixty_days_ma::NUMERIC END) > 1 THEN 'up' "
			+ "            WHEN MIN(CASE WHEN rn = 1 THEN sixty_days_ma::NUMERIC END) / MIN(CASE WHEN rn = 15 THEN sixty_days_ma::NUMERIC END) < 1 THEN 'down' "
			+ "            ELSE 'same' "
			+ "        END AS sixty_days_slope, "
			+ "        -- 計算一百二十日 MA 的斜率判斷 "
			+ "        CASE   "
			+ "            WHEN MIN(CASE WHEN rn = 1 THEN one_twenty_days_ma::NUMERIC END) = 0 OR MIN(CASE WHEN rn = 15 THEN one_twenty_days_ma::NUMERIC END) = 0 THEN 'same' "
			+ "            WHEN MIN(CASE WHEN rn = 1 THEN one_twenty_days_ma::NUMERIC END) / MIN(CASE WHEN rn = 15 THEN one_twenty_days_ma::NUMERIC END) > 1 THEN 'up' "
			+ "            WHEN MIN(CASE WHEN rn = 1 THEN one_twenty_days_ma::NUMERIC END) / MIN(CASE WHEN rn = 15 THEN one_twenty_days_ma::NUMERIC END) < 1 THEN 'down' "
			+ "            ELSE 'down' "
			+ "        END AS one_twenty_days_slope, "
			+ "        -- 計算二百四十日 MA 的斜率判斷 "
			+ "        CASE   "
			+ "            WHEN MIN(CASE WHEN rn = 1 THEN two_fourty_days_ma::NUMERIC END) = 0 OR MIN(CASE WHEN rn = 15 THEN two_fourty_days_ma::NUMERIC END) = 0 THEN 'same' "
			+ "            WHEN MIN(CASE WHEN rn = 1 THEN two_fourty_days_ma::NUMERIC END) / MIN(CASE WHEN rn = 15 THEN two_fourty_days_ma::NUMERIC END) > 1 THEN 'up' "
			+ "            WHEN MIN(CASE WHEN rn = 1 THEN two_fourty_days_ma::NUMERIC END) / MIN(CASE WHEN rn = 15 THEN two_fourty_days_ma::NUMERIC END) < 1 THEN 'down' "
			+ "            ELSE 'same' "
			+ "        END AS two_fourty_days_slope "
			+ "    FROM (  "
			+ "        SELECT   "
			+ "            stock_code,  "
			+ "            trading_day,  "
			+ "            five_days_ma,  "
			+ "            ten_days_ma,  "
			+ "            twenty_days_ma,  "
			+ "            sixty_days_ma,  "
			+ "            one_twenty_days_ma,  "
			+ "            two_fourty_days_ma,  "
			+ "            ROW_NUMBER() OVER (PARTITION BY stock_code ORDER BY trading_day DESC) AS rn  "
			+ "        FROM   "
			+ "            bstock.bstock.stock_day_price sdp  "
			+ "        WHERE   "
			+ "            sdp.trading_day <= :tradingDay "
			+ "            AND sdp.change NOT LIKE '%X%'  "
			+ "            AND sdp.change NOT LIKE '%--%'  "
			+ "            AND sdp.change NOT LIKE '%除息%'  "
			+ "            AND sdp.change NOT LIKE '%除權%'  "
			+ "            AND sdp.closing_price NOT LIKE '%-%'  "
			+ "            AND sdp.closing_price != ''  "
			+ "    ) AS ranked_data  "
			+ "    WHERE rn <= 15  "
			+ "    GROUP BY stock_code  "
			+ ") AS ma_results  "
			+ "WHERE 1 = 1 and %dynanicCondition "
			+ "ORDER BY stock_code ";
	
	
	public List<String> findKvalueUnderTwentyByDateRange(Date startDate,Integer limit){
		StringBuilder sb = new StringBuilder(FIND_K_VALUE_CONTINUE_UNDER_TEWNTY_BY_RANGE);
		EntityManager em = entityManagerFactory.createEntityManager();
		Query query = em.createNativeQuery(sb.toString(), Tuple.class);
		query.setParameter("limit", limit);
		query.setParameter("tradingDate", startDate);
		List<Tuple> tuples = (List<Tuple>)query.getResultList();
		return tuples.stream().map(tuple -> tuple.get("stockcode").toString()).toList();
	}
	
	public List<String> findKvalueUpperEightByDateRange(Date startDate,Integer limit){
		StringBuilder sb = new StringBuilder(FIND_DATE_RANGE_SUM_CHANGE_RATE_QUERY);
		EntityManager em = entityManagerFactory.createEntityManager();
		Query query = em.createNativeQuery(sb.toString(), Tuple.class);
		query.setParameter("limit", limit);
		query.setParameter("tradingDate", startDate);
		List<Tuple> tuples = (List<Tuple>)query.getResultList();
		return tuples.stream().map(tuple -> tuple.get("stockcode").toString()).toList();
	}
	
	@SuppressWarnings("unchecked")
	@Transactional
	public List<String> findByDateRangeChangeRateOverFilter(Date startDate, Integer limit, String totalChangeRate){
		StringBuilder sb = new StringBuilder(FIND_K_VALUE_CONTINUE_UNDER_EIGHTY_TEWNTY_BY_RANGE);
		EntityManager em = entityManagerFactory.createEntityManager();
		Query query = em.createNativeQuery(sb.toString(), Tuple.class);
		query.setParameter("tradingDay", startDate);
		query.setParameter("limit", limit);
		query.setParameter("totalRate", totalChangeRate);
		List<Tuple> tuples = (List<Tuple>)query.getResultList();
		return tuples.stream().map(tuple -> tuple.get("stockcode").toString()).toList();
	}
	
	public List<String> findByDateRangeMaChangeFilter(List<DynamicFilterStockPriceCondition> maConditions,
			Integer limit) {
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
		queryConditionMap.forEach(query::setParameter);
		List<Tuple> tuples = (List<Tuple>)query.getResultList();
		return tuples.stream().map(tuple -> tuple.get("stockcode").toString()).toList();
	}
	
	private static String buildConditionClause( String aliasPrefix,DynamicFilterStockPriceCondition condition, Map<String, Object> parameterMap) {
	    String column = condition.getName();
	    List<String> values = condition.getValue();
	    String operator = condition.getOperator();

	    if (column == null || column.isEmpty() || operator == null || operator.isEmpty()) {
	        return null; // 无效条件，忽略
	    }

	    switch (operator.toLowerCase()) {
	        case "eq":
	            parameterMap.put(column, values.get(0)); // 将值放入 Map
	            return " and " + aliasPrefix + "." + column + " = :" + column;
	        case "lte":
	            parameterMap.put(column, values.get(0));
	            return " and " + aliasPrefix + "." + column + " <= :" + column;
	        case "gte":
	            parameterMap.put(column, values.get(0));
	            return " and " + aliasPrefix + "." + column + " >= :" + column;
	        case "lt":
	            parameterMap.put(column, values.get(0));
	            return " and " + aliasPrefix + "." + column + " < :" + column;
	        case "gt":
	            parameterMap.put(column, values.get(0));
	            return " and " + aliasPrefix + "." + column + " > :" + column;
	        case "in":
	            parameterMap.put(column, values); // IN 操作直接放入 List
	            return " and " + aliasPrefix + "." + column + " IN (:" + column + ")";
	        case "between":
	            if (values.size() == 2) {
	                parameterMap.put(column + "_start", values.get(0));
	                parameterMap.put(column + "_end", values.get(1));
	                return " and " + aliasPrefix + "." + column + " BETWEEN :" + column + "_start AND :" + column + "_end";
	            }
	            break;
	        default:
	            throw new IllegalArgumentException("Unsupported operator: " + operator);
	    }

	    return null;
	}
}
