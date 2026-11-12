package com.bigstock.schedule.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.bigstock.sharedComponent.entity.MarginTradingAndShortSellingInfo;
import com.bigstock.sharedComponent.entity.StockDayPrice;
import com.bigstock.sharedComponent.entity.StockDayPriceRank;
import com.bigstock.sharedComponent.entity.StockInfo;
import com.bigstock.sharedComponent.entity.StockIntradayPrice;
import com.bigstock.sharedComponent.entity.StockMonthPrice;
import com.bigstock.sharedComponent.entity.StockMonthPriceRank;
import com.bigstock.sharedComponent.entity.StockWeekPrice;
import com.bigstock.sharedComponent.entity.StockWeekPriceRank;
import com.bigstock.sharedComponent.redis.CacheOperatorService;
import com.bigstock.sharedComponent.service.GrabThirdPartyStockDayPrice;
import com.bigstock.sharedComponent.service.MarginTradingAndShortSellingInfoService;
import com.bigstock.sharedComponent.service.RankStockChangeService;
import com.bigstock.sharedComponent.service.StockDayPriceService;
import com.bigstock.sharedComponent.service.StockInfoService;
import com.bigstock.sharedComponent.service.StockIntradayPriceService;
import com.bigstock.sharedComponent.service.StockMonthPriceService;
import com.bigstock.sharedComponent.service.StockWeekPriceService;
import com.bigstock.sharedComponent.utils.ChromeDriverUtils;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class GrabFromThirdParty {
	
	private static final int STOCK_BATCH_SIZE = 50;

	private static final int FIVE_MINUTE_MAX_SIZE = 2880;
	private static final int SIXTY_MINUTE_MAX_SIZE = 240;

	// Yahoo 抓取間隔
	private static final long YAHOO_REQUEST_INTERVAL_MS = 2000L;
	
	// DB 歷史資料讀取數量
	private static final int INDICATOR_HISTORY_SIZE = 240;


	private final StockInfoService stockInfoService;
	private final GrabThirdPartyStockDayPrice grabThirdPartyStockDayPrice;
	private final StockDayPriceService stockDayPriceService;

	private final StockWeekPriceService stockWeekPriceService;

	private final StockMonthPriceService stockMonthPriceService;

	private final RankStockChangeService rankStockChangeService;

	private final CacheOperatorService cacheOperatorService;

	private final MarginTradingAndShortSellingInfoService marginTradingAndShortSellingInfoService;

	private final CalculateCosineSimilarityVectorService calculateCosineSimilarityVectorService;

	private final StockIntradayPriceService stockIntradayPriceService;

	@Scheduled(cron = "0 50 18 * * ?", zone = "Asia/Taipei")
	public void updateStockIntradayPriceByThirdParty() throws Exception {
		doCalculateIntradayPrices("0");
		doCalculateIntradayPrices("1");
		doCalculateIntradayPrices("2");
	}

	@PostConstruct
	@Scheduled(cron = "0 30 15 * * ?", zone = "Asia/Taipei")
	public void updateStockDayPriceByThirdParty() throws Exception {
		List<String> tpexStockCodes = stockInfoService.getStockCodeByStockType("0").stream().filter(data -> {
			return !data.matches(".*[a-zA-Z].*");
		}).toList();
		List<String> twseStockCodes = stockInfoService.getStockCodeByStockType("1").stream().filter(data -> {
			return !data.matches(".*[a-zA-Z].*");
		}).toList();
		List<String> oesStockCodes = stockInfoService.getStockCodeByStockType("2").stream().filter(data -> {
			return !data.matches(".*[a-zA-Z].*");
		}).toList();
		List<String> allStockCodes = Lists.newArrayList();
		List<StockInfo> allStockInfos = stockInfoService.getAllStockInfo().stream().filter(data -> {
			return !data.getStockCode().matches(".*[a-zA-Z].*");
		}).toList();
		Map<String, List<StockInfo>> allStockInfoMap = allStockInfos.stream()
				.collect(Collectors.groupingBy(StockInfo::getStockCode));
		allStockCodes.addAll(tpexStockCodes);
		allStockCodes.addAll(twseStockCodes);
		allStockCodes.addAll(oesStockCodes);
		List<StockDayPrice> allStockDayPrices = Lists.newArrayList();
		allStockCodes.forEach(stockCode -> {
			allStockDayPrices.addAll(grabThirdPartyStockDayPrice.grabFromYahoo(stockCode));
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				log.warn(stockCode + e.getMessage(), e);
			}
		});
		stockDayPriceService.upsertBatch(allStockDayPrices);
		Date tradeDate = allStockDayPrices.stream().findFirst().get().getTradingDay();
		LocalDate tradeDateLdt = LocalDate.ofInstant(tradeDate.toInstant(), ZoneId.of("Asia/Taipei"));
		Integer years = tradeDateLdt.getYear();
		LocalDate tradeDateMinus365 = tradeDateLdt.minusDays(500);
		Instant instant = tradeDateMinus365.atStartOfDay(ZoneId.of("Asia/Taipei")).toInstant();
		Date tradeDateBefore365Days = Date.from(instant);
		List<StockDayPrice> stockDayPricesFor365Ds = stockDayPriceService
				.findByStockCodeAndTradingDayBeforEqualLimitTwoFourty(tradeDateBefore365Days, tradeDate);
		Map<String, List<StockDayPrice>> groupedStockDayPrices = stockDayPricesFor365Ds.stream().filter(data -> {
			java.util.Date tradingDay = data.getTradingDay();
			LocalDate dataTradeDateLdt = ((java.sql.Date) tradingDay).toLocalDate();
			return dataTradeDateLdt.compareTo(tradeDateLdt) < 0;
		}

		).collect(Collectors.groupingBy(StockDayPrice::getStockCode));

		stockDayPriceService.upsertBatch(allStockDayPrices);
		allStockDayPrices.stream().forEach(stockTwseDayPrice -> {
			if ("2454".equals(stockTwseDayPrice.getStockCode())) {
				log.info("2454");
			}
			List<StockDayPrice> allThisStockCodeDayPrices = Lists.newArrayList();
			allThisStockCodeDayPrices.add(stockTwseDayPrice);
			if (groupedStockDayPrices.containsKey(stockTwseDayPrice.getStockCode())) {
				List<StockDayPrice> stockDayPrices = groupedStockDayPrices.get(stockTwseDayPrice.getStockCode());
				allThisStockCodeDayPrices.addAll(stockDayPrices);
			}
			calculateRSVValueAndLimitDownUp(stockTwseDayPrice, allThisStockCodeDayPrices,
					allStockInfoMap.get(stockTwseDayPrice.getStockCode()).stream().findFirst().get().getStockType());
			stockTwseDayPrice.setMonthOfYear((stockTwseDayPrice.getTradingDay().getYear() + 1900) + "W"
					+ (stockTwseDayPrice.getTradingDay().getMonth() + 1));
			groupedStockDayPrices.put(stockTwseDayPrice.getStockCode(), allThisStockCodeDayPrices);
		});
		stockDayPriceService.upsertBatch(allStockDayPrices);
		List<StockDayPriceRank> allStockDayPriceRanks = Lists.newArrayList();
		allStockDayPrices.stream().forEach(stockTwseDayPrice -> {
			if (groupedStockDayPrices.containsKey(stockTwseDayPrice.getStockCode())) {
				if ("2454".equals(stockTwseDayPrice.getStockCode())) {
					log.info("2454");
				}
				StockDayPriceRank lastStockMonthPriceRank = stockDayPriceService
						.buildRanks(stockTwseDayPrice.getStockCode(), stockTwseDayPrice);
				allStockDayPriceRanks.add(lastStockMonthPriceRank);
			}
		});
		stockDayPriceService.bulkUpsertDayRankPrices(allStockDayPriceRanks, tradeDate);
		stockDayPriceService.updateRankNo();
		stockDayPriceService.deleteRankNoByStockCode();
		String targetWeek = allStockDayPriceRanks.get(0).getWeekOfYear();

		List<StockWeekPrice> stockWeekPriceForYears = stockWeekPriceService
				.findByYearBeforEqualLimitTwoFourty(String.valueOf(years - 6), String.valueOf(years));
		Map<String, List<StockWeekPrice>> groupedStockWeekPrice = stockWeekPriceForYears.stream()
				.collect(Collectors.groupingBy(StockWeekPrice::getStockCode));

		List<StockDayPrice> weekDayPrices = stockDayPriceService.findByWeekOfYear(targetWeek);

		Map<String, List<StockDayPrice>> stockWeekGroupMap = weekDayPrices.stream()
				.collect(Collectors.groupingBy(StockDayPrice::getStockCode));

		List<StockWeekPrice> allStockWeekPrices = new ArrayList<>();

		stockWeekGroupMap.forEach((stockCode, stockDayList) -> {

			// 過濾無效資料
			List<StockDayPrice> validDayPrices = stockDayList.stream()
					.filter(p -> StringUtils.isNotBlank(p.getOpeningPrice()) && !p.getOpeningPrice().contains("--"))
					.toList();

			if (validDayPrices.isEmpty()) {
				return;
			}

			// 依交易日排序（只做一次）
			List<StockDayPrice> sortedDays = validDayPrices.stream()
					.sorted(Comparator.comparing(StockDayPrice::getTradingDay)).toList();

			StockDayPrice firstDay = sortedDays.get(0);
			StockDayPrice lastDay = sortedDays.get(sortedDays.size() - 1);

			// 計算 high / low / volume
			BigDecimal high = null;
			BigDecimal low = null;
			int totalTradingVolume = 0;

			for (StockDayPrice d : sortedDays) {
				BigDecimal h = new BigDecimal(d.getHighPrice().replace(",", ""));
				BigDecimal l = new BigDecimal(d.getLowPrice().replace(",", ""));

				high = (high == null || h.compareTo(high) > 0) ? h : high;
				low = (low == null || l.compareTo(low) < 0) ? l : low;

				if (d.getTradingVolume() != null) {
					totalTradingVolume += Integer.parseInt(d.getTradingVolume());
				}
			}

			// 組裝唯一一筆 StockWeekPrice
			StockWeekPrice stockWeekPrice = new StockWeekPrice();
			stockWeekPrice.setStockCode(stockCode);
			stockWeekPrice.setWeekOfYear(firstDay.getWeekOfYear());
			stockWeekPrice.setOpeningPrice(new BigDecimal(firstDay.getOpeningPrice().replace(",", "")));
			stockWeekPrice.setClosingPrice(new BigDecimal(lastDay.getClosingPrice().replace(",", "")));
			stockWeekPrice.setHighPrice(high);
			stockWeekPrice.setLowPrice(low);
			stockWeekPrice.setTradingVolume(totalTradingVolume);
			stockWeekPrice.setFirstTradingDay(firstDay.getTradingDay());
			stockWeekPrice.setMonth(firstDay.getTradingDay().getMonth() + 1);
			stockWeekPrice.setYear(String.valueOf(firstDay.getTradingDay().getYear() + 1900));
			List<StockWeekPrice> stockWeekPrices = Lists.newArrayList();
			stockWeekPrices.add(stockWeekPrice);
			if (groupedStockWeekPrice.containsKey(stockCode)) {
				stockWeekPrices.addAll(groupedStockWeekPrice.get(stockCode));
			}
			calculateRSVValueAndLimitDownUp(stockWeekPrice, stockWeekPrices);
			allStockWeekPrices.add(stockWeekPrice);
			groupedStockWeekPrice.put(stockCode, stockWeekPrices);
		});
		stockWeekPriceService.batchInsertWeekPrices(allStockWeekPrices);

		List<StockWeekPriceRank> allStockWeekPriceRanks = Lists.newArrayList();
		allStockWeekPrices.stream().forEach(allStockWeekPrice -> {
			if (groupedStockWeekPrice.containsKey(allStockWeekPrice.getStockCode())) {
				StockWeekPriceRank stockMonthPriceRanks = stockWeekPriceService
						.buildRanks(allStockWeekPrice.getStockCode(), allStockWeekPrice);
				allStockWeekPriceRanks.add(stockMonthPriceRanks);
			}
		});
//		
		stockWeekPriceService.batchInsertWeekRankPrices(allStockWeekPriceRanks);
		stockWeekPriceService.updateRankNo();
		stockWeekPriceService.deleteByRankNoLessThanZero();

		// 取得欲處理的月份
		String targetMonth = allStockDayPrices.get(0).getMonthOfYear();
		List<StockMonthPrice> twoFourtyStockMonthPrices = stockMonthPriceService.findByMmonthOfYearAndDesc(targetMonth);
		Map<String, List<StockMonthPrice>> groupedStockMonthPrice = twoFourtyStockMonthPrices.stream()
				.collect(Collectors.groupingBy(StockMonthPrice::getStockCode));
		// 取得該月份所有日成交資料
		List<StockDayPrice> monthDayPrices = stockDayPriceService.findByMonthOfYear(targetMonth);

		// 依 stockCode 分組
		Map<String, List<StockDayPrice>> stockMonthGroupMap = monthDayPrices.stream()
				.collect(Collectors.groupingBy(StockDayPrice::getStockCode));

		// 用來 batch insert 的集合
		List<StockMonthPrice> allStockMonthPrices = new ArrayList<>();

		stockMonthGroupMap.forEach((stockCode, stockDayList) -> {
			if (groupedStockMonthPrice.containsKey(stockCode)) {
				// 過濾無效的開盤價資料
				List<StockDayPrice> validDayPrices = stockDayList.stream()
						.filter(p -> StringUtils.isNotBlank(p.getOpeningPrice()) && !p.getOpeningPrice().contains("--"))
						.toList();

				if (validDayPrices.isEmpty()) {
					return; // 該股票該月無有效資料
				}

				// 依交易日排序（只做一次）
				List<StockDayPrice> sortedDays = validDayPrices.stream()
						.sorted(Comparator.comparing(StockDayPrice::getTradingDay)).toList();

				StockDayPrice firstDay = sortedDays.get(0);
				StockDayPrice lastDay = sortedDays.get(sortedDays.size() - 1);

				// 計算最高、最低、成交量
				BigDecimal high = null;
				BigDecimal low = null;
				int totalTradingVolume = 0;

				for (StockDayPrice d : sortedDays) {
					BigDecimal h = new BigDecimal(d.getHighPrice().replace(",", ""));
					BigDecimal l = new BigDecimal(d.getLowPrice().replace(",", ""));

					high = (high == null || h.compareTo(high) > 0) ? h : high;
					low = (low == null || l.compareTo(low) < 0) ? l : low;

					if (d.getTradingVolume() != null) {
						totalTradingVolume += Integer.parseInt(d.getTradingVolume());
					}
				}

				// 組裝唯一一筆 StockMonthPrice
				StockMonthPrice stockMonthPrice = new StockMonthPrice();
				stockMonthPrice.setStockCode(stockCode);
				stockMonthPrice.setOpeningPrice(new BigDecimal(firstDay.getOpeningPrice().replace(",", "")));
				stockMonthPrice.setClosingPrice(new BigDecimal(lastDay.getClosingPrice().replace(",", "")));
				stockMonthPrice.setHighPrice(high);
				stockMonthPrice.setLowPrice(low);
				stockMonthPrice.setTradingVolume(totalTradingVolume);
				stockMonthPrice.setFirstTradingDay(firstDay.getTradingDay());

				int year = firstDay.getTradingDay().getYear() + 1900;
				int month = firstDay.getTradingDay().getMonth() + 1;

				stockMonthPrice.setYear(String.valueOf(year));
				stockMonthPrice.setMonth(month);
				stockMonthPrice.setMonthOfYear(year + "M" + month);
				List<StockMonthPrice> stockMonthPrices = Lists.newArrayList();
				stockMonthPrices.add(stockMonthPrice);
				if (groupedStockMonthPrice.containsKey(stockCode)) {
					stockMonthPrices.addAll(groupedStockMonthPrice.get(stockCode));
				}
				calculateRSVValueAndLimitDownUp(stockMonthPrice, stockMonthPrices);
				allStockMonthPrices.add(stockMonthPrice);
				groupedStockMonthPrice.put(stockCode, stockMonthPrices);
			}

		});

		// 真正合理的 batch insert
		stockMonthPriceService.batchInsertMonthPrices(allStockMonthPrices);

		List<StockMonthPriceRank> allStockMonthPriceRank = Lists.newArrayList();
		allStockMonthPrices.stream().forEach(stockMonthPrice -> {
			if (groupedStockMonthPrice.containsKey(stockMonthPrice.getStockCode())) {

				List<StockMonthPrice> singleStockStockMonthPrices = groupedStockMonthPrice
						.get(stockMonthPrice.getStockCode());
				List<StockMonthPrice> oriStockMonthPrices = Lists.newArrayList();
				oriStockMonthPrices.add(stockMonthPrice);
				oriStockMonthPrices.addAll(singleStockStockMonthPrices);
				StockMonthPriceRank stockMonthPriceRanks = stockMonthPriceService
						.buildRanks(stockMonthPrice.getStockCode(), oriStockMonthPrices);
				allStockMonthPriceRank.add(stockMonthPriceRanks);
			}
		});
		stockMonthPriceService.updateRankNo();
		stockMonthPriceService.batchInsertMonthRankPrices(allStockMonthPriceRank);
		stockMonthPriceService.deleteByRankNoLessThanZero();

		// 計算漲跌幅排名
		List<StockDayPrice> needRankTPEXs = allStockDayPrices.stream()
				.filter(data -> (!List.of("--", "---", "----").contains(data.getChange())
						&& tpexStockCodes.contains(data.getStockCode())
						&& ObjectUtils.isNotEmpty(data.getChangeRate())))
				.toList();
		List<StockDayPrice> needRankTWSEs = allStockDayPrices.stream()
				.filter(data -> (!List.of("--", "---", "----").contains(data.getChange())
						&& twseStockCodes.contains(data.getStockCode())
						&& ObjectUtils.isNotEmpty(data.getChangeRate())))
				.toList();
		List<StockDayPrice> needRankOESs = allStockDayPrices.stream()
				.filter(data -> (!List.of("--", "---", "----").contains(data.getChange())
						&& oesStockCodes.contains(data.getStockCode()) && ObjectUtils.isNotEmpty(data.getChangeRate())))
				.toList();
		rankStockChangeService.writeStockListToRedis(needRankTPEXs, allStockInfoMap, "TPEX");
		rankStockChangeService.writeStockListToRedis(needRankTWSEs, allStockInfoMap, "TWSE");
		rankStockChangeService.writeStockListToRedis(needRankOESs, allStockInfoMap, "OES");
		// 成交量排名
		rankStockChangeService.writeStockListTradingQuantityRankToRedis(
				allStockDayPrices.stream().filter(data -> (!List.of("--", "---", "----").contains(data.getChange())
						&& ObjectUtils.isNotEmpty(data.getChangeRate()))).toList(),
				allStockInfoMap);
		groupedStockDayPrices.entrySet().stream().filter(entry -> CollectionUtils.isNotEmpty(entry.getValue()))
				.forEach(entry -> {
					String stockCode = entry.getKey();
					List<StockDayPrice> stockDayPrices = entry.getValue();
					if (allStockInfoMap.containsKey(stockCode)) {
						List<StockDayPrice> cacheStockDayPrices = cacheOperatorService.getCompressedZSetAllScore(
								"ultraLongLivedCache", "stock:compressed:" + stockCode, StockDayPrice.class);
						if (CollectionUtils.isNotEmpty(cacheStockDayPrices)) {

							cacheOperatorService.upsertCompressedZSetSeries("ultraLongLivedCache",
									"stock:compressed:" + stockCode, stockDayPrices.stream().findFirst().get(),
									stockDayPrices.stream().findFirst().get().getTradingDay().getTime(),
									CacheOperatorService.DEFAULT_SERIES_MAX_SIZE);
						} else {
							cacheOperatorService.batchUpsertCompressedZSetSeries("ultraLongLivedCache",
									"stock:compressed:" + stockCode, stockDayPrices,
									stockDayPrice -> stockDayPrice.getTradingDay().getTime(),
									CacheOperatorService.DEFAULT_SERIES_MAX_SIZE);
						}
					} else {
						log.warn("stock_info missing : {}", stockCode);
					}
				});
		groupedStockWeekPrice.entrySet().stream().filter(entry -> CollectionUtils.isNotEmpty(entry.getValue()))
				.forEach(entry -> {
					String stockCode = entry.getKey();
					List<StockWeekPrice> stockWeekPrices = entry.getValue();
					if (allStockInfoMap.containsKey(stockCode)) {
						List<StockWeekPrice> cacheStockWeekPrices = cacheOperatorService.getCompressedZSetAllScore(
								"ultraLongLivedCache", "stock-weekly:compressed:" + stockCode, StockWeekPrice.class);
						if (CollectionUtils.isNotEmpty(cacheStockWeekPrices)) {

							cacheOperatorService.upsertCompressedZSetSeries("ultraLongLivedCache",
									"stock-weekly:compressed:" + stockCode, stockWeekPrices.stream().findFirst().get(),
									stockWeekPrices.stream().findFirst().get().getFirstTradingDay().getTime(),
									CacheOperatorService.DEFAULT_SERIES_MAX_SIZE);
						} else {
							cacheOperatorService.batchUpsertCompressedZSetSeries("ultraLongLivedCache",
									"stock-weekly:compressed:" + stockCode, stockWeekPrices,
									stockWeekPrice -> stockWeekPrice.getFirstTradingDay().getTime(),
									CacheOperatorService.DEFAULT_SERIES_MAX_SIZE);
						}
					} else {
						log.warn("stock_info missing : {}", stockCode);
					}
				});
		calculateCosineSimilarityVectorService.calculateAsDailyAspect();
	}

	private void calculateMissingIndicators(List<StockIntradayPrice> prices) {

		if (prices == null || prices.isEmpty()) {
			return;
		}

		// Must be oldest -> newest
		prices.sort(Comparator.comparing(StockIntradayPrice::getTradingTime));

		for (int i = 0; i < prices.size(); i++) {

			StockIntradayPrice current = prices.get(i);

			boolean needMa = current.getFiveMa() == null || current.getTenMa() == null || current.getTwentyMa() == null
					|| current.getSixtyMa() == null;

			boolean needKd = current.getLineRsvValue() == null || current.getLineKValue() == null
					|| current.getLineDValue() == null;

			if (needMa) {
				calculateMa(current, prices, i);
			}

			if (needKd) {
				calculateKd(current, prices, i);
			}
		}
	}

	private void calculateMa(StockIntradayPrice current, List<StockIntradayPrice> prices, int currentIndex) {

		if (current.getFiveMa() == null) {
			current.setFiveMa(calculateAverage(prices, currentIndex, 5));
		}

		if (current.getTenMa() == null) {
			current.setTenMa(calculateAverage(prices, currentIndex, 10));
		}

		if (current.getTwentyMa() == null) {
			current.setTwentyMa(calculateAverage(prices, currentIndex, 20));
		}

		if (current.getSixtyMa() == null) {
			current.setSixtyMa(calculateAverage(prices, currentIndex, 60));
		}
	}

	private Double calculateAverage(List<StockIntradayPrice> prices, int currentIndex, int period) {

		if (currentIndex + 1 < period) {
			return null;
		}

		double sum = 0D;

		for (int i = currentIndex - period + 1; i <= currentIndex; i++) {

			Double close = prices.get(i).getClosingPrice();

			if (close == null) {
				return null;
			}

			sum += close;
		}

		return roundToThreeDecimalPlaces(sum / period);
	}

	private void calculateKd(StockIntradayPrice current, List<StockIntradayPrice> prices, int currentIndex) {

		int period = 9;

		if (currentIndex + 1 < period) {
			return;
		}

		double highestHigh = Double.NEGATIVE_INFINITY;

		double lowestLow = Double.POSITIVE_INFINITY;

		for (int i = currentIndex - period + 1; i <= currentIndex; i++) {

			StockIntradayPrice item = prices.get(i);

			if (item.getHighPrice() == null || item.getLowPrice() == null) {

				return;
			}

			highestHigh = Math.max(highestHigh, item.getHighPrice());

			lowestLow = Math.min(lowestLow, item.getLowPrice());
		}

		Double close = current.getClosingPrice();

		if (close == null) {
			return;
		}

		double rsv;

		if (Double.compare(highestHigh, lowestLow) == 0) {

			rsv = 50D;

		} else {

			rsv = (close - lowestLow) / (highestHigh - lowestLow) * 100D;
		}

		rsv = roundToThreeDecimalPlaces(rsv);

		double previousK = 50D;
		double previousD = 50D;

		if (currentIndex > 0) {

			StockIntradayPrice previous = prices.get(currentIndex - 1);

			if (previous.getLineKValue() != null) {
				previousK = previous.getLineKValue();
			}

			if (previous.getLineDValue() != null) {
				previousD = previous.getLineDValue();
			}
		}

		double smoothingFactor = 1D / 3D;

		double k = previousK * (1D - smoothingFactor) + rsv * smoothingFactor;

		k = roundToThreeDecimalPlaces(k);

		double d = previousD * (1D - smoothingFactor) + k * smoothingFactor;

		d = roundToThreeDecimalPlaces(d);

		if (current.getLineRsvValue() == null) {
			current.setLineRsvValue(rsv);
		}

		if (current.getLineKValue() == null) {
			current.setLineKValue(k);
		}

		if (current.getLineDValue() == null) {
			current.setLineDValue(d);
		}
	}

	public void calculateRSVValueAndLimitDownUp(StockDayPrice stockTwseDayPrice,
			List<StockDayPrice> twoFourtyStockDayPrices, String stockType) {
		if (stockTwseDayPrice.getClosingPrice().equals("---") || stockTwseDayPrice.getClosingPrice().equals("----")
				|| stockTwseDayPrice.getClosingPrice().equals("--") || stockTwseDayPrice.getClosingPrice().equals("-")
				|| stockTwseDayPrice.getLowPrice().equals("--") || stockTwseDayPrice.getLowPrice().equals("---")
				|| stockTwseDayPrice.getLowPrice().equals("----")
				|| StringUtils.isBlank(stockTwseDayPrice.getClosingPrice())) {
			return;
		}

		Double upperLimitPrice = null;
		Double lowerLimitPrice = null;
		Double standarPrice = null;
		BigDecimal change;

		try {
			change = new BigDecimal(stockTwseDayPrice.getChange());
			standarPrice = Double.valueOf(stockTwseDayPrice.getClosingPrice().replace("+", "").replaceAll(",", ""))
					- change.doubleValue();
			upperLimitPrice = ChromeDriverUtils.calculateLimitPrice(standarPrice, true, stockType);
			lowerLimitPrice = ChromeDriverUtils.calculateLimitPrice(standarPrice, false, stockType);
		} catch (Exception e) {
			// 登櫃情況直接漲跌幅特殊計算
			log.error("Invalid change value, change={}, stockTwseDayPrice={}", stockTwseDayPrice.getChange(),
					stockTwseDayPrice, e);

			change = new BigDecimal(
					(Double.valueOf(stockTwseDayPrice.getClosingPrice().replace("+", "").replaceAll(",", "")) - Double
							.valueOf(stockTwseDayPrice.getOpeningPrice().replace("+", "").replaceAll(",", ""))));
			standarPrice = Double.valueOf(stockTwseDayPrice.getClosingPrice().replace("+", "").replaceAll(",", ""))
					- change.doubleValue();
			// 上下限值例外處理
			upperLimitPrice = ChromeDriverUtils.calculateLimitPrice(standarPrice, true, stockType) * 3;
			lowerLimitPrice = ChromeDriverUtils.calculateLimitPrice(standarPrice, false, stockType) * 3;
			BigDecimal tempChange = change.setScale(3, RoundingMode.HALF_UP);
			stockTwseDayPrice.setChange(tempChange.toString());
		}

		stockTwseDayPrice.setLimitDown(lowerLimitPrice.toString());
		twoFourtyStockDayPrices.get(0).setLimitDown(lowerLimitPrice.toString());
		stockTwseDayPrice.setLimitUp(upperLimitPrice.toString());
		twoFourtyStockDayPrices.get(0).setLimitUp(upperLimitPrice.toString());
		// 如果交易天數沒超過9天，則就不進行後續計算
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
		if (rsv.isNaN()) {
			rsv = (latestClosingPrice - (lowestLow - 0.001d)) / (highestHigh - (lowestLow - 0.001d)) * 100.0;
		}

		rsv = roundToThreeDecimalPlaces(rsv);
		Double previousK = StringUtils.isNotBlank(twoFourtyStockDayPrices.get(1).getLineKvalue())
				? Double.valueOf(twoFourtyStockDayPrices.get(1).getLineKvalue())
				: 50; // Default initial K value
		Double previousD = StringUtils.isNotBlank(twoFourtyStockDayPrices.get(1).getLineDvalue())
				? Double.valueOf(twoFourtyStockDayPrices.get(1).getLineDvalue())
				: 50;// Default initial D value
		double smoothingFactor = 1.0 / 3.0;

		Double k = previousK * (1 - smoothingFactor) + rsv * smoothingFactor;
		k = roundToThreeDecimalPlaces(k);
		Double d = previousD * (1 - smoothingFactor) + k * smoothingFactor;
		d = roundToThreeDecimalPlaces(d);

		stockTwseDayPrice.setLineDvalue(d.toString());
		twoFourtyStockDayPrices.get(0).setLineDvalue(d.toString());
		stockTwseDayPrice.setLineKvalue(k.toString());
		twoFourtyStockDayPrices.get(0).setLineKvalue(k.toString());
		stockTwseDayPrice.setLineRSVvalue(rsv.toString());
		twoFourtyStockDayPrices.get(0).setLineRSVvalue(rsv.toString());

		// 新增計算移動平均線 (MA) 的邏輯
		int[] maPeriods = { 240, 120, 60, 20, 10, 5 }; // 定義需要計算的移動平均線週期
		for (int maPeriod : maPeriods) {
			Double maValue = calculateMovingAverage(twoFourtyStockDayPrices, maPeriod);
			switch (maPeriod) {
			case 240:
				stockTwseDayPrice.setTwoFourtyDaysMa(maValue.toString()); // 設置 240 日 MA
				twoFourtyStockDayPrices.get(0).setTwoFourtyDaysMa(maValue.toString());
				break;
			case 120:
				stockTwseDayPrice.setOneTwentyDaysMa(maValue.toString()); // 設置 120 日 MA
				twoFourtyStockDayPrices.get(0).setOneTwentyDaysMa(maValue.toString());
				break;
			case 60:
				stockTwseDayPrice.setSixtyDaysMa(maValue.toString()); // 設置 60 日 MA
				twoFourtyStockDayPrices.get(0).setSixtyDaysMa(maValue.toString());
				break;
			case 20:
				stockTwseDayPrice.setTwentyDaysMa(maValue.toString()); // 設置 20 日 MA
				twoFourtyStockDayPrices.get(0).setTwentyDaysMa(maValue.toString());
				break;
			case 10:
				stockTwseDayPrice.setTenDaysMa(maValue.toString()); // 設置 10 日 MA
				twoFourtyStockDayPrices.get(0).setTenDaysMa(maValue.toString());
				break;
			case 5:
				stockTwseDayPrice.setFiveDaysMa(maValue.toString()); // 設置 5 日 MA
				twoFourtyStockDayPrices.get(0).setFiveDaysMa(maValue.toString());
				break;
			}
		}
	}

	private double roundToThreeDecimalPlaces(double value) {
		return new BigDecimal(value).setScale(3, RoundingMode.HALF_UP).doubleValue();
	}

	private double calculateMovingAverage(List<StockDayPrice> stockDayPrices, int period) {
		if (stockDayPrices.size() < period) {
			return 0.0; // 如果資料不足，返回 0
		}
		double average = stockDayPrices.stream().limit(period)
				.mapToDouble(stockDayPrice -> Double.valueOf(stockDayPrice.getClosingPrice().replaceAll(",", "")))
				.average().orElse(0.0);

		// 使用 BigDecimal 保留小數點第 4 位（四捨五入）
		return new BigDecimal(average).setScale(4, RoundingMode.HALF_UP).doubleValue();
	}

	public void calculateRSVValueAndLimitDownUp(StockMonthPrice stockMonthPrice,
			List<StockMonthPrice> twoFourtyStockMonthPrices) {

		if (twoFourtyStockMonthPrices.size() < 9) {
			return; // 如果不满足条件，返回 null
		}

		int period = 9;
		List<BigDecimal> closingPrices = twoFourtyStockMonthPrices.stream()
				.map(innerTwelfthStockDayPrice -> innerTwelfthStockDayPrice.getClosingPrice()).toList();
		List<BigDecimal> highPrices = twoFourtyStockMonthPrices.stream()
				.map(innerTwelfthStockDayPrice -> innerTwelfthStockDayPrice.getHighPrice()).toList();
		List<BigDecimal> lowPrices = twoFourtyStockMonthPrices.stream()
				.map(innerTwelfthStockDayPrice -> innerTwelfthStockDayPrice.getLowPrice()).toList();

		BigDecimal highestHigh = new BigDecimal(Double.MIN_VALUE);
		BigDecimal lowestLow = new BigDecimal(Double.MAX_VALUE);

		for (int i = 0; i < period; i++) {
			highestHigh = highestHigh.max(highPrices.get(i));
			lowestLow = lowestLow.min(lowPrices.get(i));
		}

		BigDecimal latestClosingPrice = closingPrices.get(0);
		BigDecimal rsv = (latestClosingPrice.subtract(lowestLow))
				.divide(highestHigh.subtract(lowestLow), 6, RoundingMode.HALF_UP).multiply(new BigDecimal("100.0"));
		BigDecimal previousK = ObjectUtils.isNotEmpty(twoFourtyStockMonthPrices.get(1).getLineKValue())
				? twoFourtyStockMonthPrices.get(1).getLineKValue()
				: new BigDecimal("50"); // Default initial K value
		BigDecimal previousD = ObjectUtils.isNotEmpty(twoFourtyStockMonthPrices.get(1).getLineDValue())
				? twoFourtyStockMonthPrices.get(1).getLineDValue()
				: new BigDecimal("50");// Default initial D value

		BigDecimal smoothingFactor = BigDecimal.ONE.divide(new BigDecimal("3.0"), 4, RoundingMode.HALF_UP);
		BigDecimal k = previousK.multiply(BigDecimal.ONE.subtract(smoothingFactor)).add(rsv.multiply(smoothingFactor));
		k = k.setScale(3, RoundingMode.HALF_UP);
		BigDecimal d = previousD.multiply(BigDecimal.ONE.subtract(smoothingFactor)).add(k.multiply(smoothingFactor));
		d = d.setScale(3, RoundingMode.HALF_UP);

		stockMonthPrice.setLineDValue(d);
		twoFourtyStockMonthPrices.get(0).setLineDValue(d);
		stockMonthPrice.setLineKValue(k);
		twoFourtyStockMonthPrices.get(0).setLineDValue(d);
		stockMonthPrice.setLineRsvValue(rsv);
		twoFourtyStockMonthPrices.get(0).setLineRsvValue(rsv);
		// 新增計算移動平均線 (MA) 的邏輯
		int[] maPeriods = { 240, 120, 60, 20, 10, 5 }; // 定義需要計算的移動平均線週期
		for (int maPeriod : maPeriods) {
			BigDecimal maValue = calculateStockMonthPriceMovingAverage(twoFourtyStockMonthPrices, maPeriod);
			switch (maPeriod) {
			case 240:
				stockMonthPrice.setTwoFourtyMonthMa(maValue); // 設置 240 日 MA
				twoFourtyStockMonthPrices.get(0).setTwoFourtyMonthMa(maValue);
				break;
			case 120:
				stockMonthPrice.setOneTwentyMonthMa(maValue); // 設置 120 日 MA
				twoFourtyStockMonthPrices.get(0).setOneTwentyMonthMa(maValue);
				break;
			case 60:
				stockMonthPrice.setSixtyMonthMa(maValue); // 設置 60 日 MA
				twoFourtyStockMonthPrices.get(0).setSixtyMonthMa(maValue);
				break;
			case 20:
				stockMonthPrice.setTwentyMonthMa(maValue); // 設置 20 日 MA
				twoFourtyStockMonthPrices.get(0).setTwentyMonthMa(maValue);
				break;
			case 10:
				stockMonthPrice.setTenMonthMa(maValue); // 設置 10 日 MA
				twoFourtyStockMonthPrices.get(0).setTenMonthMa(maValue);
				break;
			case 5:
				stockMonthPrice.setFiveMonthMa(maValue); // 設置 5 日 MA
				twoFourtyStockMonthPrices.get(0).setFiveMonthMa(maValue);
				break;
			}
		}
	}

//	@PostConstruct
	@Scheduled(cron = "0 50 21 * * ?", zone = "Asia/Taipei")
	public void grabMarginTradingAndShortSellingInfo() throws Exception {
		List<String> tpexStockCodes = stockInfoService.getStockCodeByStockType("0").stream().filter(data -> {
			return !data.matches(".*[a-zA-Z].*");
		}).toList();
		List<String> twseStockCodes = stockInfoService.getStockCodeByStockType("1").stream().filter(data -> {
			return !data.matches(".*[a-zA-Z].*");
		}).toList();
		List<String> allStockCodes = Lists.newArrayList();
		List<MarginTradingAndShortSellingInfo> allMarginTradingAndShortSellingInfos = Lists.newArrayList();
		allStockCodes.addAll(tpexStockCodes);
		allStockCodes.addAll(twseStockCodes);
		allStockCodes.stream().forEach(stockCode -> {
			List<MarginTradingAndShortSellingInfo> singleStockCodeTop10MarginTradingAndShortSellingInfos = grabThirdPartyStockDayPrice
					.grabMarginTradingAndShortSellingInfoFromYahoo(stockCode);
			allMarginTradingAndShortSellingInfos.addAll(singleStockCodeTop10MarginTradingAndShortSellingInfos);
			try {
				Thread.sleep(1500);
			} catch (InterruptedException e) {
				log.warn(e.getMessage(), e);
				throw new RuntimeException(e);
			}
		});
//		Date currentTradeDate = stockDayPriceService.getCurrentTradeDate();
//		LocalDate tradeDateLdt = LocalDate.ofInstant(currentTradeDate.toInstant(), ZoneId.of("Asia/Taipei"));
//		Integer years = tradeDateLdt.getYear();
//		LocalDate tradeDateMinus365 = tradeDateLdt.minusDays(500);
//		Instant instant = tradeDateMinus365.atStartOfDay(ZoneId.of("Asia/Taipei")).toInstant();
//		Date tradeDateBefore365Days = Date.from(instant);
//		List<MarginTradingAndShortSellingInfo> stockDayPricesFor365Ds = marginTradingAndShortSellingInfoService
//				.findByTradingDayBeforEqualLimitTwoFourty(tradeDateBefore365Days, currentTradeDate);
		Map<String, List<MarginTradingAndShortSellingInfo>> groupedMarginTradingAndShortSellingInfos = allMarginTradingAndShortSellingInfos
				.stream()
//				.filter(data -> {
//			java.util.Date tradingDay = data.getTradingDay();
//			LocalDate dataTradeDateLdt = ((java.sql.Date) tradingDay).toLocalDate();
//			return dataTradeDateLdt.compareTo(tradeDateLdt) <= 0;
//		})
				.collect(Collectors.groupingBy(MarginTradingAndShortSellingInfo::getStockCode));
		marginTradingAndShortSellingInfoService
				.bulkUpsertMarginTradingAndShortSellingInfo(allMarginTradingAndShortSellingInfos);
		groupedMarginTradingAndShortSellingInfos.entrySet().forEach(entry -> {
			List<MarginTradingAndShortSellingInfo> singleMarginTradingAndShortSellingInfos = entry.getValue();
			if (CollectionUtils.isNotEmpty(singleMarginTradingAndShortSellingInfos)) {
				cacheOperatorService.batchUpsertCompressedZSetSeries("ultraLongLivedCache",
						"marginTrading:compressed:" + entry.getKey(), singleMarginTradingAndShortSellingInfos,
						marginTradingAndShortSellingInfo -> marginTradingAndShortSellingInfo.getTradingDay().getTime(),
						CacheOperatorService.DEFAULT_SERIES_MAX_SIZE);
			}
		});
	}

	public void calculateRSVValueAndLimitDownUp(StockWeekPrice stockWeekPrice,
			List<StockWeekPrice> twoFourtyStockWeekPrices) {
		if (twoFourtyStockWeekPrices.size() < 9) {
			return; // 如果不满足条件，返回 null
		}

		int period = 9;
		List<BigDecimal> closingPrices = twoFourtyStockWeekPrices.stream()
				.map(innerTwelfthStockDayPrice -> innerTwelfthStockDayPrice.getClosingPrice()).toList();
		List<BigDecimal> highPrices = twoFourtyStockWeekPrices.stream()
				.map(innerTwelfthStockDayPrice -> innerTwelfthStockDayPrice.getHighPrice()).toList();
		List<BigDecimal> lowPrices = twoFourtyStockWeekPrices.stream()
				.map(innerTwelfthStockDayPrice -> innerTwelfthStockDayPrice.getLowPrice()).toList();

		BigDecimal highestHigh = new BigDecimal(Double.MIN_VALUE);
		BigDecimal lowestLow = new BigDecimal(Double.MAX_VALUE);

		for (int i = 0; i < period; i++) {
			highestHigh = highestHigh.max(highPrices.get(i));
			lowestLow = lowestLow.min(lowPrices.get(i));
		}

		BigDecimal latestClosingPrice = closingPrices.get(0);
		BigDecimal rsv = (latestClosingPrice.subtract(lowestLow))
				.divide(highestHigh.subtract(lowestLow), 6, RoundingMode.HALF_UP).multiply(new BigDecimal("100.0"));
		BigDecimal previousK = ObjectUtils.isNotEmpty(twoFourtyStockWeekPrices.get(1).getLineKValue())
				? twoFourtyStockWeekPrices.get(1).getLineKValue()
				: new BigDecimal("50"); // Default initial K value
		BigDecimal previousD = ObjectUtils.isNotEmpty(twoFourtyStockWeekPrices.get(1).getLineDValue())
				? twoFourtyStockWeekPrices.get(1).getLineDValue()
				: new BigDecimal("50");// Default initial D value

		BigDecimal smoothingFactor = BigDecimal.ONE.divide(new BigDecimal("3.0"), 4, RoundingMode.HALF_UP);
		BigDecimal k = previousK.multiply(BigDecimal.ONE.subtract(smoothingFactor)).add(rsv.multiply(smoothingFactor));
		k = k.setScale(3, RoundingMode.HALF_UP);
		BigDecimal d = previousD.multiply(BigDecimal.ONE.subtract(smoothingFactor)).add(k.multiply(smoothingFactor));
		d = d.setScale(3, RoundingMode.HALF_UP);

		stockWeekPrice.setLineDValue(d);
		twoFourtyStockWeekPrices.get(0).setLineDValue(d);
		stockWeekPrice.setLineKValue(k);
		twoFourtyStockWeekPrices.get(0).setLineKValue(k);
		stockWeekPrice.setLineRsvValue(rsv);
		twoFourtyStockWeekPrices.get(0).setLineRsvValue(rsv);
		// 新增計算移動平均線 (MA) 的邏輯
		int[] maPeriods = { 240, 120, 60, 20, 10, 5 }; // 定義需要計算的移動平均線週期
		for (int maPeriod : maPeriods) {
			BigDecimal maValue = calculateStockWeekPriceMovingAverage(twoFourtyStockWeekPrices, maPeriod);
			switch (maPeriod) {
			case 240:
				stockWeekPrice.setTwoFourtyWeekMa(maValue); // 設置 240 日 MA
				twoFourtyStockWeekPrices.get(0).setTwoFourtyWeekMa(maValue);
				break;
			case 120:
				stockWeekPrice.setOneTwentyWeekMa(maValue); // 設置 120 日 MA
				twoFourtyStockWeekPrices.get(0).setOneTwentyWeekMa(maValue);
				break;
			case 60:
				stockWeekPrice.setSixtyWeekMa(maValue); // 設置 60 日 MA
				twoFourtyStockWeekPrices.get(0).setSixtyWeekMa(maValue);
				break;
			case 20:
				stockWeekPrice.setTwentyWeekMa(maValue); // 設置 20 日 MA
				twoFourtyStockWeekPrices.get(0).setTwentyWeekMa(maValue);
				break;
			case 10:
				stockWeekPrice.setTenWeekMa(maValue); // 設置 10 日 MA
				twoFourtyStockWeekPrices.get(0).setTenWeekMa(maValue);
				break;
			case 5:
				stockWeekPrice.setFiveWeekMa(maValue); // 設置 5 日 MA
				twoFourtyStockWeekPrices.get(0).setFiveWeekMa(maValue);
				break;
			}
		}
	}

	// 計算股票週期移動平均價
	private BigDecimal calculateStockWeekPriceMovingAverage(List<StockWeekPrice> stockDayPrices, int period) {
		if (stockDayPrices.size() < period) {
			return BigDecimal.ZERO; // 如果資料不足，返回 0
		}
		BigDecimal total = stockDayPrices.stream().limit(period).map(stockDayPrice -> stockDayPrice.getClosingPrice())
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		// 計算平均值並保留小數點第 4 位（四捨五入）
		return total.divide(new BigDecimal(period), 4, RoundingMode.HALF_UP);
	}

	// 計算股票週期移動平均價
	private BigDecimal calculateStockMonthPriceMovingAverage(List<StockMonthPrice> stockMonthPrice, int period) {
		if (stockMonthPrice.size() < period) {
			return BigDecimal.ZERO; // 如果資料不足，返回 0
		}
		BigDecimal total = stockMonthPrice.stream().limit(period).map(stockDayPrice -> stockDayPrice.getClosingPrice())
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		// 計算平均值並保留小數點第 4 位（四捨五入）
		return total.divide(new BigDecimal(period), 4, RoundingMode.HALF_UP);
	}

	private void doCalculateIntradayPrices(String stockType) throws Exception {

	    // =====================================================
	    // 1. 取得股票清單
	    // =====================================================

	    List<String> stockCodes = stockInfoService
	            .getStockCodeByStockType(stockType)
	            .stream()
	            .filter(StringUtils::isNotBlank)
	            .filter(code -> !code.matches(".*[a-zA-Z].*"))
	            .distinct()
	            .toList();

	    if (stockCodes.isEmpty()) {

	        log.info(
	                "No stock codes found, stockType={}",
	                stockType
	        );

	        return;
	    }

	    log.info(
	            "Start calculating intraday prices, stockType={}, totalStocks={}",
	            stockType,
	            stockCodes.size()
	    );

	    int successCount = 0;
	    int failedCount = 0;
	    int emptyCount = 0;

	    // =====================================================
	    // 2. 每批處理 50 檔
	    // =====================================================

	    for (int start = 0;
	         start < stockCodes.size();
	         start += STOCK_BATCH_SIZE) {

	        int end = Math.min(
	                start + STOCK_BATCH_SIZE,
	                stockCodes.size()
	        );

	        List<String> batchStockCodes =
	                stockCodes.subList(start, end);

	        log.info(
	                "Start intraday batch, range={}~{}, total={}",
	                start + 1,
	                end,
	                stockCodes.size()
	        );

	        // =================================================
	        // 3. 只取得本批股票的 DB 歷史資料
	        // =================================================

	        Map<String, List<StockIntradayPrice>> fiveDbGroup =
	                stockIntradayPriceService
	                        .getTopByPeriodAndStockCodes(
	                                "5m",
	                                batchStockCodes,
	                                INDICATOR_HISTORY_SIZE
	                        )
	                        .stream()
	                        .collect(Collectors.groupingBy(
	                                StockIntradayPrice::getStockCode
	                        ));

	        Map<String, List<StockIntradayPrice>> sixtyDbGroup =
	                stockIntradayPriceService
	                        .getTopByPeriodAndStockCodes(
	                                "60m",
	                                batchStockCodes,
	                                INDICATOR_HISTORY_SIZE
	                        )
	                        .stream()
	                        .collect(Collectors.groupingBy(
	                                StockIntradayPrice::getStockCode
	                        ));

	        // =================================================
	        // 4. 本批資料
	        // =================================================

	        List<StockIntradayPrice> batchUpsertPrices =
	                new ArrayList<>();

	        Map<String, List<StockIntradayPrice>> batchFiveCache =
	                new LinkedHashMap<>();

	        Map<String, List<StockIntradayPrice>> batchSixtyCache =
	                new LinkedHashMap<>();

	        int batchSuccessCount = 0;
	        int batchFailedCount = 0;

	        // =================================================
	        // 5. 處理本批股票
	        // =================================================

	        for (String stockCode : batchStockCodes) {

	            try {

	                // =========================================
	                // 5.1 Yahoo 抓取 5m K 線
	                // =========================================

	                List<StockIntradayPrice> fiveMinutes =
	                        grabThirdPartyStockDayPrice
	                                .grabIntradayFromYahoo(
	                                        stockCode,
	                                        "5m"
	                                );

	                if (fiveMinutes == null || fiveMinutes.isEmpty()) {

	                    log.warn(
	                            "Yahoo returned empty intraday prices, stockCode={}",
	                            stockCode
	                    );

	                    emptyCount++;

	                    continue;
	                }

	                // =========================================
	                // 5.2 整理 Yahoo 5m
	                // =========================================

	                fiveMinutes = normalizeIntradayPrices(
	                        fiveMinutes,
	                        FIVE_MINUTE_MAX_SIZE
	                );

	                if (fiveMinutes.isEmpty()) {

	                    emptyCount++;

	                    continue;
	                }

	                // =========================================
	                // 5.3 聚合成 60m
	                // =========================================

	                List<StockIntradayPrice> sixtyMinutes =
	                        grabThirdPartyStockDayPrice
	                                .aggregateToHourly(fiveMinutes);

	                sixtyMinutes = normalizeIntradayPrices(
	                        sixtyMinutes,
	                        SIXTY_MINUTE_MAX_SIZE
	                );

	                // =========================================
	                // 5.4 整理 5m 與計算技術指標
	                // =========================================

	                List<StockIntradayPrice> fivePrices =
	                        prepareIntradayPrices(
	                                fiveMinutes,
	                                fiveDbGroup.get(stockCode),
	                                FIVE_MINUTE_MAX_SIZE
	                        );

	                // =========================================
	                // 5.5 整理 60m 與計算技術指標
	                // =========================================

	                List<StockIntradayPrice> sixtyPrices =
	                        sixtyMinutes.isEmpty()
	                                ? new ArrayList<>()
	                                : prepareIntradayPrices(
	                                        sixtyMinutes,
	                                        sixtyDbGroup.get(stockCode),
	                                        SIXTY_MINUTE_MAX_SIZE
	                                );

	                // =========================================
	                // 5.6 累積 PostgreSQL Upsert 資料
	                // =========================================

	                batchUpsertPrices.addAll(fivePrices);
	                batchUpsertPrices.addAll(sixtyPrices);

	                // =========================================
	                // 5.7 暫存 Redis 資料
	                // =========================================

	                batchFiveCache.put(
	                        stockCode,
	                        fivePrices
	                );

	                if (!sixtyPrices.isEmpty()) {

	                    batchSixtyCache.put(
	                            stockCode,
	                            sixtyPrices
	                    );

	                }

	                successCount++;
	                batchSuccessCount++;

	                log.debug(
	                        "Stock intraday calculated, stockCode={}, fiveSize={}, sixtySize={}",
	                        stockCode,
	                        fivePrices.size(),
	                        sixtyPrices.size()
	                );

	            } catch (Exception e) {
	                failedCount++;
	                batchFailedCount++;

	                log.error(
	                        "Failed to calculate intraday prices, stockCode={}",
	                        stockCode,
	                        e
	                );

	            } finally {

	                // =========================================
	                // Yahoo request interval
	                // =========================================

	                if (!Thread.currentThread().isInterrupted()) {

	                    try {

	                        Thread.sleep(
	                                YAHOO_REQUEST_INTERVAL_MS
	                        );

	                    } catch (InterruptedException e) {

	                        Thread.currentThread().interrupt();

	                        throw e;

	                    }

	                }

	            }

	        }

	        // =================================================
	        // 6. PostgreSQL 批次寫入
	        // =================================================

	        if (batchUpsertPrices.isEmpty()) {

	            log.warn(
	                    "Skip empty intraday batch, start={}, end={}",
	                    start,
	                    end
	            );

	            continue;
	        }

	        log.info(
	                "Start PostgreSQL bulk upsert, records={}",
	                batchUpsertPrices.size()
	        );

	        stockIntradayPriceService
	                .bulkUpsertIntradayPrices(
	                        batchUpsertPrices
	                );

	        log.info(
	                "PostgreSQL bulk upsert completed, records={}",
	                batchUpsertPrices.size()
	        );

	        // =================================================
	        // 7. PostgreSQL 成功後更新 Redis
	        // =================================================

	        // 5m Redis
	        batchFiveCache.forEach((stockCode, prices) -> {

	            try {

	                updateIntradayRedis(
	                        stockCode,
	                        "5m",
	                        prices
	                );

	            } catch (Exception e) {

	                log.error(
	                        "Failed to update 5m Redis, stockCode={}",
	                        stockCode,
	                        e
	                );

	            }

	        });

	        // 60m Redis
	        batchSixtyCache.forEach((stockCode, prices) -> {

	            try {

	                updateIntradayRedis(
	                        stockCode,
	                        "60m",
	                        prices
	                );

	            } catch (Exception e) {

	                log.error(
	                        "Failed to update 60m Redis, stockCode={}",
	                        stockCode,
	                        e
	                );

	            }

	        });

	        // =================================================
	        // 8. 本批完成
	        // =================================================

	        log.info(
	                "Intraday batch completed, processed={}/{}, success={}, failed={}, upsertRecords={}",
	                end,
	                stockCodes.size(),
	                batchSuccessCount,
	                batchFailedCount,
	                batchUpsertPrices.size()
	        );

	    }

	    // =====================================================
	    // 9. 全部完成
	    // =====================================================

	    log.info(
	            "Intraday calculation completed, stockType={}, success={}, failed={}, empty={}",
	            stockType,
	            successCount,
	            failedCount,
	            emptyCount
	    );

	}
	
	private List<StockIntradayPrice> normalizeIntradayPrices(
	        List<StockIntradayPrice> prices,
	        int maxSize) {

	    if (prices == null || prices.isEmpty()) {
	        return new ArrayList<>();
	    }

	    // 按交易時間排序；相同時間以後出現的資料為準
	    Map<LocalDateTime, StockIntradayPrice> uniquePrices =
	            new TreeMap<>();

	    for (StockIntradayPrice price : prices) {

	        if (price == null || price.getTradingTime() == null) {
	            continue;
	        }

	        uniquePrices.put(
	                price.getTradingTime(),
	                price
	        );

	    }

	    List<StockIntradayPrice> sortedPrices =
	            new ArrayList<>(uniquePrices.values());

	    int fromIndex = Math.max(
	            0,
	            sortedPrices.size() - maxSize
	    );

	    return new ArrayList<>(
	            sortedPrices.subList(
	                    fromIndex,
	                    sortedPrices.size()
	            )
	    );

	}
	
	private List<StockIntradayPrice> prepareIntradayPrices(
	        List<StockIntradayPrice> yahooPrices,
	        List<StockIntradayPrice> dbPrices,
	        int maxSize) {

	    if (yahooPrices == null || yahooPrices.isEmpty()) {
	        return new ArrayList<>();
	    }

	    // ==========================================
	    // 1. 合併 DB 與 Yahoo
	    // ==========================================

	    Map<LocalDateTime, StockIntradayPrice> mergedPrices =
	            new TreeMap<>();

	    if (dbPrices != null) {

	        for (StockIntradayPrice price : dbPrices) {

	            if (price != null && price.getTradingTime() != null) {

	                mergedPrices.put(
	                        price.getTradingTime(),
	                        price
	                );

	            }

	        }

	    }

	    // Yahoo 資料優先，覆蓋 DB 同時間 K 線
	    for (StockIntradayPrice price : yahooPrices) {

	        if (price != null && price.getTradingTime() != null) {

	            mergedPrices.put(
	                    price.getTradingTime(),
	                    price
	            );

	        }

	    }

	    // ==========================================
	    // 2. 按時間由舊到新排列
	    // ==========================================

	    List<StockIntradayPrice> calculatedPrices =
	            new ArrayList<>(mergedPrices.values());

	    // ==========================================
	    // 3. 計算技術指標
	    // ==========================================

	    calculateMissingIndicators(calculatedPrices);

	    // ==========================================
	    // 4. 保留最新指定筆數
	    // ==========================================

	    return normalizeIntradayPrices(
	            calculatedPrices,
	            maxSize
	    );
	}
	private void updateIntradayRedis(
	        String stockCode,
	        String period,
	        List<StockIntradayPrice> prices) {

	    if (StringUtils.isBlank(stockCode)
	            || prices == null
	            || prices.isEmpty()) {
	        return;
	    }

	    int maxSize;

	    switch (period) {

	        case "5m":
	            maxSize = FIVE_MINUTE_MAX_SIZE;
	            break;

	        case "60m":
	            maxSize = SIXTY_MINUTE_MAX_SIZE;
	            break;

	        default:
	            throw new IllegalArgumentException(
	                    "Unsupported intraday period: " + period
	            );

	    }

	    List<StockIntradayPrice> latestPrices =
	            normalizeIntradayPrices(prices, maxSize);

	    String redisKey =
	            "stock-intraday:" + period + ":" + stockCode;

	    // 寫入最新 Snapshot
	    cacheOperatorService.putSnapshotDataListAtomic(
	            "ultraLongLivedCache",
	            redisKey,
	            latestPrices
	    );

	    // 保留最新兩個 Snapshot 版本
	    cacheOperatorService.cleanupOldSnapshots(
	            "ultraLongLivedCache",
	            redisKey,
	            2
	    );

	}
}
