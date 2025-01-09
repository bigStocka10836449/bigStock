package com.bigstock.biz.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.bigstock.biz.domain.StockDayPriceNativeQueryService;
import com.bigstock.sharedComponent.dto.DynamicFilterStockCodeVo;
import com.bigstock.sharedComponent.dto.DynamicFilterStockPriceCondition;
import com.bigstock.sharedComponent.dto.SingleStockDayPriceVo;
import com.bigstock.sharedComponent.dto.SingleStockMonthPriceVo;
import com.bigstock.sharedComponent.dto.SingleStockPriceVo;
import com.bigstock.sharedComponent.dto.SingleStockWeekPriceVo;
import com.bigstock.sharedComponent.dto.StockInfoVo;
import com.bigstock.sharedComponent.dto.StructureContinueIncreaseVo;
import com.bigstock.sharedComponent.entity.MarginTradingAndShortSellingInfo;
import com.bigstock.sharedComponent.entity.ShareholderStructure;
import com.bigstock.sharedComponent.entity.StockDayPrice;
import com.bigstock.sharedComponent.entity.StockExchangeDetail;
import com.bigstock.sharedComponent.entity.StockMonthPrice;
import com.bigstock.sharedComponent.entity.StockWeekPrice;
import com.bigstock.sharedComponent.service.MarginTradingAndShortSellingInfoService;
import com.bigstock.sharedComponent.service.ShareholderStructureService;
import com.bigstock.sharedComponent.service.StockDayPriceService;
import com.bigstock.sharedComponent.service.StockExchangeDetailService;
import com.bigstock.sharedComponent.service.StockInfoService;
import com.bigstock.sharedComponent.service.StockMonthPriceService;
import com.bigstock.sharedComponent.service.StockWeekPriceService;
import com.google.common.collect.Lists;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BizService {

	private final ShareholderStructureService shareholderStructureService;

	private final StockDayPriceService stockDayPriceService;

	private final StockExchangeDetailService stockExchangeDetailService;

	private final MarginTradingAndShortSellingInfoService marginTradingAndShortSellingInfoService;

	private final StockDayPriceNativeQueryService stockDayPriceNativeQueryService;

	private final StockWeekPriceService stockWeekPriceService;

	private final StockMonthPriceService stockMonthPriceService;

	private final StockInfoService stockInfoService;

	public List<StockExchangeDetail> getStockExchangeDetail(String stockCode, Date tradeDate) {
		return stockExchangeDetailService.findByStockCodeAndTradingDateOrderBySeqAsc(stockCode, tradeDate);
	}

	public List<ShareholderStructure> getStockShareholderStructure(String stockCode, int limit) {
		List<ShareholderStructure> shareholderStructures = shareholderStructureService
				.getShareholderStructureByStockCodeDesc(stockCode);
		if (shareholderStructures.size() > 52) {
			return shareholderStructures.subList(0, limit);
		} else {
			return shareholderStructures;
		}
	}

	public List<MarginTradingAndShortSellingInfo> getStockMarginTradingAndShortSelling(String stockCode) {
		List<StockDayPrice> stockDayPrices = stockDayPriceService.findPreviousFiftyTowDaysBeforeLastestDayInfo("2330");
		Date lastTradingDay = stockDayPrices.stream().findFirst().get().getTradingDay();
		Date firstTradingDay = stockDayPrices.stream()
				.sorted(Comparator.comparing(StockDayPrice::getTradingDay).reversed()).findFirst().get()
				.getTradingDay();
		List<MarginTradingAndShortSellingInfo> marginTradingAndShortSellingInfos = marginTradingAndShortSellingInfoService
				.findMarginTradingAndShortSellingInfoByDateRange(stockCode, lastTradingDay, firstTradingDay);
		Set<Date> marginTradingDaysSet = marginTradingAndShortSellingInfos.stream()
				.map(MarginTradingAndShortSellingInfo::getTradingDay).collect(Collectors.toSet());

		List<MarginTradingAndShortSellingInfo> newInfos = stockDayPrices.stream()
				.filter(stockDayPrice -> !marginTradingDaysSet.contains(stockDayPrice.getTradingDay()))
				.map(stockDayPrice -> {
					MarginTradingAndShortSellingInfo newInfo = new MarginTradingAndShortSellingInfo();
					newInfo.setTradingDay(stockDayPrice.getTradingDay());
					return newInfo;
				}).collect(Collectors.toList());

		if (CollectionUtils.isNotEmpty(newInfos)) {
			marginTradingAndShortSellingInfos.addAll(newInfos);
			marginTradingAndShortSellingInfos
					.sort(Comparator.comparing(MarginTradingAndShortSellingInfo::getTradingDay).reversed());
		}
		return marginTradingAndShortSellingInfos;
	}

	public SingleStockPriceVo getSingleStockPrices(String stockCode, Date startDate, Date endDate)
			throws ParseException {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		List<StockDayPrice> stockDayPrices = Lists.newArrayList();

		if (ObjectUtils.isEmpty(startDate) || ObjectUtils.isEmpty(endDate)) {
			stockDayPrices = stockDayPriceService.findStockCodeAndLimit(stockCode, 360);
		} else {

			stockDayPrices = stockDayPriceService.findByStockCodeAndStartDateAndEndDateCache(stockCode,
					sdf.format(startDate), sdf.format(endDate));
		}
		List<SingleStockDayPriceVo> singleStockDayPriceVos = stockDayPrices.stream()
				.sorted(Comparator.comparing(StockDayPrice::getTradingDay).reversed()).map(stockDayPrice -> {
					String highPrice = stockDayPrice.getHighPrice();
					String lowPrice = stockDayPrice.getLowPrice();
					String openingPrice = stockDayPrice.getOpeningPrice();
					String closingPrice = stockDayPrice.getClosingPrice();
					SimpleDateFormat sfd = new SimpleDateFormat("yyyy-MM-dd");
					SingleStockDayPriceVo vo = new SingleStockDayPriceVo();
					vo.setClosingPrice(closingPrice.replaceAll(",", ""));
					vo.setOpeningPrice(openingPrice.replaceAll(",", ""));
					vo.setHighPrice(highPrice.replaceAll(",", ""));
					vo.setLowPrice(lowPrice.replaceAll(",", ""));
					vo.setTradingDate(sfd.format(stockDayPrice.getTradingDay()));
					vo.setStockCode(stockCode);
					String tradingVolume = stockDayPrice.getTradingVolume();
					if (StringUtils.isNotBlank(tradingVolume)) {
						vo.setTradingVolume(new BigDecimal(tradingVolume)
								.divide(new BigDecimal("1000"), 0, RoundingMode.HALF_UP).toString());
					}
					vo.setFiveMa(ObjectUtils.isNotEmpty(stockDayPrice.getFiveDaysMa())
							? new BigDecimal(stockDayPrice.getFiveDaysMa())
							: new BigDecimal(0));
					vo.setTenMa(ObjectUtils.isNotEmpty(stockDayPrice.getTenDaysMa())
							? new BigDecimal(stockDayPrice.getTenDaysMa())
							: new BigDecimal(0));
					vo.setTwentyMa(ObjectUtils.isNotEmpty(stockDayPrice.getTwentyDaysMa())
							? new BigDecimal(stockDayPrice.getTwentyDaysMa())
							: new BigDecimal(0));
					vo.setSixtyMa(ObjectUtils.isNotEmpty(stockDayPrice.getTwentyDaysMa())
							? new BigDecimal(stockDayPrice.getSixtyDaysMa())
							: new BigDecimal(0));
					vo.setOneTwentyMa(ObjectUtils.isNotEmpty(stockDayPrice.getOneTwentyDaysMa())
							? new BigDecimal(stockDayPrice.getOneTwentyDaysMa())
							: new BigDecimal(0));
					vo.setTwoFourtyMa(ObjectUtils.isNotEmpty(stockDayPrice.getTwoFourtyDaysMa())
							? new BigDecimal(stockDayPrice.getTwoFourtyDaysMa())
							: new BigDecimal(0));
					vo.setLineKvalue(ObjectUtils.isNotEmpty(stockDayPrice.getLineKvalue())
							? new BigDecimal(stockDayPrice.getLineKvalue())
							: new BigDecimal(0));
					vo.setLineDvalue(ObjectUtils.isNotEmpty(stockDayPrice.getLineDvalue())
							? new BigDecimal(stockDayPrice.getLineDvalue())
							: new BigDecimal(0));
					return vo;
				}).toList();
		List<SingleStockWeekPriceVo> singleStockWeekPriceVo = stockWeekPriceService
				.findStockCodeAndLimit(stockCode, 360).stream()
				.sorted(Comparator.comparing(StockWeekPrice::getWeekOfYear)).map(stockWeekPrice -> {
					SingleStockWeekPriceVo vo = new SingleStockWeekPriceVo();
					vo.setClosingPrice(stockWeekPrice.getClosingPrice().toString());
					vo.setOpeningPrice(stockWeekPrice.getOpeningPrice().toString());
					vo.setHighPrice(stockWeekPrice.getHighPrice().toString());
					vo.setLowPrice(stockWeekPrice.getLowPrice().toString());
					vo.setFirstTradingDate(sdf.format(stockWeekPrice.getFirstTradingDay()));
					vo.setStockCode(stockCode);
					String tradingVolume = stockWeekPrice.getTradingVolume().toString();
					if (StringUtils.isNotBlank(tradingVolume)) {
						vo.setTradingVolume(new BigDecimal(tradingVolume)
								.divide(new BigDecimal("1000"), 0, RoundingMode.HALF_UP).toString());
					}
					vo.setFiveMa(ObjectUtils.isNotEmpty(stockWeekPrice.getFiveWeekMa()) ? stockWeekPrice.getFiveWeekMa()
							: new BigDecimal(0));
					vo.setTenMa(ObjectUtils.isNotEmpty(stockWeekPrice.getTenWeekMa()) ? stockWeekPrice.getTenWeekMa()
							: new BigDecimal(0));
					vo.setTwentyMa(
							ObjectUtils.isNotEmpty(stockWeekPrice.getTwentyWeekMa()) ? stockWeekPrice.getTenWeekMa()
									: new BigDecimal(0));
					vo.setSixtyMa(
							ObjectUtils.isNotEmpty(stockWeekPrice.getTwentyWeekMa()) ? stockWeekPrice.getTwentyWeekMa()
									: new BigDecimal(0));
					vo.setOneTwentyMa(ObjectUtils.isNotEmpty(stockWeekPrice.getOneTwentyWeekMa())
							? stockWeekPrice.getOneTwentyWeekMa()
							: new BigDecimal(0));
					vo.setTwoFourtyMa(ObjectUtils.isNotEmpty(stockWeekPrice.getTwoFourtyWeekMa())
							? stockWeekPrice.getTwoFourtyWeekMa()
							: new BigDecimal(0));
					vo.setLineKvalue(
							ObjectUtils.isNotEmpty(stockWeekPrice.getLineKValue()) ? stockWeekPrice.getLineKValue()
									: new BigDecimal(0));
					vo.setLineDvalue(
							ObjectUtils.isNotEmpty(stockWeekPrice.getLineDValue()) ? stockWeekPrice.getLineDValue()
									: new BigDecimal(0));
					return vo;
				}).toList();
		List<SingleStockMonthPriceVo> singleStockMonthPriceVo = stockMonthPriceService
				.findStockCodeAndLimit(stockCode, 240).stream()
				.sorted(Comparator.comparing(StockMonthPrice::getYear).thenComparing(StockMonthPrice::getMonth))
				.map(stockMonthPrice -> {
					SingleStockMonthPriceVo vo = new SingleStockMonthPriceVo();
					vo.setClosingPrice(stockMonthPrice.getClosingPrice().toString());
					vo.setOpeningPrice(stockMonthPrice.getOpeningPrice().toString());
					vo.setHighPrice(stockMonthPrice.getHighPrice().toString());
					vo.setLowPrice(stockMonthPrice.getLowPrice().toString());
					vo.setFirstTradingDate(sdf.format(stockMonthPrice.getFirstTradingDay()));
					vo.setStockCode(stockCode);
					String tradingVolume = stockMonthPrice.getTradingVolume().toString();
					if (StringUtils.isNotBlank(tradingVolume)) {
						vo.setTradingVolume(new BigDecimal(tradingVolume)
								.divide(new BigDecimal("1000"), 0, RoundingMode.HALF_UP).toString());
					}
					vo.setFiveMa(
							ObjectUtils.isNotEmpty(stockMonthPrice.getFiveMonthMa()) ? stockMonthPrice.getFiveMonthMa()
									: new BigDecimal(0));
					vo.setTenMa(
							ObjectUtils.isNotEmpty(stockMonthPrice.getTenMonthMa()) ? stockMonthPrice.getTenMonthMa()
									: new BigDecimal(0));
					vo.setTwentyMa(ObjectUtils.isNotEmpty(stockMonthPrice.getTwentyMonthMa())
							? stockMonthPrice.getTwentyMonthMa()
							: new BigDecimal(0));
					vo.setSixtyMa(ObjectUtils.isNotEmpty(stockMonthPrice.getTwentyMonthMa())
							? stockMonthPrice.getTwentyMonthMa()
							: new BigDecimal(0));
					vo.setOneTwentyMa(ObjectUtils.isNotEmpty(stockMonthPrice.getOneTwentyMonthMa())
							? stockMonthPrice.getOneTwentyMonthMa()
							: new BigDecimal(0));
					vo.setTwoFourtyMa(ObjectUtils.isNotEmpty(stockMonthPrice.getTwoFourtyMonthMa())
							? stockMonthPrice.getTwoFourtyMonthMa()
							: new BigDecimal(0));
					vo.setLineKvalue(
							ObjectUtils.isNotEmpty(stockMonthPrice.getLineKValue()) ? stockMonthPrice.getLineKValue()
									: new BigDecimal(0));
					vo.setLineDvalue(
							ObjectUtils.isNotEmpty(stockMonthPrice.getLineDValue()) ? stockMonthPrice.getLineDValue()
									: new BigDecimal(0));
					return vo;
				}).toList();
		SingleStockPriceVo sspv = new SingleStockPriceVo();
		sspv.setSingleStockDayPriceVos(singleStockDayPriceVos);
		sspv.setSingleStockWeekPriceVos(singleStockWeekPriceVo);
		sspv.setSingleStockMonthPriceVos(singleStockMonthPriceVo);
		return sspv;
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

	public List<StockInfoVo> getMatchStockCodeByCondition(DynamicFilterStockCodeVo dynamicFilterStockCodeVo) {
		Map<String, List<DynamicFilterStockPriceCondition>> dynamicFilterStockPriceConditionsMap = dynamicFilterStockCodeVo
				.getConditions().stream().collect(Collectors.groupingBy(DynamicFilterStockPriceCondition::getType));
		List<StockDayPrice> stockDayPrices = stockDayPriceService.findPreviousFiftyTowDaysBeforeLastestDayInfo("2330");
		Date lastTradingDay = stockDayPrices.stream().findFirst().get().getTradingDay();
		List<String> matchStocks = dynamicFilterStockPriceConditionsMap.entrySet().stream().map(entry -> {
			List<DynamicFilterStockPriceCondition> dynamicFilterStockPriceConditions = entry.getValue();
			String type = entry.getKey();
			if ("kd".equals(type)) {
				DynamicFilterStockPriceCondition dynamicFilterStockPriceCondition = dynamicFilterStockPriceConditions
						.get(0);
				if (dynamicFilterStockPriceCondition.getValue().get(0).equals("20")) {
					return stockDayPriceNativeQueryService.findKvalueUnderTwentyByDateRange(lastTradingDay,
							dynamicFilterStockPriceCondition.getLimit());
				} else {
					return stockDayPriceNativeQueryService.findKvalueUpperEightByDateRange(lastTradingDay,
							dynamicFilterStockPriceCondition.getLimit());
				}
			} else if ("change".equals(type)) {
				return stockDayPriceNativeQueryService.findByDateRangeChangeRateOverFilter(lastTradingDay,
						dynamicFilterStockPriceConditions.get(0).getLimit(),
						dynamicFilterStockPriceConditions.get(0).getValue().get(0));
			} else if ("limitUp".equals(type)) {
				return stockDayPriceService.findTodateReachLimitUp(lastTradingDay).stream()
						.map(stockDayPrice -> stockDayPrice.getStockCode()).toList();
			} else if ("ma".equals(type)) {
				return stockDayPriceNativeQueryService.findByDateRangeMaChangeFilter(dynamicFilterStockPriceConditions,
						lastTradingDay, dynamicFilterStockPriceConditions.get(0).getLimit());
			} else {
				return new ArrayList<String>(); // 返回空列表
			}
		}).filter(list -> !list.isEmpty()) // 过滤掉空的列表
				.reduce((list1, list2) -> {
					// 计算交集
					list1.retainAll(list2);
					return list1;
				}).orElse(new ArrayList<String>()); // 如果所有列表为空，则返回空列表

		List<StockInfoVo> stockInfos = stockInfoService.findByIds(matchStocks).stream()
				.filter(data -> StringUtils.isNotBlank(data.getStockType())).map(data -> {
					StockInfoVo vo = new StockInfoVo();
					vo.setStockCode(data.getStockCode());
					vo.setStockName(data.getStockName());
					switch (data.getStockType()) {
					case "0" -> vo.setStockTypeName("上櫃");
					case "1" -> vo.setStockTypeName("上市");
					case "2" -> vo.setStockTypeName("興櫃");
					default -> throw new RuntimeException("無法判斷個股上市櫃類型");
					}
					;
					return vo;
				}).toList();
		return stockInfos;
	}

	private String findMaxWeek(LocalDate today, AtomicInteger weekOfYear) {
		boolean checkResult = false;
		while (!checkResult) {
			String week = String.valueOf(today.getYear()) + "W" + weekOfYear.get();
			checkResult = shareholderStructureService.checkWeekExist(week);
			if (!checkResult) {
				weekOfYear.addAndGet(-1);
			}
			if (weekOfYear.get() == 0) {
				today = today.minusYears(1).withMonth(12).withDayOfMonth(31);
				weekOfYear = new AtomicInteger(today.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
			}
		}
		return String.valueOf(today.getYear()) + "W" + weekOfYear;
	}
}
