package com.bigstock.sharedComponent.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import com.bigstock.sharedComponent.redis.CacheOperatorService;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

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

    public List<Map> getStockTags(String stockCode) {
    	List<String> stockTags = cache.getSnapshotDataList("ultraLongLivedCache",
				"stockTags:"+stockCode, String.class);
    	List<Map> tagMapping = Lists.newArrayList();
    	stockTags.stream().forEach(stockTag ->{
    		List<Map> tagInfos = cache.getSnapshotDataList("ultraLongLivedCache",
    				"tagInfo:"+stockTag, Map.class);
        	Optional<Map> tagInfoOp =  tagInfos.stream().findFirst();
        	Map<String, String> mapping = Maps.newHashMap();
        	mapping.put("code", stockTag);
        	mapping.put("title", tagInfoOp.isPresent() ? tagInfoOp.get().get("title").toString() : "");
        	tagMapping.add(mapping);
    	});
        return tagMapping;
    }

    // ---------- tag → stocks ----------
	public void cacheTagStocks(String tag, String tagMeta, Collection<String> stockCodes) {
		Map<String, Object> tagInfo = new HashMap<>();
		tagInfo.put("title", tagMeta);
		tagInfo.put("stockCodes", stockCodes);
		cache.putSnapshotDataListAtomic("ultraLongLivedCache", "tagInfo:" + tag, Lists.newArrayList(tagInfo));
		long deleted = cache.cleanupOldSnapshots("ultraLongLivedCache", "tagInfo:" + tag, 2);
		log.info("Old snapshot cleanup done. namespace={}, deleted={}",
				("cache:ultraLongLivedCache:tagInfo:" + tagInfo), deleted);
	}

    public List<String> getTagStocks(String tag) {
    	List<Map> tagInfos = cache.getSnapshotDataList("ultraLongLivedCache",
				"tagInfo:"+tag, Map.class);
    	Optional<Map> tagInfoOp =  tagInfos.stream().findFirst();
    	if(tagInfoOp.isEmpty()) {
    		return Lists.newArrayList();
    	}
    	Object stockCodesOb = tagInfoOp.get().get("stockCodes");
    	List<String> jsonArray = Lists.newArrayList(stockCodesOb.toString().replace("[", "").replace("]", "").split(","));

        return jsonArray;
    }

}