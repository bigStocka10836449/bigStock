package com.bigstock.biz.infra;

import java.util.Collection;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.server.authorization.AuthorizationContext;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
public class JwtReactiveAuthorizationManager implements ReactiveAuthorizationManager<AuthorizationContext> {

	public final List<String> authoriMenuLies;

	public JwtReactiveAuthorizationManager(List<String> authoriMenuLies) {
		this.authoriMenuLies = authoriMenuLies;
	}

	@Override
	public Mono<AuthorizationDecision> check(Mono<Authentication> authenticationMono,
			AuthorizationContext authorizationContext) {
		return authenticationMono.flatMap(this::checkAuthorities);
	}

	private Mono<AuthorizationDecision> checkAuthorities(Authentication auth) {
		Jwt principal = (Jwt) auth.getPrincipal();
		if (principal == null) {
			return Mono.just(new AuthorizationDecision(false));
		}
		Collection<? extends GrantedAuthority> tokenAuthorities = auth.getAuthorities();
		if (CollectionUtils.isEmpty(tokenAuthorities)) {
			log.info("Acess Token does not have any authority");
			return Mono.just(new AuthorizationDecision(false));
		}
		boolean isMatch = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority)
				.anyMatch(this.authoriMenuLies::contains);
		if (!isMatch) {
			log.info("Insufficient authority");
			return Mono.just(new AuthorizationDecision(false));
		}
		return  Mono.just(new AuthorizationDecision(true));
	}
}