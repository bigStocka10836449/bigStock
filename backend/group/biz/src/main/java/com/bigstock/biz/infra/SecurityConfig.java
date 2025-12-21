package com.bigstock.biz.infra;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.bigstock.sharedComponent.entity.RolePath;
import com.bigstock.sharedComponent.service.RolePathService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Value("${server.oauth2.secret-key}")
    private String secretKey;

    private final BigStockJwtAuthFilter jwtFilter;
    private final RolePathService rolePathService;

    /* ========= JWT Decoder ========= */

    @Bean
    public JwtDecoder jwtDecoder() {
        SecretKey key = new SecretKeySpec(secretKey.getBytes(), "HmacSHA512");
        return NimbusJwtDecoder.withSecretKey(key).build();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /* ========= Security Filter Chain ========= */

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

    	List<RolePath> rolePaths = rolePathService.getAllRolePaths();

        http
            .csrf(csrf -> csrf.disable())
            .httpBasic(httpBasic -> httpBasic.disable())
            .formLogin(form -> form.disable())
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)

            .authorizeHttpRequests(auth -> {

                /* ===== 公開路徑 ===== */
                String[] publicPaths = {
                    "/gateway/swagger/**",
                    "/api/biz/swagger/**",
                    "/auth/swagger/**",
                    "/webjars/**",
                    "/actuator/health",
                    "/auth/**",
                    "/api/auth/**"
                };

                auth.requestMatchers(HttpMethod.OPTIONS, publicPaths).permitAll();
                auth.requestMatchers(HttpMethod.GET, publicPaths).permitAll();
                auth.requestMatchers(HttpMethod.POST, "/auth/**", "/api/auth/**").permitAll();

                /* =========================================================
                   動態 Role → Path（反轉成 Path → Roles）
                   ========================================================= */

                // key = method + path, value = roles
                record MethodPath(HttpMethod method, String path) {}

                Map<MethodPath, Set<String>> ruleMap = new HashMap<>();

                for (RolePath rolePath : rolePaths) {

                    // DB JSON: {"GET":["/a","/b"],"POST":["/c"]}
                    Map<String, List<String>> pathsMap =
                            parseRoleAllowedUrlPath(rolePath.getRoleAllowedUrlPath());

                    // ✅ 角色來源（請與 JWT authorities 一致）
                    String role = rolePath.getRoleId().toString();

                    pathsMap.forEach((method, paths) -> {
                        HttpMethod httpMethod = HttpMethod.valueOf(method);

                        for (String path : paths) {
                            MethodPath key = new MethodPath(httpMethod, path);
                            ruleMap
                                .computeIfAbsent(key, k -> new HashSet<>())
                                .add(role);
                        }
                    });
                }

                /* ===== 套用到 Spring Security ===== */
                ruleMap.forEach((key, roles) -> {

                    JwtAuthorizationManager authorizationManager =
                            new JwtAuthorizationManager(new ArrayList<>(roles));

                    auth.requestMatchers(key.method(), key.path())
                        .access(authorizationManager);
                });

                auth.anyRequest().authenticated();
            })

            .oauth2ResourceServer(oauth2 ->
                oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            );

        return http.build();
    }

    /* ========= JWT → Authority 轉換 ========= */

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Object roles = jwt.getClaims().get("roles");
            if (roles instanceof Collection<?> collection) {
                return collection.stream()
                    .map(Object::toString)
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toSet());
            }
            return List.of();
        });
        return converter;
    }

    /* ========= CORS（MVC 版本） ========= */

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:13001", "http://127.0.0.1:13001"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("X-Refreshed-Token"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /* ========= 原有 JSON parse 邏輯（保留） ========= */

    private Map<String, List<String>> parseRoleAllowedUrlPath(String json) {
        try {
            return new ObjectMapper().readValue(
                json, new TypeReference<Map<String, List<String>>>() {});
        } catch (IOException e) {
            throw new RuntimeException("Error parsing role_allowed_url_path JSON", e);
        }
    }
}