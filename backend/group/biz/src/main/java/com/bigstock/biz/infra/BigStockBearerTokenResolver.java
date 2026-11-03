package com.bigstock.biz.infra;

import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.server.resource.BearerTokenErrors;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

@Component
public class BigStockBearerTokenResolver implements BearerTokenResolver {

    private static final String JWT_COOKIE_NAME =
            "bstock_guest_token";

    private final DefaultBearerTokenResolver defaultResolver =
            new DefaultBearerTokenResolver();

    @Override
    public String resolve(HttpServletRequest request) {

        // =====================================================
        // 1. Mobile: Authorization Bearer
        // =====================================================

        String bearerToken = defaultResolver.resolve(request);

        // =====================================================
        // 2. Web: HttpOnly Cookie
        // =====================================================

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

        // =====================================================
        // 3. Reject duplicate cookies
        // =====================================================

        if (cookieCount > 1) {
            throw invalidToken();
        }

        // =====================================================
        // 4. Reject ambiguous authentication sources
        // =====================================================

        if (bearerToken != null && cookieToken != null) {
            throw invalidToken();
        }

        // =====================================================
        // 5. Mobile
        // =====================================================

        if (bearerToken != null) {
            return bearerToken;
        }

        // =====================================================
        // 6. Web
        // =====================================================

        if (cookieToken != null) {

            if (cookieToken.isBlank()) {
                throw invalidToken();
            }

            return cookieToken.trim();
        }

        return null;
    }

    private OAuth2AuthenticationException invalidToken() {
        return new OAuth2AuthenticationException(
            BearerTokenErrors.invalidToken(
                "Invalid or ambiguous authentication token"
            )
        );
    }
}