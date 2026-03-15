package com.bigstock.schedule.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.bigstock.sharedComponent.service.StockDayPriceService;
import com.bigstock.sharedComponent.service.StockInfoService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StockDayPriceWeeklyScheduleService {

	private final StockInfoService stockInfoService;
	
	private final StockDayPriceService stockDayPriceService;
	
	public void init() {

		List<String> tpexStockCodes = stockInfoService.getStockCodeByStockType("0");
		List<String> teseStockCodes = stockInfoService.getStockCodeByStockType("1");
		tpexStockCodes.stream().forEach(data ->{
//			stockDayPriceService.f
		});
	}
}
