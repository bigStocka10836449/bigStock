package com.bigstock.biz.service;

import java.util.List;

import org.redisson.api.RBatch;
import org.redisson.api.RMap;
import org.redisson.api.RMapAsync;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RScoredSortedSetAsync;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import com.bigstock.sharedComponent.entity.StockDayPrice;

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
	public void writeStockListToRedis(List<StockDayPrice> list, String market) {

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

	        map.putAsync("open", stock.getOpeningPrice());
	        map.putAsync("close", stock.getClosingPrice());
	        map.putAsync("high", stock.getHighPrice());
	        map.putAsync("low", stock.getLowPrice());
	        map.putAsync("changeRate", stock.getChangeRate());

	        ranking.addAsync(stock.getChangeRate(), stockCode);
	    }

	    batch.execute();
	}
	
}
