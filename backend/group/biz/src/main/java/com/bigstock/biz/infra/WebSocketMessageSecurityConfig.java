package com.bigstock.biz.infra;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.config.ChannelRegistration;

import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.messaging.access.intercept.AuthorizationChannelInterceptor;
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;
import org.springframework.security.messaging.context.SecurityContextChannelInterceptor;

import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import lombok.extern.slf4j.Slf4j;

/**
 * BigStock WebSocket STOMP Security
 *
 * Web:
 *   Cookie JWT + Redis
 *
 * Mobile:
 *   Query JWT + FCM + Redis
 *
 * JWT 已由 JwtHandshakeInterceptor 驗證。
 * PrincipalHandshakeHandler 負責建立 Authentication。
 *
 * 不使用 @EnableWebSocketSecurity，
 * 避免 Mobile STOMP CONNECT 被強制要求 CSRF。
 */
@Slf4j
@Configuration
public class WebSocketMessageSecurityConfig
        implements WebSocketMessageBrokerConfigurer {

    private final AuthorizationManager<Message<?>>
            bigStockMessageAuthorizationManager;

    public WebSocketMessageSecurityConfig(
            AuthorizationManager<Message<?>>
                    bigStockMessageAuthorizationManager
    ) {
        this.bigStockMessageAuthorizationManager =
                bigStockMessageAuthorizationManager;
    }

    @Bean
    public static AuthorizationManager<Message<?>>
            bigStockMessageAuthorizationManager() {

        MessageMatcherDelegatingAuthorizationManager.Builder messages =
                new MessageMatcherDelegatingAuthorizationManager.Builder();

        messages

                // CONNECT、HEARTBEAT、DISCONNECT 等無 Destination 訊息
                .nullDestMatcher()
                .authenticated()

                // 只允許六個市場行情 Topic
                .simpSubscribeDestMatchers(
                        "/topic/marketTWPM",
                        "/topic/marketTWPMN",
                        "/topic/marketNASDAQ_F",
                        "/topic/marketS_P_500_F",
                        "/topic/marketDOW_JONES_F",
                        "/topic/marketPHLX_SEMICONDUCTOR_SECTOR"
                )
                .authenticated()

                // 其他 SUBSCRIBE 與 MESSAGE 一律拒絕
                .simpTypeMatchers(
                        SimpMessageType.SUBSCRIBE,
                        SimpMessageType.MESSAGE
                )
                .denyAll()

                // 未明確允許的訊息拒絕
                .anyMessage()
                .denyAll();

        return messages.build();
    }

    @Override
    public void configureClientInboundChannel(
            ChannelRegistration registration
    ) {

        log.info(
                "[WS SECURITY] Registering STOMP authorization interceptors"
        );

        registration.interceptors(
                new SecurityContextChannelInterceptor(),
                new AuthorizationChannelInterceptor(
                        bigStockMessageAuthorizationManager
                )
        );
    }
}