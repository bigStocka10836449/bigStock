package com.bigstock.biz.utils;

import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpStatusCodeException;

public class HttpDownloadUtils {

    private static final RestTemplate restTemplate = new RestTemplate();

    private HttpDownloadUtils() {}

    public static byte[] downloadBytes(String url) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.USER_AGENT, "Mozilla/5.0");
            HttpEntity<Void> req = new HttpEntity<>(headers);

            ResponseEntity<byte[]> resp = restTemplate.exchange(
                    url, HttpMethod.GET, req, byte[].class
            );

            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                throw new RuntimeException("HTTP download failed: " + resp.getStatusCode());
            }
            return resp.getBody();
        } catch (HttpStatusCodeException e) {
            throw new RuntimeException("HTTP download failed: " + e.getStatusCode() + " " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new RuntimeException("HTTP download failed: " + e.getMessage(), e);
        }
    }
}
