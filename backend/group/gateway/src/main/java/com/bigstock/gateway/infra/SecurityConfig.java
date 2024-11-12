package com.bigstock.gateway.infra;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
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

import com.bigstock.sharedComponent.entity.RolePath;
import com.bigstock.sharedComponent.service.RolePathService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

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

	@Bean
	public JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
		return jwtAuthenticationConverter;
	}

	@Bean
	public BCryptPasswordEncoder bCryptPasswordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http, RolePathService rolePathService)
			throws Exception {

		// 獲取所有的角色及其對應的路徑
		List<RolePath> rolePaths = rolePathService.getAllRolePaths();

		http.addFilterBefore(bigStockGatewayCustomWebFilter, SecurityWebFiltersOrder.HTTP_BASIC).cors()
				.configurationSource(corsConfiguration()).and().authorizeExchange(exchanges -> {

					// 動態生成每個角色的 pathMatchers
				    for (RolePath rolePath : rolePaths) {
		                String roleId = rolePath.getRoleId().toString();
		                Map<String, List<String>> pathsMap = parseRoleAllowedUrlPath(rolePath.getRoleAllowedUrlPath());

		                // 根據每個 HTTP 方法設置 pathMatchers 和動態的 JwtReactiveAuthorizationManager
		                for (Map.Entry<String, List<String>> entry : pathsMap.entrySet()) {
		                    String httpMethod = entry.getKey();
		                    List<String> paths = entry.getValue();

		                    JwtReactiveAuthorizationManager authorizationManager = new JwtReactiveAuthorizationManager(List.of(roleId));

		                    // 動態配置每個 HTTP 方法對應的路徑和授權管理器
		                    switch (httpMethod) {
		                        case "GET":
		                            exchanges.pathMatchers(HttpMethod.GET, paths.toArray(new String[0]))
		                                     .access(authorizationManager);
		                            break;
		                        case "POST":
		                            exchanges.pathMatchers(HttpMethod.POST, paths.toArray(new String[0]))
		                                     .access(authorizationManager);
		                            break;
		                        // 其他 HTTP 方法的處理
		                    }
		                }
		            }

					String[] publicPaths = { "/gateway/swagger/**", "/api/biz/swagger/**", "/auth/swagger/**",
							"/webjars/**", "/actuator/health" };
					exchanges.pathMatchers(HttpMethod.OPTIONS, publicPaths).permitAll();
					exchanges.pathMatchers(HttpMethod.GET, publicPaths).permitAll();
					exchanges.pathMatchers(HttpMethod.POST, "/auth/**", "/api/auth/**", "/webjars/**").permitAll();
				}).oauth2ResourceServer(oauth2 -> oauth2.jwt().jwtAuthenticationConverter(this::convert)).csrf()
				.disable().httpBasic().disable().formLogin().disable();

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

	private Map<String, List<String>> parseRoleAllowedUrlPath(String roleAllowedUrlPath) {
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			return objectMapper.readValue(roleAllowedUrlPath, new TypeReference<Map<String, List<String>>>() {
			});
		} catch (IOException e) {
			throw new RuntimeException("Error parsing role_allowed_url_path JSON", e);
		}
	}
}
