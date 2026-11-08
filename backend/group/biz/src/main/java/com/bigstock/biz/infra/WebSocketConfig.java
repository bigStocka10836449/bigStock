package com.bigstock.biz.infra;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
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
import com.bigstock.biz.component.SocketSingleSessionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	private final JwtHandshakeInterceptor jwtHandshakeInterceptor;
	private final PrincipalHandshakeHandler principalHandshakeHandler;
	private final ConnectInterceptor connectInterceptor;
	private final LocalSessionRegistry registry;
	private final SocketSingleSessionService sessions;
	private final BigStockWebSocketOriginInterceptor bigStockWebSocketOriginInterceptor;

	@Value("${security.websocket.allowed-origins}")
	private String webSocketAllowedOrigins;

	@Override
	public void configureMessageBroker(MessageBrokerRegistry registry) {

		registry.enableSimpleBroker("/topic").setHeartbeatValue(new long[] { 10000, 10000 })
				.setTaskScheduler(webSocketTaskScheduler());
	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		String[] allowedOrigins = Arrays.stream(webSocketAllowedOrigins.split(",")).map(String::trim)
				.filter(s -> !s.isBlank()).toArray(String[]::new);
		if (allowedOrigins.length == 0 || Arrays.asList(allowedOrigins).contains("*")) {
			throw new IllegalStateException("Explicit WebSocket origins are required");
		}
		registry.addEndpoint("/ws").setAllowedOrigins(allowedOrigins)
				.addInterceptors(bigStockWebSocketOriginInterceptor, jwtHandshakeInterceptor)
				.setHandshakeHandler(principalHandshakeHandler).withSockJS();
	}

	@Override
	public void configureClientInboundChannel(ChannelRegistration registration) {
		registration.interceptors(connectInterceptor);
	}

	@Override
	public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
		// Test against the largest actual inbound STOMP payload before deploying.
		registration.setMessageSizeLimit(64 * 1024);
		registration.setSendBufferSizeLimit(512 * 1024);
		registration.setSendTimeLimit(15_000);
		registration.setTimeToFirstMessage(30_000);

		registration.addDecoratorFactory(handler -> new WebSocketHandlerDecorator(handler) {
			@Override
			public void afterConnectionEstablished(WebSocketSession session) throws Exception {
				registry.register(session);
				super.afterConnectionEstablished(session);
			}

			@Override
			public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
				try {
					registry.remove(session.getId());
					// Compare-and-delete prevents an old connection from deleting
					// a new connection's active session mapping.
					try {
						sessions.disconnect(session.getId());
					} catch (RuntimeException ex) {
						log.error("[WS] Redis cleanup failed", ex);
					}
				} finally {
					super.afterConnectionClosed(session, status);
				}
			}
		});
	}

	@Bean
	public ThreadPoolTaskScheduler webSocketTaskScheduler() {
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.setPoolSize(2);
		scheduler.setThreadNamePrefix("ws-heartbeat-");
		scheduler.initialize();
		return scheduler;
	}
}
