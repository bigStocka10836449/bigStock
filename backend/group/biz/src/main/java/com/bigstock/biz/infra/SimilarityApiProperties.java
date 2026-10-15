package com.bigstock.biz.infra;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "similarity.api")
public class SimilarityApiProperties {

    private String baseUrl = "https://bigstock-similiarearch.zeabur.app";
    private Duration connectTimeout = Duration.ofSeconds(5);
    private Duration readTimeout = Duration.ofMinutes(5);
}