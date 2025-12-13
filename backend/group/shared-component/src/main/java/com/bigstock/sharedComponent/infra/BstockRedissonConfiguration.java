package com.bigstock.sharedComponent.infra;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
//
//@Configuration
//public class BstockRedissonConfiguration {
//
//	@Value("${spring.redis.host}")
//	private String host;
//
//	@Value("${spring.redis.port}")
//	private String port;
//	@Bean
//	public RedissonClient redissonClient() {
//		// Redis 连接配置，可以根据实际情况进行修改
//		Config config = new Config();
//		config.useSingleServer().setAddress("redis://" + host + ":" + port);
//		// 初始化 RedissonClient
//		return Redisson.create(config);
//	}
//}
