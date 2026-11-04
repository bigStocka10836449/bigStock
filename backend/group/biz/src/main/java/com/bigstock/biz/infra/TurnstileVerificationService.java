package com.bigstock.biz.infra;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.annotation.JsonProperty;

@Service
public class TurnstileVerificationService {
	private static final String URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify";

	@Value("${security.turnstile.secret-key}")
	private String secretKey;
	@Value("${security.turnstile.expected-hostname:localhost}")
	private String hostname;
	@Value("${security.turnstile.expected-action:fcm_challenge}")
	private String action;

	private final RestTemplate client;

	public TurnstileVerificationService(@Value("${security.turnstile.connect-timeout:3000}") int connectTimeout,
			@Value("${security.turnstile.read-timeout:5000}") int readTimeout) {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(connectTimeout);
		factory.setReadTimeout(readTimeout);
		this.client = new RestTemplate(factory);
	}

	public void verify(String token) {
		if (token == null || token.isBlank() || token.length() > 2048) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Turnstile validation failed");
		}
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("secret", secretKey);
		form.add("response", token);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		ResponseEntity<SiteverifyResponse> response;
		try {
			response = client.postForEntity(URL, new HttpEntity<>(form, headers), SiteverifyResponse.class);
		} catch (Exception ex) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Turnstile verification unavailable");
		}
		SiteverifyResponse body = response.getBody();
		if (body == null || !body.success() || !hostname.equals(body.hostname()) || !action.equals(body.action())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Turnstile validation failed");
		}
	}

	public record SiteverifyResponse(boolean success, String hostname, String action,
			@JsonProperty("error-codes") List<String> errorCodes) {
	}
}
