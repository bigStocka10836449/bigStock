package com.bigstock.biz.component;


import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

    private static final String JWT_COOKIE_NAME =
            "bstock_guest_token";

    private final FcmRecordService fcmRecordService;

    @Value("${server.oauth2.secret-key}")
    private String secretKey;

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) throws IOException {

        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            unauthorized(response);
            return false;
        }

        HttpServletRequest httpRequest =
                servletRequest.getServletRequest();

        // ====================================================
        // 1. Resolve JWT
        // ====================================================

        String token = resolveToken(httpRequest);

        if (token == null || token.isBlank()) {
            unauthorized(response);
            return false;
        }

        // ====================================================
        // 2. Verify JWT
        // ====================================================

        final Claims claims;

        try {
            claims = Jwts.parser()
                    .verifyWith(
                            Keys.hmacShaKeyFor(
                                    secretKey.getBytes(
                                            StandardCharsets.UTF_8
                                    )
                            )
                    )
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

        } catch (JwtException | IllegalArgumentException e) {

            log.warn("[WS AUTH] Invalid JWT");

            unauthorized(response);
            return false;
        }

        // ====================================================
        // 3. Verify Expiration
        // ====================================================

        Date expiration = claims.getExpiration();

        if (expiration == null ||
                !expiration.after(new Date())) {

            unauthorized(response);
            return false;
        }

        // ====================================================
        // 4. Verify Subject
        // ====================================================

        String userId = claims.getSubject();

        if (userId == null || userId.isBlank()) {

            unauthorized(response);
            return false;
        }

        // ====================================================
        // 5. Verify FCM Device
        // ====================================================

        final FcmRecord fcmRecord;

        try {
            fcmRecord = fcmRecordService.getFcmRecord(token);

        } catch (RuntimeException e) {

            log.error(
                    "[WS AUTH] FCM verification lookup failed",
                    e
            );

            response.setStatusCode(
                    HttpStatus.SERVICE_UNAVAILABLE
            );

            return false;
        }

        if (fcmRecord == null ||
                fcmRecord.getFcmToken() == null ||
                fcmRecord.getFcmToken().isBlank()) {

            unauthorized(response);
            return false;
        }

        /*
         * getFcmRecord(token) 必須確保：
         *
         * - 裝置已完成驗證
         * - Token 尚未撤銷
         * - 裝置紀錄仍有效
         *
         * 如果目前的方法只是單純查詢，
         * 請在 FcmRecordService 補上完整條件。
         */

        // ====================================================
        // 6. Store userId
        // ====================================================

        attributes.put(
                WebSocketAttributes.USER_ID,
                userId
        );

        log.debug(
                "[WS AUTH] Handshake authenticated, userId={}",
                userId
        );

        return true;
    }

    /**
     * New Web:
     * Cookie: bstock_guest_token=<JWT>
     *
     * Legacy:
     * /ws/websocket?token=<JWT>
     *
     * Reject ambiguous authentication sources.
     */
    private String resolveToken(HttpServletRequest request) {

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

        String[] queryTokens =
                request.getParameterValues("token");

        if (queryTokens != null && queryTokens.length != 1) {
            return null;
        }

        String queryToken =
                queryTokens == null ? null : queryTokens[0];

        if (cookieToken != null && queryToken != null) {
            return null;
        }

        String resolved =
                cookieToken != null
                        ? cookieToken
                        : queryToken;

        if (resolved == null || resolved.isBlank()) {
            return null;
        }

        return resolved.trim();
    }

    private void unauthorized(ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception
    ) {
        // No additional cleanup.
    }
}