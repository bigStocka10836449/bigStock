package com.bigstock.auth.infra;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsUtils;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.server.WebFilter;

@Configuration
public class WebConfig {

    @Bean
    public WebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:13001")); //未來要改在設定檔裡面
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("X-Refreshed-Token"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        CorsWebFilter corsWebFilter = new CorsWebFilter(source);

        return (exchange, chain) -> {

            return corsWebFilter.filter(exchange, chain)
                .doOnSubscribe(subscription -> {
                    exchange.getResponse().getHeaders().add("Access-Control-Expose-Headers", "X-Refreshed-Token");
                });
        };
    }
}