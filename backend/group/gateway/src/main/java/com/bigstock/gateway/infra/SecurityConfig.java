package com.bigstock.gateway.infra;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	@Value("${server.oauth2.secret-key}")
	private String secretKey;

	@Autowired
	BigStockGatewayCustomWebFilter bigStockGatewayCustomWebFilter;

	@Bean
	public ReactiveJwtDecoder reactiveJwtDecoder() {
		SecretKey signingKey = new SecretKeySpec(secretKey.getBytes(), "HMAC-SHA-512");
		return NimbusReactiveJwtDecoder.withSecretKey(signingKey).build();
	}

//	@Bean
//	public JwtDecoder jwtDecoder(ReactiveJwtDecoder reactiveJwtDecoder) {
//	    return new JwtDecoderAdapter(reactiveJwtDecoder);
//	}

	@Bean
	public JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
		// 設置你的 JwtAuthenticationConverter 配置
		return jwtAuthenticationConverter;
	}

	@Bean
	public BCryptPasswordEncoder bCryptPasswordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http) throws Exception { // disable CSRF
		http.addFilterBefore(bigStockGatewayCustomWebFilter, SecurityWebFiltersOrder.HTTP_BASIC).cors()
				.configurationSource(corsConfiguration()).and()
				.authorizeExchange(exchanges -> exchanges.pathMatchers(HttpMethod.OPTIONS).permitAll()
						// EAM 系統
						.pathMatchers(HttpMethod.POST, "/txn")
						.access(new JwtReactiveAuthorizationManager(List.of("Admin", "Member", "User")))
						.pathMatchers(HttpMethod.POST, "/gateway/**")
						.access(new JwtReactiveAuthorizationManager(List.of("Admin", "Member", "User")))
						.pathMatchers(HttpMethod.GET, "/gateway/swagger/**").permitAll()
						.pathMatchers(HttpMethod.GET, "/gateway/**")
						.access(new JwtReactiveAuthorizationManager(List.of("Admin", "Member", "User")))
						.pathMatchers(HttpMethod.PATCH, "/schedule/**")
						.access(new JwtReactiveAuthorizationManager(List.of("Admin", "Member", "User")))
						.pathMatchers(HttpMethod.GET, "/biz/**")		
						.access(new JwtReactiveAuthorizationManager(List.of("Admin", "Member", "User")))
						.pathMatchers(HttpMethod.GET, "/api/biz/**")		
						.access(new JwtReactiveAuthorizationManager(List.of("Admin", "Member", "User")))
						.pathMatchers(HttpMethod.GET, "/api/biz/swagger/**").permitAll()
						.pathMatchers(HttpMethod.GET, "/auth/swagger/**").permitAll()
						.pathMatchers(HttpMethod.POST, "/biz/**")
						.access(new JwtReactiveAuthorizationManager(List.of("Admin", "Member")))
						.pathMatchers(HttpMethod.POST, "/auth/**").permitAll()
						.pathMatchers(HttpMethod.POST, "/api/auth/**").permitAll()
//						.pathMatchers(HttpMethod.POST, "/swagger/**").permitAll()
//						.pathMatchers(HttpMethod.GET, "/swagger/**").permitAll()
						.pathMatchers(HttpMethod.POST, "/webjars/**").permitAll()
						.pathMatchers(HttpMethod.GET, "/webjars/**").permitAll()
//						.pathMatchers(HttpMethod.POST, "/swagger-ui.html").permitAll()
//						.pathMatchers(HttpMethod.GET, "/swagger-ui.html").permitAll()

						.pathMatchers(HttpMethod.GET, "/actuator/health").permitAll().and()
						.oauth2ResourceServer(oauth2ResourceServer -> oauth2ResourceServer.jwt()
								.jwtAuthenticationConverter(this::convert)))
				.csrf().disable().httpBasic().disable().formLogin().disable();
		;

		return http.build();
	}

	CorsConfigurationSource corsConfiguration() {
		CorsConfiguration corsConfig = new CorsConfiguration();
		corsConfig.applyPermitDefaultValues();
		corsConfig.addAllowedMethod(HttpMethod.GET);
		corsConfig.addAllowedMethod(HttpMethod.POST);
		corsConfig.addAllowedMethod(HttpMethod.PATCH);
		corsConfig.addAllowedMethod(HttpMethod.PUT);
		corsConfig.addAllowedMethod(HttpMethod.DELETE);
		corsConfig.addAllowedMethod(HttpMethod.OPTIONS);
//		corsConfig.setAllowedOrigins(Arrays.asList("http://127.0.0.1:8081", "http://localhost:8080"));
		corsConfig.setAllowedOrigins(Arrays.asList("*"));
		corsConfig.setAllowedHeaders(Arrays.asList("*"));
		corsConfig.setMaxAge(36000L);
		corsConfig.setAllowCredentials(false); // When allowCredentials is true, allowedOrigins cannot contain the
												// special value "*"

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", corsConfig);
		return source;
	}

	@SuppressWarnings("unchecked")
	private Mono<JwtAuthenticationToken> convert(Jwt jwt) {
		Object authoritiesObj = jwt.getClaims().get("roles");
		if (authoritiesObj == null) {
			return Mono.just(new JwtAuthenticationToken(jwt, CollectionUtils.EMPTY_COLLECTION));
		}
		Collection<SimpleGrantedAuthority> authorities = ((Collection<String>) authoritiesObj).stream()
				.map(SimpleGrantedAuthority::new).collect(Collectors.toSet());
		return Mono.just(new JwtAuthenticationToken(jwt, authorities));
	}
}
