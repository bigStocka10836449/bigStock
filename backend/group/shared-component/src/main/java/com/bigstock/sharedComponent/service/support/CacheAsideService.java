package com.bigstock.sharedComponent.service.support;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public interface CacheAsideService {

    CacheAsideExecutor getCacheAsideExecutor();

    default <T> List<T> getOrLoadWithLock(
            String cacheName,
            String cacheKey,
            Supplier<List<T>> redisReader,
            Supplier<List<T>> dbLoader,
            Consumer<List<T>> redisWriter) {

        return getCacheAsideExecutor().executeList(
                cacheName,
                cacheKey,
                redisReader,
                dbLoader,
                redisWriter
        );
    }
}