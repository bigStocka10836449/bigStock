package com.bigstock.biz.component;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;

@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

	@Value("${server.oauth2.secret-key}")
	private String secretKey;

	@Override
	public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
			Map<String, Object> attributes) throws IOException {

		if (request instanceof ServletServerHttpRequest servletRequest) {
			HttpServletRequest req = servletRequest.getServletRequest();
			String token = req.getParameter("token");

			if (token == null || token.isBlank())
				return false;

			// Replace with real JWT validation
			try {
				Claims claims = parseJwtToken(token);

				Date expiration = claims.getExpiration();
				if (expiration != null && new Date().after(expiration)) {
					unauthorized(response);
					return false;
				}

				String userId = claims.getSubject();
				if (userId == null) {
					unauthorized(response);
					return false;
				}

				attributes.put("userId", userId);
				return true;

			} catch (Exception e) {
				unauthorized(response);
				return false;
			}
		}
		return false;
	}

	@Override
	public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
			Exception exception) {
	}

	private Claims parseJwtToken(String token) {
		Jws<Claims> jws = Jwts.parser().verifyWith(Keys.hmacShaKeyFor(secretKey.getBytes())).build()
				.parseSignedClaims(token);
		return jws.getPayload();
	}

	private void unauthorized(ServerHttpResponse response) {
		response.setStatusCode(HttpStatus.UNAUTHORIZED);
	}
}
