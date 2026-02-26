package com.bigstock.biz.infra;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import com.bigstock.biz.component.LocalSessionRegistry;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class RedisKickConfig {

    private final LocalSessionRegistry registry;

    /**
     * 監聽從Redis發來的推播信息，目前只監聽踢人的行為
     * @param factory
     * @return
     */
    @Bean
    public RedisMessageListenerContainer container(RedisConnectionFactory factory) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);

        container.addMessageListener(
                (message, pattern) -> {
                    String sessionId = new String(message.getBody());
                    registry.close(sessionId);
                },
                new PatternTopic("WS_KICK")
        );

        return container;
    }
}
