package com.bigstock.sharedComponent.redis;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.bigstock.sharedComponent.utils.ByteArrayRedisSerializer;

@Configuration
public class BstockCacheConfig {

    @Value("${spring.redis.host}")
    private String host;

    @Value("${spring.redis.port}")
    private int port;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {

        LettuceConnectionFactory factory =
                new LettuceConnectionFactory(host, port);

        factory.setValidateConnection(true);
        factory.setShareNativeConnection(false);

        return factory;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory factory) {

        RedisTemplate<String, Object> template =
                new RedisTemplate<>();

        template.setConnectionFactory(factory);

        StringRedisSerializer keySer =
                new StringRedisSerializer();

        GenericJackson2JsonRedisSerializer valSer =
                new GenericJackson2JsonRedisSerializer();

        template.setKeySerializer(keySer);
        template.setValueSerializer(valSer);
        template.setHashKeySerializer(keySer);
        template.setHashValueSerializer(valSer);
        

        template.afterPropertiesSet();

        return template;
    }
    
    @Bean("rawRedisTemplate")
    public RedisTemplate<String, byte[]> rawRedisTemplate(
            RedisConnectionFactory factory) {

        RedisTemplate<String, byte[]> template =
                new RedisTemplate<>();

        template.setConnectionFactory(factory);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new ByteArrayRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new ByteArrayRedisSerializer());

        template.afterPropertiesSet();

        return template;
    }
}