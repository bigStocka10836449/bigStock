package com.bigstock.biz.component;

import java.security.Principal;
import java.util.Collections;
import java.util.Map;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

@Component
public class PrincipalHandshakeHandler extends DefaultHandshakeHandler {

    /**
     * JWT 已由 JwtHandshakeInterceptor 驗證成功。
     *
     * 從經過驗證的 Handshake Attributes 取得 userId，
     * 並建立 Spring Security Authentication。
     *
     * 同時支援：
     * Web    → Cookie JWT
     * Mobile → Query JWT
     */
    @Override
    protected Principal determineUser(
            ServerHttpRequest request,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) {

        Object value = attributes.get("userId");

        if (!(value instanceof String userId)
                || userId.isBlank()) {

            throw new IllegalStateException(
                    "WebSocket authenticated userId is missing"
            );
        }

        return new UsernamePasswordAuthenticationToken(
                userId,
                null,
                Collections.singletonList(
                        new SimpleGrantedAuthority("ROLE_GUEST")
                )
        );
    }
}
