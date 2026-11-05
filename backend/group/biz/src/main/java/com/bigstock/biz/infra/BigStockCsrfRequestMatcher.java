package com.bigstock.biz.infra;

import java.util.Set;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;

@Component
public class BigStockCsrfRequestMatcher implements RequestMatcher {

    private static final String JWT_COOKIE_NAME =
            "bstock_guest_token";

    private static final Set<String> SAFE_METHODS = Set.of(
            "GET",
            "HEAD",
            "OPTIONS"
    );

    @Override
    public boolean matches(HttpServletRequest request) {

        // =====================================================
        // 1. Safe HTTP methods
        // =====================================================

        if (SAFE_METHODS.contains(request.getMethod())) {
            return false;
        }

        // =====================================================
        // 2. Check Web JWT Cookie
        // =====================================================

        boolean hasWebCookie = false;

        Cookie[] cookies = request.getCookies();

        if (cookies != null) {

            for (Cookie cookie : cookies) {

                if (JWT_COOKIE_NAME.equals(cookie.getName())) {

                    hasWebCookie = true;

                    break;
                }
            }
        }

        // =====================================================
        // 3. Check Mobile Bearer Header
        // =====================================================

        String authorization =
                request.getHeader("Authorization");

        boolean hasBearerHeader =
                authorization != null
                && authorization.regionMatches(
                        true,
                        0,
                        "Bearer ",
                        0,
                        7
                );

        // =====================================================
        // 4. Mobile Bearer only -> No CSRF
        // =====================================================

        if (hasBearerHeader && !hasWebCookie) {
            return false;
        }

        // =====================================================
        // 5. Web / Cookie / Anonymous unsafe requests
        //    -> Require CSRF
        // =====================================================

        return true;
    }
}
