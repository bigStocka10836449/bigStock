package com.bigstock.biz.infra;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class TurnstileConfig {

    @Bean("turnstileRestTemplate")
    public RestTemplate turnstileRestTemplate(
            RestTemplateBuilder builder,
            @Value("${security.turnstile.connect-timeout}")
            long connectTimeout,
            @Value("${security.turnstile.read-timeout}")
            long readTimeout) {

        return builder
                .setConnectTimeout(
                    Duration.ofMillis(connectTimeout)
                )
                .setReadTimeout(
                    Duration.ofMillis(readTimeout)
                )
                .build();
    }
}