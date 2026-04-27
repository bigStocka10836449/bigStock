package com.bigstock.biz.infra;

import java.io.IOException;
import java.util.Date;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class BigStockJwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(BigStockJwtAuthFilter.class);

    @Value("${server.oauth2.secret-key}")
    private String secretKey;


    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        if (!path.startsWith("/actuator/")) {
            log.info("Request URL: {}", request.getRequestURI());
        }

        // 白名單 ,"/device/**"
        if (path.startsWith("/actuator/")
                || path.startsWith("/auth/")
                || path.startsWith("/api/guest-token")
                || path.startsWith("/api/refresh-token")
                || path.contains("/swagger")
                || path.contains("/webjars")||
                path.contains("/auth/") || path.contains("/stockShareholderStructure")|| path.contains("/financialCalendarImport")|| path.contains("/device") || path.contains("/ws")) {

            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            unauthorized(response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            Claims claims = parseJwtToken(token);

            String role = claims.get("role", String.class);
            if ("Guest".equals(role)) {
                filterChain.doFilter(request, response);
                return;
            }

            Date expiration = claims.getExpiration();
            if (expiration != null && new Date().after(expiration)) {
                unauthorized(response);
                return;
            }

            // 若之後要放入 SecurityContext，可在這裡處理
            filterChain.doFilter(request, response);

        } catch (JwtException e) {
            unauthorized(response);
        }
    }

    private Claims parseJwtToken(String token) {
        Jws<Claims> jws = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(secretKey.getBytes()))
                .build()
                .parseSignedClaims(token);
        return jws.getPayload();
    }

    private void unauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.getWriter().write("Unauthorized");
    }
}