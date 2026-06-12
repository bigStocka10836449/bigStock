package com.bigstock.biz.schedule;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Service;

import com.bigstock.biz.service.StockBasicInfoDbService;
import com.bigstock.sharedComponent.service.StockInfoService;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
// LOCAL_RANK_TEST_DISABLED: @EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class GrabBasicInfo {

	private final StockInfoService stockInfoService;

	private final StockBasicInfoDbService stockBasicInfoDbService;
//	@PostConstruct
	public void updateStockDayPriceByThirdParty() throws Exception {
		stockInfoService.getStockCodeByStockType("0").stream().filter(data -> {
			return !data.matches(".*[a-zA-Z].*");
		}).forEach(data -> {
			stockBasicInfoDbService.syncToDb(data, null, false, null);
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		});
		stockInfoService.getStockCodeByStockType("1").stream().filter(data -> {
			return !data.matches(".*[a-zA-Z].*");
		}).forEach(data -> {
			stockBasicInfoDbService.syncToDb(data, null, false, null);
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		});
		log.info("GrabBasicInfo done");
	}
}
