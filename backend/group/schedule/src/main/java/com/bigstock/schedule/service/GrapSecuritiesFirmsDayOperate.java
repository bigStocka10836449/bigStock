package com.bigstock.schedule.service;

import java.net.URI;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.bigstock.sharedComponent.service.SecuritiesFirmsDayOperateService;
import com.bigstock.sharedComponent.service.StockDayPriceService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
//@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class GrapSecuritiesFirmsDayOperate {

	private final RestTemplate restTemplate = new RestTemplate();

	private final StockDayPriceService stockDayPriceService;

	private final SecuritiesFirmsDayOperateService securitiesFirmsDayOperateService;
	@PostConstruct
	@Scheduled(cron = "0 30 17 * * ?", zone = "Asia/Taipei")
	public void callWindowsToExcecute() {
		Date currentTradeDate = stockDayPriceService.getCurrentTradeDate();
		boolean isfinished = securitiesFirmsDayOperateService.chechIsFinishedWithTradingDate("2330", currentTradeDate);
		//最新的交易日期沒有買賣日報表訊息的話才執行抓取寫入
		if (!isfinished) {
			HttpHeaders headers = new HttpHeaders();
			headers.set("User-Agent", "Mozilla/5.0");

			HttpEntity<Void> entity = new HttpEntity<>(headers);

			List<String> urls = List.of("http://bigstock-windows0.zeabur.internal:8080/run-job",
					"http://bigstock-windows1-bile.zeabur.internal:8080/run-job");

			for (String url : urls) {

				try {

					restTemplate.exchange(URI.create(url), HttpMethod.POST, entity, String.class);

				} catch (Exception e) {

					log.error("Failed to call {}", url, e);

				}
			}
		}
	}
}
