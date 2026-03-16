package com.bigstock.schedule.service;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@EnableScheduling
@RequiredArgsConstructor
@Slf4j

public class GrabUSMarketHistory {

	
	@Scheduled(cron = "0 30 12 * * ?", zone = "Asia/Taipei")
	public void grabUSHistory() {
		
	}
}
