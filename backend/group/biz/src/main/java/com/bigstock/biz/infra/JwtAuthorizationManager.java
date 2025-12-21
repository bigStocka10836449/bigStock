package com.bigstock.biz.infra;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JwtAuthorizationManager
        implements AuthorizationManager<RequestAuthorizationContext> {

    private final List<String> authoriMenuLies;

    public JwtAuthorizationManager(List<String> authoriMenuLies) {
        this.authoriMenuLies = authoriMenuLies;
    }

    @Override
    public AuthorizationDecision check(
            Supplier<Authentication> authenticationSupplier,
            RequestAuthorizationContext context) {

        Authentication authentication = authenticationSupplier.get();
        return checkAuthorities(authentication);
    }

    private AuthorizationDecision checkAuthorities(Authentication auth) {

        if (auth == null || !(auth.getPrincipal() instanceof Jwt jwt)) {
            log.info("Authentication or JWT principal is missing");
            return new AuthorizationDecision(false);
        }

        Collection<? extends GrantedAuthority> tokenAuthorities = auth.getAuthorities();

        if (CollectionUtils.isEmpty(tokenAuthorities)) {
            log.info("Access Token does not have any authority");
            return new AuthorizationDecision(false);
        }

        boolean isMatch = tokenAuthorities.stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authoriMenuLies::contains);

        if (!isMatch) {
            log.info("Insufficient authority, required one of: {}", authoriMenuLies);
            return new AuthorizationDecision(false);
        }

        return new AuthorizationDecision(true);
    }
}