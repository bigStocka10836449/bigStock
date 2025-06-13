package com.bigstock.sharedComponent.redis;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.redisson.api.RList;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import com.bigstock.sharedComponent.entity.ShareholderStructure;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RedissonCacheService {

	private final RedissonClient redissonClient;
	public static final String SHAREHOLDER_STRUCTURE_RMAP_CACHE = "shareholderStructureRmapCache";
	public static final String SHAREHOLDER_STRUCTURE_RLIST = "shareholderStructureRList";
	public static final String LAST_TWO_WEEKS_INCREASE_RMAP_CACHE = "lastTwoWeeksIncreaseRmapCache";
	public static final String LAST_TWO_WEEKS_INCREASE_RLIST = "lastTwoWeeksIncreaseRList";

}
