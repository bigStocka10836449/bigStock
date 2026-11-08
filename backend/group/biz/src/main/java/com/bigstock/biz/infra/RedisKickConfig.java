package com.bigstock.biz.infra;

import java.nio.charset.StandardCharsets;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import com.bigstock.biz.component.LocalSessionRegistry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class RedisKickConfig {

	private final LocalSessionRegistry registry;

	@Bean
	public RedisMessageListenerContainer container(RedisConnectionFactory factory) {

		RedisMessageListenerContainer container = new RedisMessageListenerContainer();

		container.setConnectionFactory(factory);

		container.addMessageListener((message, pattern) -> {

			String sessionId = new String(message.getBody(), StandardCharsets.UTF_8);

			if (sessionId.isBlank() || sessionId.length() > 256) {
				return;
			}

			try {

				log.debug("[WS] Received kick event, sessionId={}", sessionId);

				registry.close(sessionId);

			} catch (Exception e) {

				log.error("[WS] Failed to kick WebSocket session", e);
			}

		}, new ChannelTopic("WS_KICK"));

		return container;
	}
}
