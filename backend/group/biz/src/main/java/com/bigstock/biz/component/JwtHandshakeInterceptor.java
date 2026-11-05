package com.bigstock.biz.component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import com.bigstock.sharedComponent.entity.FcmRecord;
import com.bigstock.sharedComponent.service.FcmRecordService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

	private static final String JWT_COOKIE_NAME = "bstock_guest_token";

	private static final String WEB_TOKEN_TYPE = "web_guest";

	private static final String GUEST_ROLE = "4";

	private static final String WEB_JWT_REDIS_PREFIX = "web:jwt:";

	private final RedissonClient redissonClient;

	private final FcmRecordService fcmRecordService;

	@Value("${server.oauth2.secret-key}")
	private String secretKey;

	// ============================================================
	// Handshake
	// ============================================================

	@Override
	public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
			Map<String, Object> attributes) throws IOException {

		// ========================================================
		// 1. Resolve Servlet Request
		// ========================================================

		if (!(request instanceof ServletServerHttpRequest servletRequest)) {

			unauthorized(response);
			return false;
		}

		HttpServletRequest httpRequest = servletRequest.getServletRequest();

		// ========================================================
		// 2. Resolve Authentication Source
		// ========================================================

		ResolvedToken resolvedToken = resolveToken(httpRequest);

		if (resolvedToken == null) {

			log.debug("[WS AUTH] Missing or ambiguous credentials");

			unauthorized(response);
			return false;
		}

		String token = resolvedToken.token;

		// ========================================================
		// 3. Verify JWT Signature
		// ========================================================

		final Claims claims;

		try {

			claims = Jwts.parser().verifyWith(Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8))).build()
					.parseSignedClaims(token).getPayload();

		} catch (JwtException | IllegalArgumentException e) {

			log.debug("[WS AUTH] Invalid JWT");

			unauthorized(response);
			return false;
		}

		// ========================================================
		// 4. Verify Expiration
		// ========================================================

		Date expiration = claims.getExpiration();

		if (expiration == null || !expiration.after(new Date())) {

			log.debug("[WS AUTH] JWT expired");

			unauthorized(response);
			return false;
		}

		// ========================================================
		// 5. Verify Subject
		// ========================================================

		String userId = claims.getSubject();

		if (userId == null || userId.isBlank()) {

			unauthorized(response);
			return false;
		}

		// ========================================================
		// 6. Verify Guest Role
		// ========================================================

		Object rolesValue = claims.get("roles");

		if (!(rolesValue instanceof List<?> roles) || !roles.contains(GUEST_ROLE)) {

			log.debug("[WS AUTH] Invalid guest role");

			unauthorized(response);
			return false;
		}

		// ========================================================
		// 7. Determine Token Type
		// ========================================================

		String tokenType = claims.get("token_type", String.class);

		// ========================================================
		// 8. WEB: Turnstile JWT + Redis
		// ========================================================

		if (WEB_TOKEN_TYPE.equals(tokenType)) {

			// Web authentication only accepts Cookie.
			if (resolvedToken.source != TokenSource.COOKIE) {

				log.debug("[WS AUTH] Web JWT must use Cookie");

				unauthorized(response);
				return false;
			}

			if (!validateWebSession(userId, token, response)) {

				return false;
			}

			log.debug("[WS AUTH] Web guest authenticated, userId={}", userId);

			// ========================================================
			// 9. MOBILE: Legacy FCM JWT
			// ========================================================

		} else if (tokenType == null || tokenType.isBlank()) {

			// Existing Mobile WebSocket uses ?token=<JWT>.
			if (resolvedToken.source != TokenSource.QUERY) {

				log.debug("[WS AUTH] Mobile JWT must use query token");

				unauthorized(response);
				return false;
			}

			if (!validateMobileSession(userId, token, response)) {

				return false;
			}

			log.debug("[WS AUTH] Mobile guest authenticated, userId={}", userId);

			// ========================================================
			// 10. Unknown JWT Type
			// ========================================================

		} else {

			log.warn("[WS AUTH] Unsupported token type={}", tokenType);

			unauthorized(response);
			return false;
		}

		// ========================================================
		// 11. Store userId for HandshakeHandler / STOMP
		// ========================================================

		attributes.put(WebSocketAttributes.USER_ID, userId);

		return true;
	}

	// ============================================================
	// WEB Session Validation
	// ============================================================

	/**
	 * Web JWT must:
	 *
	 * 1. Have a valid signature and expiration. 2. Have token_type = web_guest. 3.
	 * Arrive through bstock_guest_token Cookie. 4. Match Redis web:jwt:{guestId}.
	 *
	 * No FcmRecord lookup.
	 */
	private boolean validateWebSession(String guestId, String token, ServerHttpResponse response) {

		String redisKey = WEB_JWT_REDIS_PREFIX + guestId;

		final String cachedToken;

		try {

			cachedToken = redissonClient.<String>getBucket(redisKey).get();

		} catch (RuntimeException e) {

			log.error("[WS AUTH] Web Redis session lookup failed", e);

			serviceUnavailable(response);
			return false;
		}

		if (!secureTokenEquals(token, cachedToken)) {

			log.debug("[WS AUTH] Web session not found or revoked");

			unauthorized(response);
			return false;
		}

		return true;
	}

	// ============================================================
	// MOBILE Session Validation
	// ============================================================

	/**
	 * Preserve Mobile FCM verification.
	 *
	 * Mobile JWT does not have token_type yet.
	 * 
	 * @throws IOException
	 */
	private boolean validateMobileSession(String guestId, String token, ServerHttpResponse response)
			throws IOException {

		final FcmRecord fcmRecord;

		try {

			fcmRecord = fcmRecordService.getFcmRecord(token);

		} catch (RuntimeException e) {

			log.error("[WS AUTH] Mobile FCM lookup failed", e);

			serviceUnavailable(response);
			return false;
		}

		if (fcmRecord == null || fcmRecord.getFcmToken() == null || fcmRecord.getFcmToken().isBlank()) {

			log.debug("[WS AUTH] Mobile FCM record invalid");

			unauthorized(response);
			return false;
		}

		// The JWT must still be the one allowed by FCM.
		if (!secureTokenEquals(token, fcmRecord.getAllowedJwt())) {

			unauthorized(response);
			return false;
		}

		// Verify that the FCM token belongs to this guestId.
		String expectedGuestId = org.apache.commons.codec.digest.DigestUtils.md5Hex(fcmRecord.getFcmToken())
				.toUpperCase(java.util.Locale.ROOT);

		if (!expectedGuestId.equals(guestId)) {

			unauthorized(response);
			return false;
		}

		// Preserve the Mobile Redis session binding.
		final String cachedToken;

		try {

			cachedToken = redissonClient.<String>getBucket("jwt:" + guestId).get();

		} catch (RuntimeException e) {

			log.error("[WS AUTH] Mobile Redis session lookup failed", e);

			serviceUnavailable(response);
			return false;
		}

		if (!secureTokenEquals(token, cachedToken)) {

			unauthorized(response);
			return false;
		}

		return true;
	}

	// ============================================================
	// Resolve JWT
	// ============================================================

	/**
	 * WEB: Cookie: bstock_guest_token=<JWT>
	 *
	 * MOBILE (legacy): /ws/websocket?token=<JWT>
	 *
	 * Reject ambiguous credential sources.
	 */
	private ResolvedToken resolveToken(HttpServletRequest request) {

		String cookieToken = null;
		int cookieCount = 0;

		Cookie[] cookies = request.getCookies();

		if (cookies != null) {

			for (Cookie cookie : cookies) {

				if (JWT_COOKIE_NAME.equals(cookie.getName())) {

					cookieCount++;
					cookieToken = cookie.getValue();
				}
			}
		}

		if (cookieCount > 1) {
			return null;
		}

		String[] queryTokens = request.getParameterValues("token");

		if (queryTokens != null && queryTokens.length != 1) {
			return null;
		}

		String queryToken = queryTokens == null ? null : queryTokens[0];

		// Reject multiple authentication sources.
		if (cookieToken != null && queryToken != null) {
			return null;
		}

		// Web Cookie.
		if (cookieToken != null && !cookieToken.isBlank()) {

			return new ResolvedToken(cookieToken.trim(), TokenSource.COOKIE);
		}

		// Legacy Mobile query token.
		if (queryToken != null && !queryToken.isBlank()) {

			return new ResolvedToken(queryToken.trim(), TokenSource.QUERY);
		}

		return null;
	}

	// ============================================================
	// Constant-Time Token Comparison
	// ============================================================

	private boolean secureTokenEquals(String expected, String actual) {

		if (expected == null || actual == null) {
			return false;
		}

		return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
				actual.getBytes(StandardCharsets.UTF_8));
	}

	// ============================================================
	// HTTP Error Responses
	// ============================================================

	private void unauthorized(ServerHttpResponse response) {

		response.setStatusCode(HttpStatus.UNAUTHORIZED);
	}

	private void serviceUnavailable(ServerHttpResponse response) {

		response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
	}

	// ============================================================
	// Authentication Source
	// ============================================================

	private enum TokenSource {

		COOKIE, QUERY
	}

	private static final class ResolvedToken {

		private final String token;

		private final TokenSource source;

		private ResolvedToken(String token, TokenSource source) {

			this.token = token;
			this.source = source;
		}
	}

	// ============================================================
	// After Handshake
	// ============================================================

	@Override
	public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
			Exception exception) {

		// No additional cleanup.
	}
}