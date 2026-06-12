package com.bigstock.sharedComponent.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.bigstock.sharedComponent.redis.CacheOperatorService;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockTagCacheService {

    private final CacheOperatorService cache;
    
    private final StockInfoService stockInfoService;
    
    private static Map<String,String> tagsMapping = Maps.newHashMap();
    
    private static Map<String,String> stockCodeMapping = Maps.newHashMap();
    
    
    
//    LOCAL_RANK_TEST_DISABLED: @PostConstruct
    public void init() {
    	List<Map> tagInfos = cache.getDataList("ultraLongLivedCache",
				"tagsMapping:*", Map.class);
       	 tagInfos.stream()
    		    .filter(Objects::nonNull)
    		    .forEach(map -> {
    		    	tagsMapping.put(map.get("tag").toString(), map.get("title").toString());
    		    });
       	stockInfoService.getAllStockInfo().forEach(stockInfo ->{
       		stockCodeMapping.put(stockInfo.getStockCode(), stockInfo.getStockName().trim());
       	});
    }

    // ---------- stock → tags ----------
    public void cacheStockTags(String stockCode, Collection<String> tags) {
    	cache.putSnapshotDataListAtomic("ultraLongLivedCache", "stockTags:"+stockCode, Lists.newArrayList(tags) );
        long deleted = cache.cleanupOldSnapshots("ultraLongLivedCache",  "stockTags:"+stockCode, 2);
        log.info("Old snapshot cleanup done. namespace={}, deleted={}", ("cache:ultraLongLivedCache:stockTags:"+stockCode), deleted);
    }

	public List<Map<String, String>> getStockTags(String stockCode) {

		List<String> stockTags = cache.getSnapshotDataList("ultraLongLivedCache", "stockTags:" + stockCode,
				String.class);
		return stockTags.stream().map(stockTag -> {
			Map<String, String> singleTagMapping = Maps.newHashMap();
			singleTagMapping.put(stockTag, tagsMapping.get(stockTag));
			return singleTagMapping;
		}).toList();
	}
    
    public Map<String,String> getTagsMapping() {
    	if(tagsMapping != null) {
    		return tagsMapping;
    	}
    	List<Map> tagInfos = cache.getDataList("ultraLongLivedCache",
				"tagsMapping:*", Map.class);
       	List<Map<String, String>> tagInfosTyped = tagInfos.stream()
    		    .filter(Objects::nonNull)
    		    .map(map -> {
    		        Map<String, String> newMap = new HashMap<>();
    		        for (Object key : map.keySet()) {
    		            Object value = map.get(key);
    		            if (key != null && value != null) {
    		                newMap.put(String.valueOf(key), String.valueOf(value));
    		            }
    		        }
    		        return newMap;
    		    })
    		    .collect(Collectors.toList());
    	Map<String, String> result = tagInfosTyped.stream()
    		    .filter(Objects::nonNull)
    		    .flatMap(map -> map.entrySet().stream())
    		    .filter(e -> e.getKey() != null && e.getValue() != null)
    		    .collect(Collectors.toMap(
    		        Map.Entry::getKey,
    		        Map.Entry::getValue,
    		        (existing, replacement) -> existing  
    		    ));
    
        return result;
    }

    // ---------- tag → stocks ----------
	public void cacheTagStocks(String tag, String tagMeta, Collection<String> stockCodes) {
		Map<String, Object> tagInfo = new HashMap<>();
		tagInfo.put("title", tagMeta);
		tagInfo.put("stockCodes", stockCodes);
		cache.putSnapshotDataListAtomic("ultraLongLivedCache", "tagInfo:" + tag, Lists.newArrayList(tagInfo));
		Map<String, Object> tagMapping = new HashMap<>();
		tagMapping.put("tag", tag);
		tagMapping.put("title", tagMeta);
		cache.putDataList("ultraLongLivedCache", "tagsMapping:" + tag, Lists.newArrayList(tagMapping));
		long deleted = cache.cleanupOldSnapshots("ultraLongLivedCache", "tagInfo:" + tag, 2);
		log.info("Old snapshot cleanup done. namespace={}, deleted={}",
				("cache:ultraLongLivedCache:tagInfo:" + tagInfo), deleted);
	}

    public Map<String, String> getTagStocks(String tag) {
    	List<Map> tagInfos = cache.getSnapshotDataList("ultraLongLivedCache",
				"tagInfo:"+tag, Map.class);
    	Optional<Map> tagInfoOp =  tagInfos.stream().findFirst();
    	if(tagInfoOp.isEmpty()) {
    		return Maps.newHashMap();
    	}
    	Object stockCodesOb = tagInfoOp.get().get("stockCodes");
    	List<String> stockCodes = Lists.newArrayList(stockCodesOb.toString().replace("[", "").replace("]", "").split(","));
    	Map<String, String> partStockCodeMapping = Maps.newHashMap();
    	stockCodes.forEach(stockCode ->{
    		partStockCodeMapping.put(stockCode.trim(), stockCodeMapping.get(stockCode.trim()));
    	});
        return partStockCodeMapping;
    }

}