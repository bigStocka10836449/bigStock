package com.bigstock.sharedComponent.service.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class CacheAsideExecutor {

    private final RedissonClient redissonClient;

    private static final long LOCK_WAIT_SECONDS = 10;

    /**
     * Cache-Aside without sorting.
     *
     * Redis -> Lock -> Double Check -> DB -> Redis
     */
    public <T> List<T> executeList(
            String cacheName,
            String cacheKey,
            Supplier<List<T>> redisReader,
            Supplier<List<T>> dbLoader,
            Consumer<List<T>> redisWriter) {

        return executeList(
                cacheName,
                cacheKey,
                redisReader,
                dbLoader,
                redisWriter,
                null
        );
    }

    /**
     * Cache-Aside with optional result sorting.
     *
     * @param comparator
     *        null: keep original order
     *        non-null: sort returned data
     */
    public <T> List<T> executeList(
            String cacheName,
            String cacheKey,
            Supplier<List<T>> redisReader,
            Supplier<List<T>> dbLoader,
            Consumer<List<T>> redisWriter,
            Comparator<? super T> comparator) {

        // =====================================================
        // 1. Redis First
        // =====================================================

        List<T> cached = redisReader.get();

        if (cached != null && !cached.isEmpty()) {
            return sortIfNeeded(cached, comparator);
        }

        // =====================================================
        // 2. Distributed Lock
        // =====================================================

        String lockKey =
                "lock:cacheAside:" + cacheName + ":" + cacheKey;

        RLock lock = redissonClient.getLock(lockKey);

        boolean lockAcquired = false;

        try {

            lockAcquired = lock.tryLock(
                    LOCK_WAIT_SECONDS,
                    TimeUnit.SECONDS
            );

            if (!lockAcquired) {

                // Lock 等待失敗，再查一次 Redis
                cached = redisReader.get();

                if (cached != null && !cached.isEmpty()) {
                    return sortIfNeeded(cached, comparator);
                }

                throw new IllegalStateException(
                        "Could not acquire cache lock: " + lockKey
                );
            }

            // =====================================================
            // 3. Double Check Redis
            // =====================================================

            cached = redisReader.get();

            if (cached != null && !cached.isEmpty()) {
                return sortIfNeeded(cached, comparator);
            }

            // =====================================================
            // 4. DB Fallback
            // =====================================================

            List<T> data = dbLoader.get();

            if (data == null || data.isEmpty()) {
                return Collections.emptyList();
            }

            // =====================================================
            // 5. Redis Write Back
            // =====================================================

            redisWriter.accept(data);

            log.debug(
                    "Cache populated, cacheName={}, cacheKey={}, size={}",
                    cacheName,
                    cacheKey,
                    data.size()
            );

            // =====================================================
            // 6. Return Sorted Result
            // =====================================================

            return sortIfNeeded(data, comparator);

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Interrupted while acquiring cache lock: " + lockKey,
                    e
            );

        } finally {

            // =====================================================
            // 7. Unlock
            // =====================================================

            if (lockAcquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * Sort result without modifying original list.
     */
    private <T> List<T> sortIfNeeded(
            List<T> data,
            Comparator<? super T> comparator) {

        if (data == null || comparator == null || data.size() <= 1) {
            return data;
        }

        List<T> result = new ArrayList<>(data);

        result.sort(comparator);

        return result;
    }
}