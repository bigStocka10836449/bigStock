package com.bigstock.schedule.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.bigstock.schedule.utils.ChromeDriverUtils;
import com.bigstock.sharedComponent.entity.StockDayPrice;
import com.bigstock.sharedComponent.service.StockDayPriceService;
import com.bigstock.sharedComponent.service.StockInfoService;
import com.google.common.collect.Maps;

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
		Map<String,Boolean> innerRecordMap = Maps.newHashMap();
		if ("0".equals(stockType)) {
			if (manualGrapRangeHistoryStockPriceTPEXFlag && enableflag) {
				return;
			}
			stockInfoService.getStockCodeByStockType("0").stream().sorted(Collections.reverseOrder()).forEach(stockCode -> {
				List<StockDayPrice> stockDayPrices;
				try {
					if (manualGrapRangeHistoryStockPriceTPEXFlag && !enableflag) {
						manualGrapRangeHistoryStockPriceTPEXFlag = false;
						throw new Exception("manualGrapRangeHistoryStockPriceTPEX stop");
					}
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
					manualGrapRangeHistoryStockPriceTPEXFlag = true;
//					if (stockDayPriceService.findByStockCodeAndStartDateAndEndDate(stockCode,
//							sdf.format(Date
//									.from(searchStartDate.atStartOfDay(ZoneId.systemDefault()).toInstant())),
//							sdf.format(
//									Date.from(searchEndDate.atStartOfDay(ZoneId.systemDefault()).toInstant())))
//							.size() > 70 || innerRecordMap.containsKey(stockCode)) {
//						return;
//					}
					Integer needCalculate = stockDayPriceService.checkIsTradingDateRangeContaineNotCalculate(stockCode,Date
							.from(searchStartDate.atStartOfDay(ZoneId.systemDefault()).toInstant()),
							Date.from(searchEndDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));
					if(!Integer.valueOf("1").equals(needCalculate)) {
						return;
					}
					log.info("sync stockCode: {} , startDate : {} , endDate: {}", stockCode, searchStartDate,
							searchEndDate);
					stockDayPrices = ChromeDriverUtils.getTpexStockHistory(
							Date.from(searchStartDate.atStartOfDay(ZoneId.systemDefault()).toInstant()),
							Date.from(searchEndDate.atStartOfDay(ZoneId.systemDefault()).toInstant()),
							manualDateRangeTpexBaseurl, stockCode);
					stockDayPrices.stream().forEach(stockDayPrice ->{
						calculateRSVValueAndLimitDownUp(stockDayPrice);
						stockDayPriceService.save(stockDayPrice);
					});
					Thread.sleep(8000);
				} catch (Exception e) {
					throw new RuntimeException(e);
				} finally {
					innerRecordMap.put(stockCode, true);
				}
			});
			log.info("finsh TPEX StockCodePrice sync, startDate : {} , endDate: {}", searchStartDate, searchEndDate);
		} else {
			if (manualGrapRangeHistoryStockPriceTESEFlag && enableflag) {
				return;
			}
			stockInfoService.getStockCodeByStockType("1") .stream().sorted(Collections.reverseOrder()).forEach(stockCode -> {
				List<StockDayPrice> stockDayPrices;
				try {
					if (manualGrapRangeHistoryStockPriceTESEFlag && !enableflag) {
						manualGrapRangeHistoryStockPriceTESEFlag = false;
						throw new Exception("manualGrapRangeHistoryStockPriceTESE stop");
					}
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
					manualGrapRangeHistoryStockPriceTESEFlag = true;
//					if (stockDayPriceService
//							.findByStockCodeAndStartDateAndEndDate(stockCode,
//									sdf.format(Date
//											.from(searchStartDate.atStartOfDay(ZoneId.systemDefault()).toInstant())),
//									sdf.format(
//											Date.from(searchEndDate.atStartOfDay(ZoneId.systemDefault()).toInstant())))
//							.size() > 70 || innerRecordMap.containsKey(stockCode)) {
//						return;
//					}
					Integer needCalculate = stockDayPriceService.checkIsTradingDateRangeContaineNotCalculate(stockCode,Date
							.from(searchStartDate.atStartOfDay(ZoneId.systemDefault()).toInstant()),
							Date.from(searchEndDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));
					if(!Integer.valueOf("1").equals(needCalculate)) {
						return;
					}
					log.info("sync stockCode: {} , startDate : {} , endDate: {}", stockCode, searchStartDate,
							searchEndDate);
					stockDayPrices = ChromeDriverUtils.getTwseStockHistory(
							Date.from(searchStartDate.atStartOfDay(ZoneId.systemDefault()).toInstant()),
							Date.from(searchEndDate.atStartOfDay(ZoneId.systemDefault()).toInstant()),
							manualDateRangeTwseBaseurl, stockCode);
					stockDayPrices.stream().forEach(stockDayPrice ->{
						calculateRSVValueAndLimitDownUp(stockDayPrice);
						stockDayPriceService.save(stockDayPrice);
					});
					Thread.sleep(8000);
				} catch (Exception e) {
					log.warn(e.getMessage(), e);
				} finally {
					innerRecordMap.put(stockCode, true);
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
	
	public void calculateRSVValueAndLimitDownUp(StockDayPrice stockTwseDayPrice){
		if(stockTwseDayPrice.getClosingPrice().equals("---") || stockTwseDayPrice.getClosingPrice().equals("----") || stockTwseDayPrice.getClosingPrice().equals("--")  ) {
			return;
		}
		List<StockDayPrice> twoFourtyStockDayPrices = stockDayPriceService
				.findByStockCodeAndTradingDayBeforEqualLimitTwoFourty(stockTwseDayPrice.getStockCode(),
						stockTwseDayPrice.getTradingDay());
		if (twoFourtyStockDayPrices.size() < 9) {
			return; // 如果不满足条件，返回 null
		}

		int period = 9;
		List<Double> closingPrices = twoFourtyStockDayPrices.stream().map(innerTwelfthStockDayPrice -> Double
				.valueOf(innerTwelfthStockDayPrice.getClosingPrice().replaceAll(",", ""))).toList();
		List<Double> highPrices = twoFourtyStockDayPrices.stream().map(innerTwelfthStockDayPrice -> Double
				.valueOf(innerTwelfthStockDayPrice.getHighPrice().replaceAll(",", ""))).toList();
		List<Double> lowPrices = twoFourtyStockDayPrices.stream().map(innerTwelfthStockDayPrice -> Double
				.valueOf(innerTwelfthStockDayPrice.getLowPrice().replaceAll(",", ""))).toList();

		double highestHigh = Double.MIN_VALUE;
		double lowestLow = Double.MAX_VALUE;

		for (int i = 0; i < period; i++) {
			highestHigh = Math.max(highestHigh, highPrices.get(i));
			lowestLow = Math.min(lowestLow, lowPrices.get(i));
		}

		double latestClosingPrice = closingPrices.get(0);
		Double rsv = (latestClosingPrice - lowestLow) / (highestHigh - lowestLow) * 100.0;
		rsv = roundToThreeDecimalPlaces(rsv);
		Double previousK =  StringUtils.isNotBlank( twoFourtyStockDayPrices.get(1).getLineKvalue())  ? Double.valueOf(twoFourtyStockDayPrices.get(1).getLineKvalue())  : 50; // Default initial K value
		Double previousD = StringUtils.isNotBlank( twoFourtyStockDayPrices.get(1).getLineDvalue())  ? Double.valueOf(twoFourtyStockDayPrices.get(1).getLineDvalue())  : 50;// Default initial D value
		double smoothingFactor = 1.0 / 3.0;

		Double k = previousK * (1 - smoothingFactor) + rsv * smoothingFactor;
		k = roundToThreeDecimalPlaces(k);
		Double d = previousD * (1 - smoothingFactor) + k * smoothingFactor;
		d =  roundToThreeDecimalPlaces(d);
		
		stockTwseDayPrice.setLineDvalue(d.toString());
		stockTwseDayPrice.setLineKvalue(k.toString());
		stockTwseDayPrice.setLineRSVvalue(rsv.toString());
		if (stockTwseDayPrice.getClosingPrice().contains("-") || stockTwseDayPrice.getChange().contains("X")) {
			return;
		}
		if (StringUtils.isBlank(stockTwseDayPrice.getClosingPrice())) {
			return;
		}
		String chage = stockTwseDayPrice.getChange().trim().replace("+", "").replaceAll(",", "");
		if (chage.equals("0.00")) {
			chage = "0";
		}
		if (chage.equals("除息") || chage.equals("除權")) {
			Optional<StockDayPrice> stockDayPriceOp = stockDayPriceService
					.findByStockCodeAndTradingDayBeforLimitOne(stockTwseDayPrice.getStockCode(),
							stockTwseDayPrice.getTradingDay());
			if (stockDayPriceOp.isPresent()) {
				chage = String.valueOf(Double
						.valueOf(stockTwseDayPrice.getClosingPrice().replace("+", "").replaceAll(",", ""))
						- Double.valueOf(
								stockDayPriceOp.get().getClosingPrice().replace("+", "").replaceAll(",", "")));
			} else {
				chage = "0";
			}
		}
		Double standarPrice = Double
				.valueOf(stockTwseDayPrice.getClosingPrice().replace("+", "").replaceAll(",", ""))
				- new BigDecimal(chage).doubleValue();
		Double upperLimitPrice = ChromeDriverUtils.calculateLimitPrice(standarPrice, true);
		Double lowerLimitPrice = ChromeDriverUtils.calculateLimitPrice(standarPrice, false);
		stockTwseDayPrice.setLimitDown(lowerLimitPrice.toString());
		stockTwseDayPrice.setLimitUp(upperLimitPrice.toString());
		Double closingPrice = Double
				.valueOf(stockTwseDayPrice.getClosingPrice().replace("+", "").replaceAll(",", ""));
		stockTwseDayPrice.setLimitDown(lowerLimitPrice.toString());
		stockTwseDayPrice.setLimitUp(upperLimitPrice.toString());
		Double rate = ((closingPrice - standarPrice) / standarPrice) * 100;
		rate = Math.round(rate * 100.0) / 100.0;
		stockTwseDayPrice.setChangeRate(rate);
		
		// 新增計算移動平均線 (MA) 的邏輯
	    int[] maPeriods = {240, 120, 60, 20, 10, 5}; // 定義需要計算的移動平均線週期
	    for (int maPeriod : maPeriods) {
	        Double maValue = calculateMovingAverage(twoFourtyStockDayPrices, maPeriod);
	        switch (maPeriod) {
	            case 240:
	                stockTwseDayPrice.setTwoFourtyDaysMa(maValue.toString()); // 設置 240 日 MA
	                break;
	            case 120:
	                stockTwseDayPrice.setOneTwentyDaysMa(maValue.toString()); // 設置 120 日 MA
	                break;
	            case 60:
	                stockTwseDayPrice.setSixtyDaysMa(maValue.toString()); // 設置 60 日 MA
	                break;
	            case 20:
	                stockTwseDayPrice.setTwentyDaysMa(maValue.toString()); // 設置 20 日 MA
	                break;
	            case 10:
	                stockTwseDayPrice.setTenDaysMa(maValue.toString()); // 設置 10 日 MA
	                break;
	            case 5:
	                stockTwseDayPrice.setFiveDaysMa(maValue.toString()); // 設置 5 日 MA
	                break;
	        }
	    }
	}
	
	private double calculateMovingAverage(List<StockDayPrice> stockDayPrices, int period) {
	    if (stockDayPrices.size() < period) {
	        return 0.0; // 如果資料不足，返回 0
	    }
	    double average = stockDayPrices.stream()
	            .limit(period)
	            .mapToDouble(stockDayPrice -> Double.valueOf(stockDayPrice.getClosingPrice().replaceAll(",", "")))
	            .average()
	            .orElse(0.0);

	    // 使用 BigDecimal 保留小數點第 4 位（四捨五入）
	    return new BigDecimal(average)
	            .setScale(4, RoundingMode.HALF_UP)
	            .doubleValue();
	}
	
	private double roundToThreeDecimalPlaces(double value) {
	    return new BigDecimal(value)
	            .setScale(3, RoundingMode.HALF_UP)
	            .doubleValue();
	}
}
