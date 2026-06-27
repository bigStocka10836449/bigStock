package com.bigstock.schedule.controller;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bigstock.schedule.service.SecuritiesFirmsRankPrecomputeService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/securities-firms/rank/precompute")
@RequiredArgsConstructor
public class SecuritiesFirmsRankManualController {

    private final SecuritiesFirmsRankPrecomputeService precomputeService;

    @PostMapping("/manual")
    public ResponseEntity<Map<String, Object>> triggerManualPrecompute() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("triggerSource", "MANUAL_API");
        response.put("submittedAt", LocalDateTime.now().toString());

        try {
            precomputeService.precomputeAsync("MANUAL_API");
            response.put("status", "ACCEPTED");
            response.put("message", "Securities firms rank precompute has been submitted asynchronously.");
            return ResponseEntity.accepted().body(response);
        } catch (Exception e) {
            response.put("status", "BUSY");
            response.put("message", "Securities firms rank precompute is already running or queued.");
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
        }
    }
}
