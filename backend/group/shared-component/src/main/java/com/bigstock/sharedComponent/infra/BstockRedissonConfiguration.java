package com.bigstock.sharedComponent.infra;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BstockRedissonConfiguration {

    @Value("${app.env:prod}")
    private String appEnv;

    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private String port;

    @Value("${spring.data.redis.password}")
    private String password;

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {

        Config config = new Config();

        SingleServerConfig serverConfig = config.useSingleServer()
                .setAddress("redis://" + host + ":" + port)
                .setPassword(password);

        // 僅 local 環境限制 Redis 連線數
        if ("local".equalsIgnoreCase(appEnv)) {

            serverConfig
                    .setConnectionMinimumIdleSize(1)
                    .setConnectionPoolSize(5)
                    .setSubscriptionConnectionMinimumIdleSize(1)
                    .setSubscriptionConnectionPoolSize(2)
                    .setConnectTimeout(15000)
                    .setTimeout(15000)
                    .setRetryAttempts(3);
        }

        return Redisson.create(config);
    }
}