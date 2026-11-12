package com.bigstock.biz.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import com.bigstock.biz.service.MarketQuotesService;
import com.bigstock.biz.service.MarketQuotesService.QuoteBatch;
import com.bigstock.sharedComponent.service.MarketQuotesJobService;
import com.fasterxml.jackson.core.JsonProcessingException;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class MarketQuotesController {

    public static final String IMPORT_PATH = "/biz/marketQuotesImport/{source}";
    private final MarketQuotesService service;
    private final MarketQuotesJobService job;

    @PostMapping(IMPORT_PATH)
    public ResponseEntity<Void> importQuotes(@PathVariable("source") String source, @RequestBody QuoteBatch request)
            throws JsonProcessingException {
        service.importQuotes(source, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/admin/marketQuotes/control")
    public Object control(@RequestBody ControlRequest request) {
        if ("PAUSE_TODAY".equals(request.action())) {
            return Map.of("status", "PAUSED_TODAY", "pausedUntil", job.pauseToday());
        }
        if ("RUN_NOW".equals(request.action())) {
            try {
                return job.runNow();
            } catch (RestClientException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Market quote worker request failed", ex);
            }
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Expected RUN_NOW or PAUSE_TODAY");
    }

    public record ControlRequest(String action) {}
}
