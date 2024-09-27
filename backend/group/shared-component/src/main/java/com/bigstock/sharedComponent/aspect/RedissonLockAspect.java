package com.bigstock.sharedComponent.aspect;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import com.bigstock.sharedComponent.annotation.BigStockCacheableWithLock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Aspect
@RequiredArgsConstructor
@Slf4j
public class RedissonLockAspect {

	private final RedissonClient redissonClient;
	private final CacheManager cacheManager;

	@Around("@annotation(com.bigstock.sharedComponent.annotation.BigStockCacheableWithLock)")
	public Object aroundAdvice(ProceedingJoinPoint joinPoint) throws Throwable {

		// Get the method signature and the method
		MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();

		// Create the expression parser

		// Add method arguments to the context
		Object[] args = joinPoint.getArgs();
		String[] argStrings = Arrays.asList(args).toArray(new String[args.length]);
		
		log.info("method {} not hit cache, excecutr database access", methodSignature.getMethod().getName());
		// Evaluate the key expression
		String cacheKey = String.join("-", argStrings);
		BigStockCacheableWithLock bigStockCacheableWithLock = methodSignature.getMethod()
				.getAnnotation(BigStockCacheableWithLock.class);
		String lockKey = "lock:" + methodSignature.getMethod().getName() + ":" + cacheKey;
		RLock lock = redissonClient.getLock(lockKey);
		boolean lockAcquired = false;

		try {
			lockAcquired = lock.tryLock(10,TimeUnit.MINUTES);
			if (lockAcquired) {
				Cache cache = cacheManager.getCache(bigStockCacheableWithLock.value());
				// Double-check if another thread has already populated the cache
				Cache.ValueWrapper cachedValue = cache.get(cacheKey);
				if (cachedValue != null) {
					return cachedValue.get();
				}
				// Proceed with the method invocation to get the value
				Object result = joinPoint.proceed();
				return result;
			} else {
				throw new RuntimeException("Could not acquire lock for " + lockKey);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("Interrupted while trying to acquire lock", e);
		} finally {
			if (lockAcquired) {
				lock.unlock();
			}
		}
	}
}
