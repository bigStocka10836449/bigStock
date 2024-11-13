package com.bigstock.biz.service;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;

import com.bigstock.sharedComponent.dto.SingleStockPriceVo;
import com.bigstock.sharedComponent.dto.StructureContinueIncreaseVo;
import com.bigstock.sharedComponent.entity.ShareholderStructure;
import com.bigstock.sharedComponent.entity.StockDayPrice;
import com.bigstock.sharedComponent.entity.StockExchangeDetail;
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

	public List<SingleStockPriceVo> getSingleStockPrices(String stockCode, Date startDate, Date endDate){
		List<StockDayPrice> stockDayPrices = stockDayPriceService.findByStockCodeAndStartDateAndEndDate(stockCode, startDate, endDate);
		return stockDayPrices.stream().map(stockDayPrice ->{
			String highPrice = stockDayPrice.getHighPrice() ;
			String lowPrice = stockDayPrice.getLowPrice();
			String openingPrice = stockDayPrice.getOpeningPrice();
			String closingPrice = stockDayPrice.getClosingPrice();
			SimpleDateFormat sfd = new SimpleDateFormat("yyyy-MM-dd");
			SingleStockPriceVo vo = new SingleStockPriceVo();
			vo.setClosingPrice(closingPrice);
			vo.setOpeningPrice(openingPrice);
			vo.setHighPrice(highPrice);
			vo.setLowPrice(lowPrice);
			vo.setTradingDate(sfd.format(stockDayPrice.getTradingDay()));
			vo.setStockCode(stockCode);
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
