package com.bigstock.sharedComponent.service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import com.bigstock.sharedComponent.redis.CacheOperatorService;
import com.google.common.collect.Lists;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockTagCacheService {

    private final CacheOperatorService cache;

    // ---------- stock → tags ----------
    public void cacheStockTags(String stockCode, Collection<String> tags) {
    	cache.putSnapshotDataListAtomic("ultraLongLivedCache", "stockTags:"+stockCode, Lists.newArrayList(tags) );
        long deleted = cache.cleanupOldSnapshots("ultraLongLivedCache",  "stockTags:"+stockCode, 2);
        log.info("Old snapshot cleanup done. namespace={}, deleted={}", ("cache:ultraLongLivedCache:stockTags:"+stockCode), deleted);
    }

    public List<String> getStockTags(String stockCode) {
    	List<String> stockTags = cache.getSnapshotDataList("ultraLongLivedCache",
				"stockInfo:"+stockCode, String.class);
        return stockTags;
    }

    // ---------- tag → stocks ----------
	public void cacheTagStocks(String tag, String tagMeta, Collection<String> stockCodes) {
		JSONObject tagInfo = new JSONObject();
		tagInfo.put("title", tagMeta);
		tagInfo.put("stockCodes", stockCodes);
		cache.putSnapshotDataListAtomic("ultraLongLivedCache", "tagInfo:" + tag, Lists.newArrayList(tagInfo));
		long deleted = cache.cleanupOldSnapshots("ultraLongLivedCache", "tagInfo:" + tag, 2);
		log.info("Old snapshot cleanup done. namespace={}, deleted={}",
				("cache:ultraLongLivedCache:tagInfo:" + tagInfo), deleted);
	}

    public List<String> getTagStocks(String tag) {
    	List<JSONObject> tagInfos = cache.getSnapshotDataList("ultraLongLivedCache",
				"tagInfo:"+tag, JSONObject.class);
    	Optional<JSONObject> tagInfoOp =  tagInfos.stream().findFirst();
    	if(tagInfoOp.isEmpty()) {
    		return Lists.newArrayList();
    	}
    	JSONArray jsonArray = tagInfoOp.get().getJSONArray("stockCodes");

    	List<String> stockCodes = IntStream.range(0, jsonArray.length())
    	        .mapToObj(jsonArray::getString)
    	        .collect(Collectors.toList());
        return stockCodes;
    }

}