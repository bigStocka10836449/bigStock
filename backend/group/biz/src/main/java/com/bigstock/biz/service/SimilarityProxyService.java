package com.bigstock.biz.service;

import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import com.bigstock.biz.infra.SimilarityApiProperties;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SimilarityProxyService {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public SimilarityProxyService(SimilarityApiProperties properties) {
        this.baseUrl = properties.getBaseUrl().trim().replaceAll("/+$", "");
        URI baseUri = URI.create(baseUrl);
        if (!("http".equals(baseUri.getScheme()) || "https".equals(baseUri.getScheme()))
                || baseUri.getHost() == null
                || baseUri.getRawQuery() != null
                || baseUri.getRawFragment() != null) {
            throw new IllegalArgumentException("similarity.api.base-url must be an HTTP(S) service URL");
        }

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMillis(properties.getConnectTimeout()));
        factory.setReadTimeout(timeoutMillis(properties.getReadTimeout()));
        this.restTemplate = new RestTemplate(factory);
    }

    public ResponseEntity<byte[]> search(byte[] request) {
        return exchange(HttpMethod.POST, "/similarity/search", request);
    }

    public ResponseEntity<byte[]> reloadCache() {
        return exchange(HttpMethod.POST, "/cache/reload", null);
    }

    public ResponseEntity<byte[]> cacheStatus() {
        return exchange(HttpMethod.GET, "/cache/status", null);
    }

    public ResponseEntity<byte[]> health() {
        return exchange(HttpMethod.GET, "/health", null);
    }

    private ResponseEntity<byte[]> exchange(HttpMethod method, String path, byte[] request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        if (request != null) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }

        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    URI.create(baseUrl + path), method, new HttpEntity<>(request, headers), byte[].class);
            return upstreamResponse(response.getStatusCode(), response.getHeaders(), response.getBody());
        } catch (RestClientResponseException e) {
            return upstreamResponse(e.getStatusCode(), e.getResponseHeaders(), e.getResponseBodyAsByteArray());
        } catch (ResourceAccessException e) {
            if (isTimeout(e)) {
                log.warn("Similarity service timed out, path={}", path);
                return proxyError(HttpStatus.GATEWAY_TIMEOUT, "Similarity service request timed out");
            }
            log.warn("Similarity service is unavailable, path={}", path);
            return proxyError(HttpStatus.BAD_GATEWAY, "Similarity service is unavailable");
        } catch (RestClientException e) {
            log.warn("Similarity service returned an invalid response, path={}", path);
            return proxyError(HttpStatus.BAD_GATEWAY, "Invalid response from similarity service");
        }
    }

    private ResponseEntity<byte[]> upstreamResponse(
            HttpStatusCode status, HttpHeaders upstreamHeaders, byte[] body) {
        HttpHeaders headers = new HttpHeaders();
        MediaType contentType = upstreamHeaders == null ? null : upstreamHeaders.getContentType();
        headers.setContentType(contentType == null ? MediaType.APPLICATION_JSON : contentType);
        if (upstreamHeaders != null) {
            String retryAfter = upstreamHeaders.getFirst(HttpHeaders.RETRY_AFTER);
            if (retryAfter != null) {
                headers.set(HttpHeaders.RETRY_AFTER, retryAfter);
            }
        }
        return new ResponseEntity<>(body, headers, status);
    }

    private ResponseEntity<byte[]> proxyError(HttpStatus status, String detail) {
        byte[] body = ("{\"detail\":\"" + detail + "\"}").getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(body);
    }

    private boolean isTimeout(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof SocketTimeoutException || cause instanceof HttpTimeoutException) {
                return true;
            }
        }
        return false;
    }

    private int timeoutMillis(Duration timeout) {
        long millis = timeout.toMillis();
        if (millis <= 0 || millis > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Similarity API timeouts must be positive and fit in milliseconds");
        }
        return (int) millis;
    }
}