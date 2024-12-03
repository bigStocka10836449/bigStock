package com.bigstock.biz.domain;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.bigstock.sharedComponent.dto.DynamicFilterStockPriceCondition;

import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StockDayPriceNativeQueryService {
	
	private final EntityManagerFactory entityManagerFactory;
	
	private String BASE_QUERY_WITH_RANGE = "SELECT sdp."
	
	private static final String BASE_QUERY = "SELECT sdp.* FROM bstock.bstock.stock_day_price sdp where 1=1 and %dynamicCondition ";
	
	private static final String alias = "sdp.";
	
	public List<String> findMatchStockCodeByCondition(List<DynamicFilterStockPriceCondition> conditions){
		StringBuilder sb = new StringBuilder(BASE_QUERY);
		
		
		return null;
		
	}
	
	private void appendParameter(StringBuilder sb, Map<String, Object> parameter, List<DynamicFilterStockPriceCondition> conditions, Date lastestTradingDate) {
		boolean isContainDateRange = (conditions.stream().anyMatch(condition -> "startDate".equals(condition.getName())) && conditions.stream().anyMatch(condition -> "startDate".equals(condition.getName()));
		/**
		 * 價格 現在價格在哪裡到哪裡
		 * 漲停 連續幾天的漲停

漲幅 連續幾天的漲幅要超過多少

KD :連續幾天都在某個區間

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
