package com.bigstock.biz.service;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import com.bigstock.sharedComponent.dto.TurnstileVerifyResponse;

@Service
public class TurnstileService {

    private static final Logger log =
            LoggerFactory.getLogger(TurnstileService.class);

    private static final String VERIFY_URL =
            "https://challenges.cloudflare.com/turnstile/v0/siteverify";

    private final RestTemplate restTemplate;

    @Value("${security.turnstile.secret-key}")
    private String secretKey;

    @Value("${security.turnstile.expected-hostname}")
    private String expectedHostname;

    @Value("${security.turnstile.expected-action}")
    private String expectedAction;

    public TurnstileService(
            @Qualifier("turnstileRestTemplate")
            RestTemplate restTemplate) {

        this.restTemplate = restTemplate;
    }

    public void verifyOrThrow(String token) {

        if (token == null ||
                token.isBlank() ||
                token.length() > 2048) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "TURNSTILE_TOKEN_INVALID"
            );
        }

        Map<String, String> payload = new HashMap<>();

        payload.put("secret", secretKey);
        payload.put("response", token);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> entity =
                new HttpEntity<>(payload, headers);

        TurnstileVerifyResponse result;

        try {

            ResponseEntity<TurnstileVerifyResponse> response =
                    restTemplate.postForEntity(
                            VERIFY_URL,
                            entity,
                            TurnstileVerifyResponse.class
                    );

            result = response.getBody();

        } catch (RestClientException ex) {

            log.error(
                    "Cloudflare Turnstile Siteverify unavailable",
                    ex
            );

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "TURNSTILE_SERVICE_UNAVAILABLE"
            );
        }

        if (result == null || !result.isSuccess()) {

            log.warn(
                    "Turnstile verification failed. errors={}",
                    result == null
                            ? "empty response"
                            : result.getErrorCodes()
            );

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "TURNSTILE_VERIFICATION_FAILED"
            );
        }

        if (!expectedHostname.equals(result.getHostname())) {

            log.warn(
                    "Turnstile hostname mismatch: {}",
                    result.getHostname()
            );

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "TURNSTILE_HOSTNAME_INVALID"
            );
        }

        if (!expectedAction.equals(result.getAction())) {

            log.warn(
                    "Turnstile action mismatch: {}",
                    result.getAction()
            );

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "TURNSTILE_ACTION_INVALID"
            );
        }

        log.debug("Turnstile verification successful");
    }
}