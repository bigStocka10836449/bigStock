package com.bigstock.biz.infra;

import java.util.function.Supplier;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;

import org.springframework.util.StringUtils;

/**
 * ============================================================
 * BigStock - SPA CSRF Token Request Handler
 * ============================================================
 *
 * Supports:
 *
 * 1. Vue / Axios raw CSRF header
 * 2. Spring Security BREACH protection
 * 3. Deferred CSRF token loading
 *
 * Based on Spring Security SPA CSRF approach.
 */
public final class SpaCsrfTokenRequestHandler
        implements CsrfTokenRequestHandler {

    /**
     * Handles plain CSRF tokens from Axios headers.
     */
    private final CsrfTokenRequestAttributeHandler plain =
            new CsrfTokenRequestAttributeHandler();

    /**
     * Handles masked CSRF tokens for BREACH protection.
     */
    private final CsrfTokenRequestAttributeHandler xor =
            new XorCsrfTokenRequestAttributeHandler();

    public SpaCsrfTokenRequestHandler() {

        /*
         * Ensure deferred CSRF token is loaded
         * when needed.
         */
        this.xor.setCsrfRequestAttributeName(null);
    }

    /**
     * Expose masked CSRF token as request attribute.
     */
    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            Supplier<CsrfToken> csrfToken) {

        this.xor.handle(
                request,
                response,
                csrfToken
        );
    }

    /**
     * Resolve CSRF token submitted by the client.
     *
     * Axios header:
     *     Use plain token.
     *
     * Other supported submission methods:
     *     Use XOR token handler.
     */
    @Override
    public String resolveCsrfTokenValue(
            HttpServletRequest request,
            CsrfToken csrfToken) {

        String headerValue =
                request.getHeader(
                        csrfToken.getHeaderName()
                );

        return (
                StringUtils.hasText(headerValue)
                        ? this.plain
                        : this.xor
        ).resolveCsrfTokenValue(
                request,
                csrfToken
        );
    }
}