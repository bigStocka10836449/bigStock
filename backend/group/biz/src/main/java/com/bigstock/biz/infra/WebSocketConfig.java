package com.bigstock.biz.infra;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;

import com.bigstock.biz.component.ConnectInterceptor;
import com.bigstock.biz.component.JwtHandshakeInterceptor;
import com.bigstock.biz.component.LocalSessionRegistry;
import com.bigstock.biz.component.PrincipalHandshakeHandler;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;
    private final PrincipalHandshakeHandler principalHandshakeHandler;
    private final ConnectInterceptor connectInterceptor;
    private final LocalSessionRegistry registry;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
		registry.enableSimpleBroker("/topic").setHeartbeatValue(new long[] { 10000, 10000 })
				.setTaskScheduler(webSocketTaskScheduler());
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .addInterceptors(jwtHandshakeInterceptor)
                .setHandshakeHandler(principalHandshakeHandler)
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(connectInterceptor);
    }
    
    /**
     * 監聽前端自主停止websocket連線或建立連線，將session實例存入到本地cache
     */
    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.addDecoratorFactory(handler ->
                new WebSocketHandlerDecorator(handler) {

                    @Override
                    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                        registry.register(session);
                        super.afterConnectionEstablished(session);
                    }

                    @Override
                    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
                        registry.remove(session.getId());
                        super.afterConnectionClosed(session, status);
                    }
                });
    }
    
    /**
     * 設定socket session 心跳監控排程
     * @return
     */
    @Bean
    public ThreadPoolTaskScheduler webSocketTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("ws-heartbeat-");
        scheduler.initialize();
        return scheduler;
    }
}