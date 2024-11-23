package com.bigstock.schedule.service;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.bigstock.schedule.utils.ChromeDriverUtils;
import com.bigstock.sharedComponent.entity.StockDayPrice;
import com.bigstock.sharedComponent.service.StockDayPriceService;
import com.bigstock.sharedComponent.service.StockInfoService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class GraspHistoryStockPrice {

	@Value("${schedule.manual-date-range.tpex-baseurl}")
	private String manualDateRangeTpexBaseurl;

	@Value("${schedule.manual-date-range.twse-baseurl}")
	private String manualDateRangeTwseBaseurl;

	@Value("${schedule.manual-date-range.search-start-date}")
	private LocalDate searchStartDate;

	@Value("${schedule.manual-date-range.search-end-date}")
	private LocalDate searchEndDate;

	private final StockDayPriceService stockDayPriceService;

	private final StockInfoService stockInfoService;

	private static boolean manualGrapRangeHistoryStockPriceTPEXFlag = false;

	private static boolean manualGrapRangeHistoryStockPriceTESEFlag = false;

	public void manualGrapRangeHistoryStockPrice(String stockType, boolean enableflag) {

		if ("0".equals(stockType)) {
			if (manualGrapRangeHistoryStockPriceTPEXFlag && enableflag) {
				return;
			}
			stockInfoService.getStockCodeByStockType("0").stream().forEach(stockCode -> {
				List<StockDayPrice> stockDayPrices;
				try {
					if (manualGrapRangeHistoryStockPriceTPEXFlag && !enableflag) {
						manualGrapRangeHistoryStockPriceTPEXFlag = false;
						throw new Exception("manualGrapRangeHistoryStockPriceTPEX stop");
					}
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
					manualGrapRangeHistoryStockPriceTPEXFlag = true;
					if (stockDayPriceService.findByStockCodeAndStartDateAndEndDate(stockCode,
							sdf.format(Date
									.from(searchStartDate.atStartOfDay(ZoneId.systemDefault()).toInstant())),
							sdf.format(
									Date.from(searchEndDate.atStartOfDay(ZoneId.systemDefault()).toInstant())))
							.size() > 40) {
						return;
					}
					log.info("sync stockCode: {} , startDate : {} , endDate: {}", stockCode, searchStartDate,
							searchEndDate);
					stockDayPrices = ChromeDriverUtils.getTpexStockHistory(
							Date.from(searchStartDate.atStartOfDay(ZoneId.systemDefault()).toInstant()),
							Date.from(searchEndDate.atStartOfDay(ZoneId.systemDefault()).toInstant()),
							manualDateRangeTpexBaseurl, stockCode);
					stockDayPriceService.saveAll(stockDayPrices);
					Thread.sleep(8000);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});
			log.info("finsh TPEX StockCodePrice sync, startDate : {} , endDate: {}", searchStartDate, searchEndDate);
		} else {
			if (manualGrapRangeHistoryStockPriceTESEFlag && enableflag) {
				return;
			}
			stockInfoService.getStockCodeByStockType("1").stream().forEach(stockCode -> {
				List<StockDayPrice> stockDayPrices;
				try {
					if (manualGrapRangeHistoryStockPriceTESEFlag && !enableflag) {
						manualGrapRangeHistoryStockPriceTESEFlag = false;
						throw new Exception("manualGrapRangeHistoryStockPriceTESE stop");
					}
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
					manualGrapRangeHistoryStockPriceTESEFlag = true;
					if (stockDayPriceService
							.findByStockCodeAndStartDateAndEndDate(stockCode,
									sdf.format(Date
											.from(searchStartDate.atStartOfDay(ZoneId.systemDefault()).toInstant())),
									sdf.format(
											Date.from(searchEndDate.atStartOfDay(ZoneId.systemDefault()).toInstant())))
							.size() > 40) {
						return;
					}
					log.info("sync stockCode: {} , startDate : {} , endDate: {}", stockCode, searchStartDate,
							searchEndDate);
					stockDayPrices = ChromeDriverUtils.getTwseStockHistory(
							Date.from(searchStartDate.atStartOfDay(ZoneId.systemDefault()).toInstant()),
							Date.from(searchEndDate.atStartOfDay(ZoneId.systemDefault()).toInstant()),
							manualDateRangeTwseBaseurl, stockCode);
					stockDayPriceService.saveAll(stockDayPrices);
					Thread.sleep(8000);
				} catch (Exception e) {
					log.warn(e.getMessage(), e);
				}
			});
			log.info("finsh TWSE StockCodePrice sync, startDate : {} , endDate: {}", searchStartDate, searchEndDate);
		}
	}

	public Date getLastTradeDate() {
		Date currentDate = Calendar.getInstance().getTime();
		return stockInfoService.getStockCodeByStockType("0").stream().map(stockCode -> {
			try {
				return ChromeDriverUtils
						.getTpexStockHistory(currentDate, currentDate, manualDateRangeTpexBaseurl, stockCode).stream()
						.map(stockDayPrice -> stockDayPrice.getTradingDay()).toList();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}).filter(tradingDates -> CollectionUtils.isNotEmpty(tradingDates)).flatMap(List::stream)
				.reduce((first, second) -> second).orElseThrow(() -> new RuntimeException("找不到最新的交易日期"));
	}
}
