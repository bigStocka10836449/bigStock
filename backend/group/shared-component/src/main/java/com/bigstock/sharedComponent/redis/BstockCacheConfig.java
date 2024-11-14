package com.bigstock.sharedComponent.redis;

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableCaching
public class BstockCacheConfig {

	@Value("${spring.redis.host}")
	private String host;

	@Value("${spring.redis.port}")
	private String port;
	
    @Bean
    public RedisCacheConfiguration cacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));
    }

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory(host,Integer.valueOf(port));
        return connectionFactory;
    }
    
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory redisConnectionFactory,
                                          RedisTemplate<String, Object> redisTemplate) {
        RedisCacheWriter cacheWriter = RedisCacheWriter.nonLockingRedisCacheWriter(redisConnectionFactory);
        RedisCacheConfiguration defaultCacheConfig = cacheConfiguration().entryTtl(Duration.ofMinutes(30));

        Map<String, Duration> ttlMap = new HashMap<>();
        ttlMap.put("shortLivedCache", Duration.ofMinutes(120));
        ttlMap.put("longLivedCache", Duration.ofDays(7));
        ttlMap.put("middleLivedCache", Duration.ofDays(1));
        ttlMap.put("defaultCache", Duration.ofMinutes(30));

        return new BstockRedisCacheManager(cacheWriter, defaultCacheConfig, redisTemplate, ttlMap);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return redisTemplate;
    }

    private static final class BstockRedisCacheManager extends RedisCacheManager {

        private final RedisTemplate<String, Object> redisTemplate;
        private final Map<String, Duration> ttlMap;
        private final Map<String, Cache> cacheMap = new ConcurrentHashMap<>();

        public BstockRedisCacheManager(RedisCacheWriter cacheWriter, RedisCacheConfiguration defaultCacheConfiguration,
                                       RedisTemplate<String, Object> redisTemplate, Map<String, Duration> ttlMap) {
            super(cacheWriter, defaultCacheConfiguration);
            this.redisTemplate = redisTemplate;
            this.ttlMap = ttlMap;
        }

        @Override
        public Cache getCache(String name) {
            return cacheMap.computeIfAbsent(name, cacheName -> {
                Duration ttl = ttlMap.getOrDefault(cacheName,
                        getDefaultCacheConfiguration().getTtlFunction().getTimeToLive(Object.class, null));
                RedisCache redisCache = (RedisCache) super.getCache(cacheName);
                return new BstockRedisCache(redisCache, redisTemplate, ttl);
            });
        }

        @Override
        public Collection<String> getCacheNames() {
            return cacheMap.keySet();
        }
    }

    private static final class BstockRedisCache implements Cache {

        private final RedisCache redisCache;
        private final RedisTemplate<String, Object> redisTemplate;
        private final Duration ttl;

        public BstockRedisCache(RedisCache redisCache, RedisTemplate<String, Object> redisTemplate, Duration ttl) {
            this.redisCache = redisCache;
            this.redisTemplate = redisTemplate;
            this.ttl = ttl;
        }

        @Override
        public String getName() {
            return redisCache.getName();
        }

        @Override
        public Object getNativeCache() {
            return redisCache.getNativeCache();
        }

        @Override
        public ValueWrapper get(Object key) {
            ValueWrapper valueWrapper = redisCache.get(key);
            if (valueWrapper != null) {
                extendTtl(key);
            }
            return valueWrapper;
        }

        @Override
        public <T> T get(Object key, Callable<T> valueLoader) {
            T value = redisCache.get(key, valueLoader);
            if (value != null) {
                extendTtl(key);
            }
            return value;
        }

        @Override
        public void put(Object key, Object value) {
            redisCache.put(key, value);
            extendTtl(key);
        }

        @Override
        public ValueWrapper putIfAbsent(Object key, Object value) {
            ValueWrapper valueWrapper = redisCache.putIfAbsent(key, value);
            if (valueWrapper != null) {
                extendTtl(key);
            }
            return valueWrapper;
        }

        @Override
        public void evict(Object key) {
            redisCache.evict(key);
        }

        @Override
        public void clear() {
            redisCache.clear();
        }

        private void extendTtl(Object key) {
            String redisKey = redisCache.getName() + "::" + key.toString();
            redisTemplate.expire(redisKey, ttl);
        }

        @Override
        public <T> T get(Object key, Class<T> type) {
            T value = redisCache.get(key, type);
            if (value != null) {
                extendTtl(key);
            }
            return value;
        }
    }
}