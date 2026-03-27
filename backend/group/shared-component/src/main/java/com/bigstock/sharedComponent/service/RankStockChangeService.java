package com.bigstock.sharedComponent.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RBatch;
import org.redisson.api.RFuture;
import org.redisson.api.RMapAsync;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RScoredSortedSetAsync;
import org.redisson.api.RedissonClient;
import org.redisson.client.protocol.ScoredEntry;
import org.springframework.stereotype.Service;

import com.bigstock.sharedComponent.dto.RankingResponse;
import com.bigstock.sharedComponent.entity.StockDayPrice;
import com.bigstock.sharedComponent.entity.StockDayPriceRank;
import com.bigstock.sharedComponent.entity.StockInfo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RankStockChangeService {

	private static final String STOCK_KEY_PREFIX = "stock:";
	private static final String RANK_KEY_PREFIX = "rank:";
	
	private final RedissonClient redissonClient;
	

	/**
	 * Write stock list to Redis using Pipeline
	 */
	public void writeStockListToRedis(List<StockDayPrice> list, Map<String, List<StockInfo>> allStockInfoMaps , String market) {

	    RBatch batch = redissonClient.createBatch();

	    RScoredSortedSetAsync<String> ranking =
	            batch.getScoredSortedSet(
	                    RANK_KEY_PREFIX + market + ":changeRate");

	    ranking.deleteAsync(); // clear previous ranking

	    for (StockDayPrice stock : list) {

	        String stockCode = stock.getStockCode();
	        String stockKey = "stock:" + market + ":" + stockCode;

	        RMapAsync<String, Object> map =
	                batch.getMap(stockKey);

	        map.putAsync("stockName", allStockInfoMaps.containsKey(stockCode) ? allStockInfoMaps.get(stockCode).get(0).getStockName() : stockCode);
	        map.putAsync("open", stock.getOpeningPrice());
	        map.putAsync("close", stock.getClosingPrice());
	        map.putAsync("high", stock.getHighPrice());
	        map.putAsync("low", stock.getLowPrice());
	        map.putAsync("changeRate", stock.getChangeRate());
	        map.putAsync("change", stock.getChange());
	        map.putAsync("tradingVolume", stock.getTradingVolume());
	        map.putAsync("change", stock.getChange());
	        map.putAsync("limitUp", stock.getLimitUp());
	        map.putAsync("limitDown", stock.getLimitDown());
	        ranking.addAsync(stock.getChangeRate(), stockCode);
	    }

	    batch.execute();
	}
	
	
	
	public void writeStockListRankToRedis(List<StockDayPriceRank> list, Map<String, List<StockInfo>> allStockInfoMaps , String market) {

	    RBatch batch = redissonClient.createBatch();

	    RScoredSortedSetAsync<String> ranking =
	            batch.getScoredSortedSet(
	                    RANK_KEY_PREFIX + market + ":changeRate");

	    ranking.deleteAsync(); // clear previous ranking

	    for (StockDayPriceRank stock : list) {
	    	if (stock.getClosingPrice().equals("---") || stock.getClosingPrice().equals("----")
					|| stock.getClosingPrice().equals("--") || stock.getClosingPrice().equals("-") || stock.getLowPrice().equals("--")
					|| stock.getLowPrice().equals("---") || stock.getLowPrice().equals("----")
					|| StringUtils.isBlank(stock.getClosingPrice())) {
				continue;
			}
	        String stockCode = stock.getStockCode();
	        String stockKey = "stock:" + market + ":" + stockCode;

	        RMapAsync<String, Object> map =
	                batch.getMap(stockKey);

	        map.putAsync("stockName", allStockInfoMaps.containsKey(stockCode) ? allStockInfoMaps.get(stockCode).get(0).getStockName() : stockCode);
	        map.putAsync("open", stock.getOpeningPrice());
	        map.putAsync("close", stock.getClosingPrice());
	        map.putAsync("high", stock.getHighPrice());
	        map.putAsync("low", stock.getLowPrice());
	        map.putAsync("changeRate", stock.getChangeRate());
	        map.putAsync("change", stock.getChange());
	        map.putAsync("tradingVolume", stock.getTradingVolume());
	        map.putAsync("change", stock.getChange());
	        map.putAsync("limitUp", stock.getLimitUp());
	        map.putAsync("limitDown", stock.getLimitDown());
	        Double tradingVolumeDouble = StringUtils.isNotBlank(stock.getTradingVolume()) ? Double.valueOf(stock.getTradingVolume()) : 0d;
	        Double closeDouble = StringUtils.isNotBlank(stock.getClosingPrice()) ? Double.valueOf(stock.getClosingPrice()) : 0d;
	        Double tradingQuantity = closeDouble * tradingVolumeDouble;
	        map.putAsync("tradingQuantity", tradingQuantity.toString());
	        ranking.addAsync(stock.getChangeRate(), stockCode);
	    }

	    batch.execute();
	}
	
	public void writeStockListTradingQuantityRankToRedis(List<StockDayPrice> list, Map<String, List<StockInfo>> allStockInfoMaps) {

	    RBatch batch = redissonClient.createBatch();

	    RScoredSortedSetAsync<String> ranking =
	            batch.getScoredSortedSet(
	                    RANK_KEY_PREFIX  + "tradingQuantity:changeRate");

	    ranking.deleteAsync(); // clear previous ranking

	    for (StockDayPrice stock : list) {
	    	if (stock.getClosingPrice().equals("---") || stock.getClosingPrice().equals("----")
					|| stock.getClosingPrice().equals("--") || stock.getClosingPrice().equals("-") || stock.getLowPrice().equals("--")
					|| stock.getLowPrice().equals("---") || stock.getLowPrice().equals("----")
					|| StringUtils.isBlank(stock.getClosingPrice())) {
				continue;
			}
	        String stockCode = stock.getStockCode();
	        String stockKey = "stock:tradingQuantity:" + stockCode;

	        RMapAsync<String, Object> map =
	                batch.getMap(stockKey);

	        map.putAsync("stockName", allStockInfoMaps.containsKey(stockCode) ? allStockInfoMaps.get(stockCode).get(0).getStockName() : stockCode);
	        map.putAsync("open", stock.getOpeningPrice());
	        map.putAsync("close", stock.getClosingPrice());
	        map.putAsync("high", stock.getHighPrice());
	        map.putAsync("low", stock.getLowPrice());
	        map.putAsync("changeRate", stock.getChangeRate());
	        map.putAsync("change", stock.getChange());
	        map.putAsync("tradingVolume", stock.getTradingVolume());
	        map.putAsync("change", stock.getChange());
	        map.putAsync("limitUp", stock.getLimitUp());
	        map.putAsync("limitDown", stock.getLimitDown());
	        Double tradingVolumeDouble = StringUtils.isNotBlank(stock.getTradingVolume()) ? Double.valueOf(stock.getTradingVolume()) : 0d;
	        Double closeDouble = StringUtils.isNotBlank(stock.getClosingPrice()) ? Double.valueOf(stock.getClosingPrice()) : 0d;
	        Double tradingQuantity = closeDouble * tradingVolumeDouble;
	        map.putAsync("tradingQuantity", tradingQuantity.toString());
	        ranking.addAsync(tradingQuantity, stockCode);
	    }

	    batch.execute();
	}
	
	public List<RankingResponse> getRanking(String market, int limit, String order) throws InterruptedException, ExecutionException {

	    String rankingKey = "rank:" + market + ":changeRate";

	    RScoredSortedSet<String> ranking =
	            redissonClient.getScoredSortedSet(rankingKey);

	    Collection<ScoredEntry<String>> entries;

	    boolean asc = "asc".equalsIgnoreCase(order);

	    if (asc) {
	        entries = ranking.entryRange(0, limit - 1);
	    } else {
	        entries = ranking.entryRangeReversed(0, limit - 1);
	    }

	    if (entries == null || entries.isEmpty()) {
	        return Collections.emptyList();
	    }

	    List<ScoredEntry<String>> entryList = new ArrayList<>(entries);

	    RBatch batch = redissonClient.createBatch();
	    List<RFuture<Map<Object, Object>>> futureList = new ArrayList<>();

	    for (ScoredEntry<String> entry : entryList) {

	        String stockCode = entry.getValue();
	        String stockKey = "stock:" + market + ":" + stockCode;

	        RMapAsync<Object, Object> map = batch.getMap(stockKey);
	        futureList.add(map.readAllMapAsync());
	    }

	    batch.execute();

	    List<RankingResponse> result = new ArrayList<>();

	    for (int i = 0; i < entryList.size(); i++) {

	        ScoredEntry<String> entry = entryList.get(i);

	        Map<Object, Object> stockMap = futureList.get(i).get();

	        RankingResponse response = new RankingResponse();
	        response.setRank(i + 1);
	        response.setStockCode(entry.getValue());

	        // Use score directly from ZSET
	        response.setChangeRate(BigDecimal.valueOf(entry.getScore()));

	        if (stockMap != null) {
	            response.setStockName((String) stockMap.get("stockName"));
	            response.setOpeningPrice(castToBigDecimal(stockMap.get("open")));
	            response.setClosingPrice(castToBigDecimal(stockMap.get("close")));
	            response.setChange(castToBigDecimal(stockMap.get("change")));
	            response.setTradingVolume(castToLong(stockMap.get("tradingVolume")));
	            response.setOpeningPrice(castToBigDecimal(stockMap.get("open")));
	            response.setLimitUp(castToBigDecimal(stockMap.get("limitUp")));
	            response.setLimitDown(castToBigDecimal(stockMap.get("limitDown")));
	            if(stockMap.containsKey("tradingQuantity")) {
	            	response.setTradingQuantity(castToBigDecimal(stockMap.get("tradingQuantity")));
	            } 
	        }

	        result.add(response);
	    }

	    return result;
	}
	
	private BigDecimal castToBigDecimal(Object value) {
	    if (value == null) {
	        return null;
	    }
	    if (value instanceof BigDecimal) {
	        return (BigDecimal) value;
	    }
	    return new BigDecimal(String.valueOf(value));
	}

	private Long castToLong(Object value) {
	    if (value == null) {
	        return null;
	    }
	    if (value instanceof Long) {
	        return (Long) value;
	    }
	    return Long.valueOf(String.valueOf(value));
	}
}
