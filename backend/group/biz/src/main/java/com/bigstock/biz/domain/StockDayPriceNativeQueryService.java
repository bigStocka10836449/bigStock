package com.bigstock.biz.domain;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.bigstock.sharedComponent.dto.DynamicFilterStockPriceCondition;

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
	
	private static final String FIND_DATE_RANGE_MA_TREND_QUERY = "SELECT * "
			+ "FROM ( "
			+ "    SELECT  "
			+ "        stock_code, "
			+ "        MIN(trading_day) AS first_day, "
			+ "        MAX(trading_day) AS last_day, "
			+ "        MIN(CASE WHEN rn = 1 THEN closing_price::NUMERIC END) AS first_price, "
			+ "        MIN(CASE WHEN rn = :limit THEN closing_price::NUMERIC END) AS last_price, "
			+ "        CASE  "
			+ "            WHEN MIN(CASE WHEN rn = :limit THEN closing_price::NUMERIC END) - MIN(CASE WHEN rn = 1 THEN closing_price::NUMERIC END) > 0 THEN '向上' "
			+ "            WHEN MIN(CASE WHEN rn = :limit THEN closing_price::NUMERIC END) - MIN(CASE WHEN rn = 1 THEN closing_price::NUMERIC END) < 0 THEN '向下' "
			+ "            ELSE '持平' "
			+ "        END AS ma_slope "
			+ "    FROM ( "
			+ "        SELECT  "
			+ "            stock_code, "
			+ "            trading_day, "
			+ "            closing_price, "
			+ "            ROW_NUMBER() OVER (PARTITION BY stock_code ORDER BY trading_day DESC) AS rn "
			+ "        FROM  "
			+ "            bstock.bstock.stock_day_price sdp "
			+ "        WHERE  "
			+ "            sdp.trading_day <= :tradingDay "
			+ "            AND sdp.change NOT LIKE '%X%' "
			+ "            AND sdp.change NOT LIKE '%--%' "
			+ "            AND sdp.change NOT LIKE '%除息%' "
			+ "            AND sdp.change NOT LIKE '%除權%' "
			+ "            AND sdp.closing_price NOT LIKE '%-%' "
			+ "            AND sdp.closing_price != '' "
			+ "    ) AS ranked_data "
			+ "    WHERE rn <= :limit "
			+ "    GROUP BY stock_code "
			+ ") AS ma_results "
			+ "WHERE ma_slope = :maSlope "
			+ "ORDER BY stock_code ";
	
	private static final String alias = "sdp.";
	
	@SuppressWarnings("unchecked")
	@Transactional
	public List<String> findByDateRangeChangeRateOverFilter(Date startDate, Integer limit, String totalChangeRate){
		StringBuilder sb = new StringBuilder(FIND_DATE_RANGE_SUM_CHANGE_RATE_QUERY);
		EntityManager em = entityManagerFactory.createEntityManager();
		Query query = em.createNativeQuery(sb.toString(), Tuple.class);
		query.setParameter("tradingDay", startDate);
		query.setParameter("limit", limit);
		query.setParameter("totalRate", totalChangeRate);
		List<Tuple> tuples = (List<Tuple>)query.getResultList();
		return tuples.stream().map(tuple -> tuple.get("stockcode").toString()).toList();
	}
	
	public
	
	private void appendParameter(StringBuilder sb, Map<String, Object> parameter, List<DynamicFilterStockPriceCondition> conditions, Date lastestTradingDate) {
		boolean isContainDateRange = (conditions.stream().anyMatch(condition -> "startDate".equals(condition.getName())) && conditions.stream().anyMatch(condition -> "startDate".equals(condition.getName()));
		/**
		 * 價格 現在價格在哪裡到哪裡
		 * 漲停 今天有沒有漲停

漲幅 連續幾天的漲幅要超過多少

KD :連續幾天都在某個區間持續向上或持續向下，或開始持平
MA 5 10 20 60 120 240 整體現在向上還是向下 還是持平 這個會綜合起來看
所以要抓動態查詢

		 */
		conditions.stream().forEach(condition ->{
			
			if(isContainDateRange) {
				""
			}
		});
		if(!isContaineDateRange) {
			sb.append("   sdp.trading_day = :lastestTradingDate ");
			parameter.put("lastestTradingDate", lastestTradingDate);
		}
	}
}
