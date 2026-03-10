package com.bigstock.biz.schedule;

import java.net.URI;
import java.util.List;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
//@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class GrapSecuritiesFirmsDayOperate {

    private final RestTemplate restTemplate = new RestTemplate();
	
//	@Scheduled(cron = "0 30 17 * * ?", zone = "Asia/Taipei")
	public void callWindowsToExcecute() {
		 HttpHeaders headers = new HttpHeaders();
		    headers.set("User-Agent", "Mozilla/5.0");

		    HttpEntity<Void> entity = new HttpEntity<>(headers);

		    List<String> urls = List.of(
		            "http://bigstock-windows0.zeabur.internal:8080/run-job",
		            "http://bigstock-windows1-bile.zeabur.internal:8080/run-job"
		    );

		    for (String url : urls) {

		        try {

		            restTemplate.exchange(
		                    URI.create(url),
		                    HttpMethod.POST,
		                    entity,
		                    String.class
		            );

		        } catch (Exception e) {

		            log.error("Failed to call {}", url, e);

		        }
		    }
	}
}
