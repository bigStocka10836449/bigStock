package com.bigstock.biz.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.bigstock.sharedComponent.dto.DynamicFilterStockCodeVo;
import com.bigstock.sharedComponent.dto.SingleStockPriceVo;
import com.bigstock.sharedComponent.dto.StructureContinueIncreaseVo;
import com.bigstock.sharedComponent.entity.MarginTradingAndShortSellingInfo;
import com.bigstock.sharedComponent.entity.ShareholderStructure;
import com.bigstock.sharedComponent.entity.StockDayPrice;
import com.bigstock.sharedComponent.entity.StockExchangeDetail;
import com.bigstock.sharedComponent.service.MarginTradingAndShortSellingInfoService;
import com.bigstock.sharedComponent.service.ShareholderStructureService;
import com.bigstock.sharedComponent.service.StockDayPriceService;
import com.bigstock.sharedComponent.service.StockExchangeDetailService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BizService {

	private final ShareholderStructureService shareholderStructureService;

	private final StockDayPriceService stockDayPriceService;
	
	private final StockExchangeDetailService stockExchangeDetailService;
	
	private final MarginTradingAndShortSellingInfoService marginTradingAndShortSellingInfoService;

	public List<StockExchangeDetail> getStockExchangeDetail(String stockCode, Date tradeDate){
		return stockExchangeDetailService.findByStockCodeAndTradingDateOrderBySeqAsc(stockCode, tradeDate);
	}
	
	public List<ShareholderStructure> getStockShareholderStructure(String stockCode, int limit) {
		 List<ShareholderStructure> shareholderStructures = shareholderStructureService.getShareholderStructureByStockCodeDesc(stockCode);
		 if(shareholderStructures.size() > 52) {
			 return shareholderStructures.subList(0, limit);
		 } else {
			 return shareholderStructures;
		 }
	}
	
	
	public List<MarginTradingAndShortSellingInfo> getStockMarginTradingAndShortSelling(String stockCode){
		List<StockDayPrice> stockDayPrices = stockDayPriceService.findPreviousFiftyTowDaysBeforeLastestDayInfo("2330");
		Date lastTradingDay = stockDayPrices.stream().findFirst().get().getTradingDay();
		Date firstTradingDay = stockDayPrices.stream().sorted(Comparator.comparing(StockDayPrice::getTradingDay).reversed()).findFirst().get().getTradingDay();
		List<MarginTradingAndShortSellingInfo> marginTradingAndShortSellingInfos = marginTradingAndShortSellingInfoService.findMarginTradingAndShortSellingInfoByDateRange(stockCode, lastTradingDay, firstTradingDay);
		Set<Date> marginTradingDaysSet = marginTradingAndShortSellingInfos.stream()
		        .map(MarginTradingAndShortSellingInfo::getTradingDay)
		        .collect(Collectors.toSet());

		List<MarginTradingAndShortSellingInfo> newInfos = stockDayPrices.stream()
		        .filter(stockDayPrice -> !marginTradingDaysSet.contains(stockDayPrice.getTradingDay()))
		        .map(stockDayPrice -> {
		            MarginTradingAndShortSellingInfo newInfo = new MarginTradingAndShortSellingInfo();
		            newInfo.setTradingDay(stockDayPrice.getTradingDay());
		            return newInfo;
		        })
		        .collect(Collectors.toList());

		if(CollectionUtils.isNotEmpty(newInfos)) {
			marginTradingAndShortSellingInfos.addAll(newInfos);
			marginTradingAndShortSellingInfos.sort(Comparator.comparing(MarginTradingAndShortSellingInfo::getTradingDay).reversed());
		}
		return marginTradingAndShortSellingInfos;
	}

	public List<SingleStockPriceVo> getSingleStockPrices(String stockCode, Date startDate, Date endDate) throws ParseException{
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		List<StockDayPrice> stockDayPrices = stockDayPriceService.findByStockCodeAndStartDateAndEndDateCache(stockCode, sdf.format(startDate), sdf.format(endDate));
		return stockDayPrices.stream().sorted(Comparator.comparing(StockDayPrice::getTradingDay).reversed()).map(stockDayPrice ->{
			String highPrice = stockDayPrice.getHighPrice() ;
			String lowPrice = stockDayPrice.getLowPrice();
			String openingPrice = stockDayPrice.getOpeningPrice();
			String closingPrice = stockDayPrice.getClosingPrice();
			SimpleDateFormat sfd = new SimpleDateFormat("yyyy-MM-dd");
			SingleStockPriceVo vo = new SingleStockPriceVo();
			vo.setClosingPrice(closingPrice.replaceAll(",", ""));
			vo.setOpeningPrice(openingPrice.replaceAll(",", ""));
			vo.setHighPrice(highPrice.replaceAll(",", ""));
			vo.setLowPrice(lowPrice.replaceAll(",", ""));
			vo.setTradingDate(sfd.format(stockDayPrice.getTradingDay()));
			vo.setStockCode(stockCode);
			String tradingVolume = stockDayPrice.getTradingVolume();
			if(StringUtils.isNotBlank(tradingVolume)) {
				new BigDecimal(tradingVolume).divide(new BigDecimal("1000"), 0, RoundingMode.HALF_UP);
				vo.setTradingVolume(stockDayPrice.getTradingVolume());
			}
			return vo;
		}).toList();
	}
	
	public SingleStockPriceVo getSingleStockPrice(String stockCode, Date searchDate) {
		Optional<StockDayPrice> stockDayPriceOp = stockDayPriceService.findByStockCodeAndTradingDate(stockCode,
				searchDate);
		String highPrice = stockDayPriceOp.isPresent() ? stockDayPriceOp.get().getHighPrice() : "0.0";
		String lowPrice = stockDayPriceOp.isPresent() ? stockDayPriceOp.get().getLowPrice() : "0.0";
		String openingPrice = stockDayPriceOp.isPresent() ? stockDayPriceOp.get().getOpeningPrice() : "0.0";
		String closingPrice = stockDayPriceOp.isPresent() ? stockDayPriceOp.get().getClosingPrice() : "0.0";

		SingleStockPriceVo vo = new SingleStockPriceVo();
		vo.setClosingPrice(closingPrice);
		vo.setOpeningPrice(openingPrice);
		vo.setHighPrice(highPrice);
		vo.setLowPrice(lowPrice);
		vo.setStockCode(stockCode);
		return vo;
	}

	public List<StructureContinueIncreaseVo> getShareholderStructureContinueIncreaseLastTowWeeks() {
		LocalDate today = LocalDate.now();
		AtomicInteger weekOfYears = new AtomicInteger(today.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));

		String firstWeekOfYear = findMaxWeek(today, weekOfYears);
		 weekOfYears.addAndGet(-1);
		String secondMaxWeekOfYear = findMaxWeek(today, weekOfYears);
		 weekOfYears.addAndGet(-1);
		String thirdMaxWeekOfYear = findMaxWeek(today, weekOfYears);
		return shareholderStructureService
				.getShareholderStructureLastTwoWeeks(firstWeekOfYear, secondMaxWeekOfYear, thirdMaxWeekOfYear).stream()
				.map(shareholderStructure -> {
					StructureContinueIncreaseVo vo = new StructureContinueIncreaseVo();
					vo.setStockCode(shareholderStructure.getStockCode());
					vo.setStockName(shareholderStructure.getStockName());
					vo.setWeekOfYear(shareholderStructure.getWeekOfYear());
					vo.setStockTotal(shareholderStructure.getStockTotal());
					vo.setBetweenFourHundredAndSixHundredBoardLot(
							shareholderStructure.getBetweenFourHundredAndSixHundredBoardLot());
					vo.setBetweenSixHundredAndEightHundredBoardLot(
							shareholderStructure.getBetweenSixHundredAndEightHundredBoardLot());
					vo.setBetweenEightHundredAndOneThousandBoardLot(
							shareholderStructure.getBetweenEightHundredAndOneThousandBoardLot());
					vo.setOverOneThousandBoardLot(shareholderStructure.getOverOneThousandBoardLot());
					return vo;
				}).toList();
	}

	public List<String> getMatchStockCodeByCondition(DynamicFilterStockCodeVo dynamicFilterStockCodeVo){
		stockDayPriceService.
	}
	
	private String findMaxWeek(LocalDate today, AtomicInteger weekOfYear) {
		boolean checkResult = false;
		while (!checkResult) {
			String week = String.valueOf(today.getYear()) + "W" + weekOfYear.get();
			checkResult = shareholderStructureService.checkWeekExist(week);
			if(!checkResult) {
				weekOfYear.addAndGet(-1);
			}
			if ( weekOfYear.get() == 0) {
				today = today.minusYears(1).withMonth(12).withDayOfMonth(31);
				weekOfYear = new AtomicInteger(today.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
			}
		}
		return String.valueOf(today.getYear()) + "W" + weekOfYear;
	}
}
