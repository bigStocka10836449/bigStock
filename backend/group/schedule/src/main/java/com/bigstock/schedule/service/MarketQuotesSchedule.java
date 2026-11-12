package com.bigstock.schedule.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import com.bigstock.sharedComponent.service.MarketQuotesJobService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarketQuotesSchedule {

    private final MarketQuotesJobService job;

    @Scheduled(cron = "0 0/15 9-14 * * *", zone = "Asia/Taipei")
    @Scheduled(cron = "0 0 15 * * *", zone = "Asia/Taipei")
    public void scheduleQuotes() {
        if (!job.isConfigured()) {
            return;
        }
        try {
            log.info("Market quote schedule result: {}", job.runScheduled());
        } catch (RestClientException ex) {
            log.warn("Market quote worker request failed: {}", ex.getClass().getSimpleName());
        }
    }
}
