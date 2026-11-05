package com.bigstock.biz.infra;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 執行於 JwtHandshakeInterceptor 之前。 Cookie Web：Origin 一定要在白名單；原生 Mobile Query JWT
 * 可不帶 Origin。 注意：真正身份仍由 JwtHandshakeInterceptor 驗證簽章、role、token_type、Redis/FCM。
 */
@Component
public class BigStockWebSocketOriginInterceptor implements HandshakeInterceptor {
	private final Set<String> allowedOrigins;

	public BigStockWebSocketOriginInterceptor(@Value("${security.websocket.allowed-origins}") String origins) {
		this.allowedOrigins = new HashSet<>();
		Arrays.stream(origins.split(",")).map(String::trim).filter(s -> !s.isBlank()).forEach(allowedOrigins::add);
		if (allowedOrigins.isEmpty() || allowedOrigins.contains("*")) {
			throw new IllegalStateException("WebSocket allowed origins must be explicit");
		}
	}

	@Override
	public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
			Map<String, Object> attributes) {
		if (!(request instanceof ServletServerHttpRequest servlet)) {
			response.setStatusCode(HttpStatus.FORBIDDEN);
			return false;
		}
		HttpServletRequest http = servlet.getServletRequest();
		String origin = http.getHeader("Origin");
		boolean hasCookieCredential = false;
		if (http.getCookies() != null) {
			for (Cookie cookie : http.getCookies()) {
				if ("bstock_guest_token".equals(cookie.getName())) {
					hasCookieCredential = true;
					break;
				}
			}
		}
		// Browsers must carry allowed Origin; native Mobile can omit Origin only when
		// no Web JWT cookie.
		boolean allowed = hasCookieCredential ? origin != null && allowedOrigins.contains(origin)
				: origin == null || allowedOrigins.contains(origin);
		if (!allowed)
			response.setStatusCode(HttpStatus.FORBIDDEN);
		return allowed;
	}

	@Override
	public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
			Exception exception) {
	}
}
