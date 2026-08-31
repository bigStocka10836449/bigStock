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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.function.Function;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.juli.logging.Log;
import org.springframework.stereotype.Service;

import com.bigstock.biz.controller.GatewayController;
import com.bigstock.biz.domain.StockDayPriceNativeQueryService;
import com.bigstock.biz.domain.StockMonthPriceNativeQueryService;
import com.bigstock.biz.domain.StockWeekPriceNativeQueryService;
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
import com.bigstock.sharedComponent.entity.StockInfo;
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

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class BizService {

	private static final List<String> MA_TYPE = List.of("five_ma_slope", "ten_ma_slope", "twenty_ma_slope",
			"sixty_ma_slope", "one_twenty_ma_slope", "two_fourty_ma_slope");

	private final ShareholderStructureService shareholderStructureService;

	private final StockDayPriceService stockDayPriceService;

	private final StockExchangeDetailService stockExchangeDetailService;

	private final MarginTradingAndShortSellingInfoService marginTradingAndShortSellingInfoService;

	private final StockDayPriceNativeQueryService stockDayPriceNativeQueryService;

	private final StockWeekPriceNativeQueryService stockWeekPriceNativeQueryService;

	private final StockMonthPriceNativeQueryService stockMonthPriceNativeQueryService;

	private final StockWeekPriceService stockWeekPriceService;

	private final StockMonthPriceService stockMonthPriceService;

	private final StockInfoService stockInfoService;

	public List<StockExchangeDetail> getStockExchangeDetail(String stockCode, Date tradeDate) {
		return stockExchangeDetailService.findByStockCodeAndTradingDateOrderBySeqAsc(stockCode, tradeDate);
	}

	public List<ShareholderStructure> getStockShareholderStructure(String stockCode, int limit) {
		List<ShareholderStructure> shareholderStructures = shareholderStructureService
				.getShareholderStructureByStockCodeDesc(stockCode);
		if (shareholderStructures.size() > limit) {
			return shareholderStructures.subList(0, limit);
		} else {
			return shareholderStructures;
		}
	}

	public List<MarginTradingAndShortSellingInfo> getStockMarginTradingAndShortSelling(String stockCode) {
		List<StockDayPrice> stockDayPrices = stockDayPriceService.findPreviousFiftyTowDaysBeforeLastestDayInfo("2330");
		Date lastTradingDay = stockDayPrices.stream().findFirst().get().getTradingDay();
		Date firstTradingDay = stockDayPrices.stream().sorted(Comparator.comparing(StockDayPrice::getTradingDay))
				.findFirst().get().getTradingDay();
		List<MarginTradingAndShortSellingInfo> marginTradingAndShortSellingInfos = marginTradingAndShortSellingInfoService
				.findMarginTradingAndShortSellingInfoByDateRange(stockCode, firstTradingDay, lastTradingDay);
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

        // 真正會查 stock_info 的地方：findAllById(ids)
	    List<String> singleStockId = Lists.newArrayList();
	    singleStockId.add(stockCode);
        List<StockInfo> singleInfos = Lists.newArrayList();
        stockDayPrices = stockDayPriceService.findLastest600StockDayPriceByStockCodeCache(stockCode);
        //stockInfoService.findByIds(matchStocks);
	    singleInfos = stockInfoService.findByIds(singleStockId);
	    final String singleStockName = singleInfos.get(0).getStockName().toString();
	    // 新增：先用「日K本身」算出日期區間，再查融資融券，同日 merge
	    Date lastTradingDay = null;   // 最新交易日
	    Date firstTradingDay = null;  // 最舊交易日

	    if (stockDayPrices != null && !stockDayPrices.isEmpty()) {
	        lastTradingDay = stockDayPrices.stream()
	                .max(Comparator.comparing(StockDayPrice::getTradingDay))
	                .map(StockDayPrice::getTradingDay)
	                .orElse(null);

	        firstTradingDay = stockDayPrices.stream()
	                .min(Comparator.comparing(StockDayPrice::getTradingDay))
	                .map(StockDayPrice::getTradingDay)
	                .orElse(null);
	    }

	    // key = yyyy-MM-dd
	    Map<String, MarginTradingAndShortSellingInfo> marginMapByDay = new HashMap<>();

	    if (firstTradingDay != null && lastTradingDay != null) {
	        List<MarginTradingAndShortSellingInfo> marginInfos =
	                marginTradingAndShortSellingInfoService
	                        .findMarginTradingAndShortSellingInfoByDateRange(
	                                stockCode,
	                                firstTradingDay,
	                                lastTradingDay
	                        );

	        if (marginInfos != null && !marginInfos.isEmpty()) {
	            for (MarginTradingAndShortSellingInfo m : marginInfos) {
	                if (m != null && m.getTradingDay() != null) {
	                    marginMapByDay.put(
	                            sdf.format(m.getTradingDay()),
	                            m
	                    );
	                }
	            }
	        }
	    }

	    //日K維持原本：日期倒序
	    List<SingleStockDayPriceVo> singleStockDayPriceVos = stockDayPrices.stream()
	            .sorted(Comparator.comparing(StockDayPrice::getTradingDay).reversed())
	            .map(stockDayPrice -> {

	            	//String stockName = singleInfos.get(0).getStockName();
	                String highPrice = stockDayPrice.getHighPrice();
	                String lowPrice = stockDayPrice.getLowPrice();
	                String openingPrice = stockDayPrice.getOpeningPrice();
	                String closingPrice = stockDayPrice.getClosingPrice();

	                SingleStockDayPriceVo vo = new SingleStockDayPriceVo();
	               
	                vo.setStockName(singleStockName);
	                vo.setClosingPrice(closingPrice == null ? null : closingPrice.replaceAll(",", ""));
	                vo.setOpeningPrice(openingPrice == null ? null : openingPrice.replaceAll(",", ""));
	                vo.setHighPrice(highPrice == null ? null : highPrice.replaceAll(",", ""));
	                vo.setLowPrice(lowPrice == null ? null : lowPrice.replaceAll(",", ""));
	                if(ObjectUtils.isNotEmpty(stockDayPrice.getChange())) {
	                	vo.setChange(stockDayPrice.getChange());
	                } else {
	                	vo.setChange("");
	                }
	                if(ObjectUtils.isNotEmpty(stockDayPrice.getChangeRate())) {
	                	vo.setChangeRate(stockDayPrice.getChangeRate().toString());
	                } else {
	                	vo.setChangeRate("");
	                }
	                
	                String tradingDateStr = sdf.format(stockDayPrice.getTradingDay());
	                vo.setTradingDate(tradingDateStr);
	                vo.setStockCode(stockCode);

	                String tradingVolume = stockDayPrice.getTradingVolume();
	                if (StringUtils.isNotBlank(tradingVolume)) {
	                    vo.setTradingVolume(
	                            new BigDecimal(tradingVolume)
	                                    .divide(new BigDecimal("1000"), 0, RoundingMode.HALF_UP)
	                                    .toString()
	                    );
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
	                vo.setSixtyMa(ObjectUtils.isNotEmpty(stockDayPrice.getSixtyDaysMa())
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

	                // =======================
	                // ✅ merge 融資融券（同日）
	                // =======================
	                MarginTradingAndShortSellingInfo margin = marginMapByDay.get(tradingDateStr);
	                if (margin != null) {
	                    vo.setMarginPurchaseBalancePreviousDay(margin.getMarginPurchaseBalancePreviousDay());
	                    vo.setMarginPurchase(margin.getMarginPurchase());
	                    vo.setMarginSales(margin.getMarginSales());
	                    vo.setCashRedemption(margin.getCashRedemption());
	                    vo.setMarginPurchaseBalance(margin.getMarginPurchaseBalance());
	                    vo.setMarginPurchaseQuota(margin.getMarginPurchaseQuota());

	                    vo.setShortSaleBalancePreviousDay(margin.getShortSaleBalancePreviousDay());
	                    vo.setShortSale(margin.getShortSale());
	                    vo.setShortConvering(margin.getShortConvering());
	                    vo.setStockRedemption(margin.getStockRedemption());
	                    vo.setShortSaleBalance(margin.getShortSaleBalance());
	                    vo.setShortSaleQuota(margin.getShortSaleQuota());

	                    vo.setOffsetting(margin.getOffsetting());
	                }

	                return vo;
	            })
	            .toList();
	    
	    

	    // 以下週/月先不要改
	    List<SingleStockWeekPriceVo> singleStockWeekPriceVo = stockWeekPriceService
	            .findStockCodeAndLimit(stockCode, 500).stream()
	            .sorted(Comparator.comparing(StockWeekPrice::getWeekOfYear))
	            .map(stockWeekPrice -> {
	                SingleStockWeekPriceVo vo = new SingleStockWeekPriceVo();
	                vo.setStockName(singleStockName);
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
	                vo.setStockName(singleStockName);
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
	                vo.setFiveMa(ObjectUtils.isNotEmpty(stockMonthPrice.getFiveMonthMa()) ? stockMonthPrice.getFiveMonthMa()
	                        : new BigDecimal(0));
	                vo.setTenMa(ObjectUtils.isNotEmpty(stockMonthPrice.getTenMonthMa()) ? stockMonthPrice.getTenMonthMa()
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

	@Transactional
	public List<StockInfoVo> getMatchStockCodeByCondition(List<DynamicFilterStockCodeVo> dynamicFilterStockCodeVos) {

	    // ✅ 1) 先把 mapper 抽出來，明確告訴編譯器：輸入是 DynamicFilterStockCodeVo，輸出是 List<StockInfoVo>
	    Function<DynamicFilterStockCodeVo, List<StockInfoVo>> mapper = (DynamicFilterStockCodeVo dynamicFilterStockCodeVo) -> {

	        String aspect = dynamicFilterStockCodeVo.getAspect();

	        Map<String, List<DynamicFilterStockPriceCondition>> dynamicFilterStockPriceConditionsMap =
	                dynamicFilterStockCodeVo.getConditions()
	                        .stream()
	                        .collect(Collectors.groupingBy(DynamicFilterStockPriceCondition::getType));
	        Date lastTradingDay =  stockDayPriceService.getCurrentTradeDate();

	        List<String> matchStocks = switch (aspect) {
	            case "daily" -> processDaily(dynamicFilterStockPriceConditionsMap, lastTradingDay);
	            case "monthly" -> processMonthly(dynamicFilterStockPriceConditionsMap, lastTradingDay);
	            case "weekly" -> processWeekly(dynamicFilterStockPriceConditionsMap, lastTradingDay);
	            default -> throw new RuntimeException("視角不存在");
	        };

	        // ✅ log：你到底拿了甚麼 stockId 再去 stock_info 查？
	        if (matchStocks == null) {
	            log.info("[StockCodeByFilter] aspect={}, matchStocks=null", aspect);
	        } else {
	            int preview = Math.min(matchStocks.size(), 20);
	            log.info("[StockCodeByFilter] aspect={}, matchStocks.size={}, preview={}",
	                    aspect, matchStocks.size(), matchStocks.subList(0, preview));
	        }

	        // ✅ 真正會查 stock_info 的地方：findAllById(ids)
	        List<StockInfo> infos = stockInfoService.findByIds(matchStocks);

	        // ✅ log：DB 撈回來的 stock_name/type 在後端是否正常（這能一刀兩斷判斷亂碼在哪）
	        if (infos != null && !infos.isEmpty()) {
	            StockInfo first = infos.get(0);
	            log.info("[StockCodeByFilter] stock_info first: code={}, name={}, type={}",
	                    safe(first.getStockCode()),
	                    safe(first.getStockName()),
	                    safe(first.getStockType()));
	        } else {
	            log.info("[StockCodeByFilter] stock_info result empty, ids.size={}",
	                    matchStocks == null ? 0 : matchStocks.size());
	        }

	        // ✅ 原本邏輯 그대로：轉成 StockInfoVo
	        return infos.stream()
	                .filter(data -> StringUtils.isNotBlank(data.getStockType()))
	                .map(data -> {
	                    StockInfoVo vo = new StockInfoVo();
	                    vo.setStockCode(data.getStockCode());
	                    vo.setStockName(data.getStockName());
	                    switch (data.getStockType()) {
	                        case "0" -> vo.setStockTypeName("上櫃");
	                        case "1" -> vo.setStockTypeName("上市");
	                        case "2" -> vo.setStockTypeName("興櫃");
	                        default -> throw new RuntimeException("無法判斷個股上市櫃類型");
	                    }
	                    return vo;
	                })
	                .collect(Collectors.toList());
	    };

	    // ✅ 2) 這裡 map(mapper) 就不會再推斷失敗
	    List<List<StockInfoVo>> resultLists = dynamicFilterStockCodeVos.stream()
	            .map(mapper)
	            .collect(Collectors.toList());

	    // ✅ 3) 交集邏輯（等價於你原本 reduce + retainAll）
	    if (resultLists.isEmpty()) {
	        return new ArrayList<>();
	    }

	    List<StockInfoVo> result = new ArrayList<>(resultLists.get(0));
	    for (int i = 1; i < resultLists.size(); i++) {
	        result.retainAll(resultLists.get(i));
	    }
	    return result;
	}

	private static String safe(String s) {
	    return s == null ? "null" : s;
	}

	private static String toCodePoints(String s) {
	    if (s == null) return "null";
	    StringBuilder sb = new StringBuilder();
	    for (int i = 0; i < s.length(); i++) {
	        sb.append(String.format("\\u%04X", (int) s.charAt(i)));
	    }
	    return sb.toString();
	}

	private List<String> processDaily(Map<String, List<DynamicFilterStockPriceCondition>> conditionsMap,
			Date lastTradingDay) {
		return conditionsMap.entrySet().stream().map(entry -> {
			String type = entry.getKey();
			List<DynamicFilterStockPriceCondition> conditions = entry.getValue();

			if ("kd".equals(type)) {
				DynamicFilterStockPriceCondition condition = conditions.get(0);
				if (condition.getValue().get(0).equals("20")) {
					return stockDayPriceNativeQueryService.findKvalueUnderTwentyByDateRange(lastTradingDay,
							condition.getLimit());
				} else {
					return stockDayPriceNativeQueryService.findKvalueUpperEightByDateRange(lastTradingDay,
							condition.getLimit());
				}
			} else if ("change".equals(type)) {
				return stockDayPriceNativeQueryService.findByDateRangeChangeRateOverFilter(lastTradingDay,
						conditions.get(0).getLimit(), conditions.get(0).getValue().get(0), conditions.get(0).getOperator());
			} else if ("limitUp".equals(type)) {
				return stockDayPriceService.findTodateReachLimitUp(lastTradingDay).stream()
						.map(StockDayPrice::getStockCode).toList();
			} else if ("ma".equals(type)) {
				AtomicInteger fiveDaysSlopeLimit = new AtomicInteger(0);
				AtomicInteger tenDaysSlopeLimit = new AtomicInteger(0);
				AtomicInteger twentyDaysSlopeLimit = new AtomicInteger(0);
				AtomicInteger sixtyDaysSlopeLimit = new AtomicInteger(0);
				AtomicInteger oneTwentyDaysSlopeLimit = new AtomicInteger(0);
				AtomicInteger twoFourtyDaysSlopeLimit = new AtomicInteger(0);
				MA_TYPE.stream().forEach(maTypeName -> {
					conditions.stream().filter(condition -> condition.getName().equals(maTypeName)).forEach(condition -> {
						switch (condition.getName()) {
						case "five_ma_slope":
							if (ObjectUtils.isNotEmpty(condition.getLimit())) {
								fiveDaysSlopeLimit.set(condition.getLimit());
							}
							break;
						case "ten_ma_slope":
							if (ObjectUtils.isNotEmpty(condition.getLimit())) {
								tenDaysSlopeLimit.set(condition.getLimit());
							}
							break;
						case "twenty_ma_slope":
							if (ObjectUtils.isNotEmpty(condition.getLimit())) {
								twentyDaysSlopeLimit.set(condition.getLimit());
							}
							break;
						case "sixty_ma_slope":
							if (ObjectUtils.isNotEmpty(condition.getLimit())) {
								sixtyDaysSlopeLimit.set(condition.getLimit());
							}
							break;
						case "one_twenty_ma_slope":
							if (ObjectUtils.isNotEmpty(condition.getLimit())) {
								oneTwentyDaysSlopeLimit.set(condition.getLimit());
							}
							break;
						case "two_fourty_ma_slope":
							if (ObjectUtils.isNotEmpty(condition.getLimit())) {
								twoFourtyDaysSlopeLimit.set(condition.getLimit());
							}
							;
							break;
						}
					});
				});
				return stockDayPriceNativeQueryService.findByDateRangeMaChangeFilter(conditions, lastTradingDay,
						fiveDaysSlopeLimit.get(), tenDaysSlopeLimit.get(), twentyDaysSlopeLimit.get(),
						sixtyDaysSlopeLimit.get(), oneTwentyDaysSlopeLimit.get(), twoFourtyDaysSlopeLimit.get());
			} else {
				return new ArrayList<String>();
			}
		}).filter(list -> !list.isEmpty()).reduce((list1, list2) -> {
			List<String> mutableList = new ArrayList<>(list1);
			mutableList.retainAll(list2);
			return mutableList;
		}).orElse(new ArrayList<String>());
	}

	private List<String> processMonthly(Map<String, List<DynamicFilterStockPriceCondition>> conditionsMap,
			Date lastTradingDay) {
		return conditionsMap.entrySet().stream().map(entry -> {
			String type = entry.getKey();
			List<DynamicFilterStockPriceCondition> conditions = entry.getValue();

			if ("kd".equals(type)) {
				DynamicFilterStockPriceCondition condition = conditions.get(0);
				if (condition.getValue().get(0).equals("20")) {
					return stockMonthPriceNativeQueryService.findKvalueUnderTwentyByDateRange(lastTradingDay,
							condition.getLimit());
				} else {
					return stockMonthPriceNativeQueryService.findKvalueUpperEightByDateRange(lastTradingDay,
							condition.getLimit());
				}
			} else if ("ma".equals(type)) {
				AtomicInteger fiveDaysSlopeLimit = new AtomicInteger(0);
				AtomicInteger tenDaysSlopeLimit = new AtomicInteger(0);
				AtomicInteger twentyDaysSlopeLimit = new AtomicInteger(0);
				AtomicInteger sixtyDaysSlopeLimit = new AtomicInteger(0);
				AtomicInteger oneTwentyDaysSlopeLimit = new AtomicInteger(0);
				AtomicInteger twoFourtyDaysSlopeLimit = new AtomicInteger(0);
				MA_TYPE.stream().forEach(maTypeName -> {
					conditions.stream().filter(condition -> condition.getName().equals(maTypeName)).forEach(condition -> {
						switch (condition.getName()) {
						case "five_ma_slope":
							if (ObjectUtils.isNotEmpty(condition.getLimit())) {
								fiveDaysSlopeLimit.set(condition.getLimit());
							}
							break;
						case "ten_ma_slope":
							if (ObjectUtils.isNotEmpty(condition.getLimit())) {
								tenDaysSlopeLimit.set(condition.getLimit());
							}
							break;
						case "twenty_ma_slope":
							if (ObjectUtils.isNotEmpty(condition.getLimit())) {
								twentyDaysSlopeLimit.set(condition.getLimit());
							}
							break;
						case "sixty_ma_slope":
							if (ObjectUtils.isNotEmpty(condition.getLimit())) {
								sixtyDaysSlopeLimit.set(condition.getLimit());
							}
							break;
						case "one_twenty_ma_slope":
							if (ObjectUtils.isNotEmpty(condition.getLimit())) {
								oneTwentyDaysSlopeLimit.set(condition.getLimit());
							}
							break;
						case "two_fourty_ma_slope":
							if (ObjectUtils.isNotEmpty(condition.getLimit())) {
								twoFourtyDaysSlopeLimit.set(condition.getLimit());
							}
							;
							break;
						}
					});
				});
				return stockMonthPriceNativeQueryService.findByDateRangeMaChangeFilter(conditions, lastTradingDay,
						fiveDaysSlopeLimit.get(), tenDaysSlopeLimit.get(), twentyDaysSlopeLimit.get(),
						sixtyDaysSlopeLimit.get(), oneTwentyDaysSlopeLimit.get(), twoFourtyDaysSlopeLimit.get());
			} else {
				return new ArrayList<String>();
			}
		}).filter(list -> !list.isEmpty()).reduce((list1, list2) -> {
			List<String> mutableList = new ArrayList<>(list1);
			mutableList.retainAll(list2);
			return mutableList;
		}).orElse(new ArrayList<String>());
	}

	private List<String> processWeekly(Map<String, List<DynamicFilterStockPriceCondition>> conditionsMap,
			Date lastTradingDay) {
		return conditionsMap.entrySet().stream().map(entry -> {
			String type = entry.getKey();
			List<DynamicFilterStockPriceCondition> conditions = entry.getValue();

			if ("kd".equals(type)) {
				DynamicFilterStockPriceCondition condition = conditions.get(0);
				if (condition.getValue().get(0).equals("20")) {
					return stockWeekPriceNativeQueryService.findKvalueUnderTwentyByDateRange(lastTradingDay,
							condition.getLimit());
				} else {
					return stockWeekPriceNativeQueryService.findKvalueUpperEightByDateRange(lastTradingDay,
							condition.getLimit());
				}
			} else if ("ma".equals(type)) {
				AtomicInteger fiveDaysSlopeLimit = new AtomicInteger(0);
				AtomicInteger tenDaysSlopeLimit = new AtomicInteger(0);
				AtomicInteger twentyDaysSlopeLimit = new AtomicInteger(0);
				AtomicInteger sixtyDaysSlopeLimit = new AtomicInteger(0);
				AtomicInteger oneTwentyDaysSlopeLimit = new AtomicInteger(0);
				AtomicInteger twoFourtyDaysSlopeLimit = new AtomicInteger(0);
				MA_TYPE.stream().forEach(maTypeName -> {
					conditions.stream().filter(condition -> condition.getName().equals(maTypeName)).forEach(condition -> {
						switch (condition.getName()) {
						case "five_ma_slope":
							if (ObjectUtils.isNotEmpty(condition.getLimit())) {
								fiveDaysSlopeLimit.set(condition.getLimit());
							}
							break;
						case "ten_ma_slope":
							if (ObjectUtils.isNotEmpty(condition.getLimit())) {
								tenDaysSlopeLimit.set(condition.getLimit());
							}
							break;
						case "twenty_ma_slope":
							if (ObjectUtils.isNotEmpty(condition.getLimit())) {
								twentyDaysSlopeLimit.set(condition.getLimit());
							}
							break;
						case "sixty_ma_slope":
							if (ObjectUtils.isNotEmpty(condition.getLimit())) {
								sixtyDaysSlopeLimit.set(condition.getLimit());
							}
							break;
						case "one_twenty_ma_slope":
							if (ObjectUtils.isNotEmpty(condition.getLimit())) {
								oneTwentyDaysSlopeLimit.set(condition.getLimit());
							}
							break;
						case "two_fourty_ma_slope":
							if (ObjectUtils.isNotEmpty(condition.getLimit())) {
								twoFourtyDaysSlopeLimit.set(condition.getLimit());
							}
							;
							break;
						}
					});
				});
				return stockWeekPriceNativeQueryService.findByDateRangeMaChangeFilter(conditions, lastTradingDay,
						fiveDaysSlopeLimit.get(), tenDaysSlopeLimit.get(), twentyDaysSlopeLimit.get(),
						sixtyDaysSlopeLimit.get(), oneTwentyDaysSlopeLimit.get(), twoFourtyDaysSlopeLimit.get());
			} else {
				return new ArrayList<String>();
			}
		}).filter(list -> !list.isEmpty()).reduce((list1, list2) -> {
			List<String> mutableList = new ArrayList<>(list1);
			mutableList.retainAll(list2);
			return mutableList;
		}).orElse(new ArrayList<String>());
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
