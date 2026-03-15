package com.bigstock.sharedComponent.aspect;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import com.bigstock.sharedComponent.annotation.BigStockCacheableWithLock;
import com.bigstock.sharedComponent.redis.CacheOperatorService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Aspect
@RequiredArgsConstructor
@Slf4j
public class RedissonLockAspect {

	private final RedissonClient redissonClient;
	private final CacheOperatorService cacheOperatorService;

	@Around("@annotation(com.bigstock.sharedComponent.annotation.BigStockCacheableWithLock)")
	public Object aroundAdvice(ProceedingJoinPoint joinPoint) throws Throwable {

		MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();

		Object[] args = joinPoint.getArgs();

		String cacheKey = Arrays.stream(args).map(String::valueOf).collect(Collectors.joining("-"));

		BigStockCacheableWithLock annotation = methodSignature.getMethod()
				.getAnnotation(BigStockCacheableWithLock.class);

		String cacheName = annotation.value();

		log.info("method {} not hit cache, execute database access", methodSignature.getMethod().getName());

		String lockKey = "lock:" + methodSignature.getMethod().getName() + ":" + cacheKey;

		RLock lock = redissonClient.getLock(lockKey);

		boolean lockAcquired = false;

		try {

			lockAcquired = lock.tryLock(10, TimeUnit.MINUTES);

			if (lockAcquired) {

				// ⭐ DOUBLE CHECK CACHE
				Object cached = cacheOperatorService.getSimple(cacheName, cacheKey, Object.class);

				if (cached != null) {
					return cached;
				}

				Object result = joinPoint.proceed();

				cacheOperatorService.putSimple(cacheName, cacheKey, result);

				return result;

			} else {
				throw new RuntimeException("Could not acquire lock for " + lockKey);
			}

		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("Interrupted while trying to acquire lock", e);
		} finally {

			if (lockAcquired && lock.isHeldByCurrentThread()) {
				lock.unlock();
			}

		}
	}
}
