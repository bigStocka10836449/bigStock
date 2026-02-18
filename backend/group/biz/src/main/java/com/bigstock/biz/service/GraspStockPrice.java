package com.bigstock.biz.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import com.bigstock.biz.utils.ChromeDriverUtils;
import com.bigstock.sharedComponent.entity.MarginTradingAndShortSellingInfo;
import com.bigstock.sharedComponent.entity.SecuritiesFirmsDayOperate;
import com.bigstock.sharedComponent.entity.StockDayPrice;
import com.bigstock.sharedComponent.entity.StockDayPriceRank;
import com.bigstock.sharedComponent.entity.StockMonthPrice;
import com.bigstock.sharedComponent.entity.StockMonthPriceRank;
import com.bigstock.sharedComponent.entity.StockWeekPrice;
import com.bigstock.sharedComponent.entity.StockWeekPriceRank;
import com.bigstock.sharedComponent.entity.TmpExDividendsExRightInfo;
import com.bigstock.sharedComponent.entity.TradeVolumeInfo;
import com.bigstock.sharedComponent.service.MarginTradingAndShortSellingInfoService;
import com.bigstock.sharedComponent.service.SecuritiesFirmsDayOperateService;
import com.bigstock.sharedComponent.service.StockDayPriceService;
import com.bigstock.sharedComponent.service.StockExchangeDetailService;
import com.bigstock.sharedComponent.service.StockInfoService;
import com.bigstock.sharedComponent.service.StockMonthPriceService;
import com.bigstock.sharedComponent.service.StockWeekPriceService;
import com.bigstock.sharedComponent.service.TmpExDividendsExRightInfoService;
import com.bigstock.sharedComponent.service.TradeVolumeInfoService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;

import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class GraspStockPrice {

	private final SecuritiesFirmsDayOperateService securitiesFirmsDayOperateService;

	private final StockInfoService stockInfoService;

	private final StockExchangeDetailService stockExchangeDetailService;

	private final StockDayPriceService stockDayPriceService;

	private final MarginTradingAndShortSellingInfoService marginTradingAndShortSellingInfoServices;

	private final TradeVolumeInfoService tradeVolumeInfoService;

	private final TmpExDividendsExRightInfoService tmpExDividendsExRightInfoService;

	private final StockWeekPriceService stockWeekPriceService;

	private final StockMonthPriceService stockMonthPriceService;

	public void grepSecuritiesFirmsDayOperate(JSONObject json)
			throws InterruptedException, RestClientException, URISyntaxException, JsonProcessingException, JSONException, ParseException {
		List<Object> datas = json.getJSONArray("batch").toList();
		for(Object  jdata : datas) {
			ObjectMapper mapper = new ObjectMapper();
			String jsonString = mapper.writeValueAsString((HashMap) jdata);
			JSONObject dataJson = new JSONObject(jsonString);
			SimpleDateFormat sim = new SimpleDateFormat("yyyy-MM-dd");
			Date tradingDate = sim.parse(dataJson.get("tradingDate").toString());;
			// 讀取CSV文件
			List<SecuritiesFirmsDayOperate> securitiesFirmsDayOperates = dataJson.getJSONArray("data").toList().stream()
					.map(jm -> {
						SecuritiesFirmsDayOperate securitiesFirmsDayOperate = new SecuritiesFirmsDayOperate();
						try {
							String innerJsonString = mapper.writeValueAsString((HashMap) jm);
							JSONObject jsb = new JSONObject(innerJsonString);
						String stockCode = dataJson.getString("stockCode").toString();
						securitiesFirmsDayOperate.setPrice(jsb.getString("價格"));
						securitiesFirmsDayOperate.setSeq(jsb.getInt("序號"));
						securitiesFirmsDayOperate
						.setStockCode(stockCode.contains("_") ? stockCode.split("_")[0] : stockCode);
						securitiesFirmsDayOperate.setSecuritiesFirms( jsb.getString("券商"));
						
						securitiesFirmsDayOperate.setStockBuyAmount(
								Long.valueOf( jsb.getString("買進股數").trim().replace(",", "")));
						securitiesFirmsDayOperate.setStockSellAmount(Long.valueOf( jsb.getString("賣出股數").trim().replace(",", "")));
							securitiesFirmsDayOperate.setTradingDate(sim.parse(dataJson.get("tradingDate").toString()));
						} catch (JSONException | ParseException |JsonProcessingException e) {
							log.error(e.getMessage(),e);
							return null;
						} 
						return securitiesFirmsDayOperate;
					}).filter(data -> !Optional.ofNullable(data).isEmpty()).sorted((x1, x2) -> x1.getSeq().compareTo(x2.getSeq())).toList();
			if(CollectionUtils.isNotEmpty(securitiesFirmsDayOperates)) {
				securitiesFirmsDayOperateService.batchReplace(dataJson.getString("stockCode").toString(), tradingDate,securitiesFirmsDayOperates);
			}
		}
	}
	
	private List<JSONObject> convertTESECSVtoJSON(List<String[]> csvData) {
		// 跳過前 3 行表頭數據
		// 創建 JSON 數組
		List<JSONObject> jsonArray = Lists.newArrayList();
		// 跳過前 3 行表頭數據
		for (int i = 3; i < csvData.size(); i++) {
			String[] row = csvData.get(i);

			// 檢查第一部分（列 1 到列 5 是否有數據）
			if (row[0] != null && !row[0].trim().isEmpty()) {
				JSONObject jsonObject = new JSONObject();
				jsonObject.put("序號", Integer.parseInt(row[0].trim()));
				jsonObject.put("券商", row[1].trim());
				jsonObject.put("價格", row[2].trim());
				jsonObject.put("買進股數", row[3].trim());
				jsonObject.put("賣出股數", row[4].trim());

				jsonArray.add(jsonObject); // 將第一部分的 JSON 對象添加到列表中
			}

			// 檢查第二部分（列 7 到列 11 是否有數據）
			if (row.length > 6 && row[6] != null && !row[6].trim().isEmpty()) {
				JSONObject jsonObject = new JSONObject();
				jsonObject.put("序號", Integer.parseInt(row[6].trim())); // 從第 7 列開始
				jsonObject.put("券商", row[7].trim());
				jsonObject.put("價格", row[8].trim());
				jsonObject.put("買進股數", row[9].trim());
				jsonObject.put("賣出股數", row[10].trim());

				jsonArray.add(jsonObject); // 將第二部分的 JSON 對象添加到列表中
			}
		}
		return jsonArray;
	}

	// 每天下午5點更新
//	@Scheduled(cron = "${schedule.task.scheduling.cron.expression.grasp-stock-price}")
	// 每周日早上8点触发更新
	@Scheduled(cron = "${schedule.task.scheduling.cron.expression.grasp-stock-price}", zone= "Asia/Taipei")
	@PostConstruct
	public void updateStockDayPrice() throws Exception {
		// 先抓DB裡面全部的代號資料

		List<StockDayPrice> stockTpexEmergingStockPrices = ChromeDriverUtils.graspTpexEmergingStockDayPrice();

		List<StockDayPrice> stockTpexDayPrices = ChromeDriverUtils
				.graspTpexDayPrice("https://www.tpex.org.tw/openapi/v1/tpex_mainboard_quotes");

////		   List<StockDayPrice> singleStockDayPrices = entry.getValue();

		List<TradeVolumeInfo> stockTpexTradeVolumeInfos = ChromeDriverUtils
				.graspTpexTtradeVolume("https://www.tpex.org.tw/openapi/v1/tpex_volume_rank");
		Map<String, TradeVolumeInfo> stockTpexTradeVolumeInfosMap = stockTpexTradeVolumeInfos.stream()
				.collect(Collectors.toMap(TradeVolumeInfo::getStockCode, tradeVolumeInfo -> tradeVolumeInfo));
		Date tradeDate = stockTpexDayPrices.stream().findFirst().get().getTradingDay();
		List<StockDayPrice> stockTwseDayPrices = ChromeDriverUtils
				.graspTwseDayPrice("https://openapi.twse.com.tw/v1/exchangeReport/STOCK_DAY_ALL", tradeDate);
		List<TradeVolumeInfo> twseTradeVolumeInfos = stockTwseDayPrices.stream().map(stockDayPrice -> {
			TradeVolumeInfo tradeVolumeInfo = new TradeVolumeInfo();
			tradeVolumeInfo.setTradingDay(stockDayPrice.getTradingDay());
			tradeVolumeInfo.setStockCode(stockDayPrice.getStockCode());
			tradeVolumeInfo.setTradeVolume(stockDayPrice.getTradingVolume());
			return tradeVolumeInfo;
		}).toList();
		List<TmpExDividendsExRightInfo> tmpExDividendsExRightInfos = ChromeDriverUtils
				.grepTmpExDividendsExRightInfo(tradeDate);
		Map<String, List<TmpExDividendsExRightInfo>> groupedtmpExDividendsExRightInfos =
				tmpExDividendsExRightInfos.stream()
			        .collect(Collectors.groupingBy(TmpExDividendsExRightInfo::getStockCode));
		boolean isExDivideOrRightHappen = (stockTpexDayPrices.stream()
				.anyMatch(stockTpexDayPrice -> (stockTpexDayPrice.getChange().contains("除息")
						|| stockTpexDayPrice.getChange().contains("除權")))
				|| stockTwseDayPrices.stream()
						.anyMatch(stockTwseDayPrice -> stockTwseDayPrice.getChange().contains("X")));
		if (isExDivideOrRightHappen) {
			List<TmpExDividendsExRightInfo> todateTmpExDividendsExRightInfo = tmpExDividendsExRightInfoService
					.findByTradingDay(tradeDate);
			if (todateTmpExDividendsExRightInfo.isEmpty()) {
				tmpExDividendsExRightInfoService.saveAll(tmpExDividendsExRightInfos);
			}
		}



		stockDayPriceService.upsertBatch(stockTpexEmergingStockPrices);
		stockDayPriceService.upsertBatch(stockTpexDayPrices);
		LocalDate tradeDateLdt = LocalDate.ofInstant(tradeDate.toInstant(), ZoneId.of("Asia/Taipei"));
		Integer years = tradeDateLdt.getYear();
		LocalDate tradeDateMinus365 = tradeDateLdt.minusDays(500); 
		Instant instant = tradeDateMinus365.atStartOfDay( ZoneId.of("Asia/Taipei")).toInstant();
		Date tradeDateBefore365Days = Date.from(instant);
		List<StockDayPrice> stockDayPricesFor365Ds = stockDayPriceService.findByStockCodeAndTradingDayBeforEqualLimitTwoFourty(tradeDateBefore365Days, tradeDate);
		Map<String, List<StockDayPrice>> groupedStockDayPrices =
				stockDayPricesFor365Ds.stream()
			        .collect(Collectors.groupingBy(StockDayPrice::getStockCode));
		stockTpexDayPrices.stream().forEach(stockTpexDayPrice -> {
			if(!groupedStockDayPrices.containsKey(stockTpexDayPrice.getStockCode())) {
				return;
			}
			if("5340".equals(stockTpexDayPrice.getStockCode())) {
				log.info("5340");
			}
			List<StockDayPrice> stockDayPrices = groupedStockDayPrices.get(stockTpexDayPrice.getStockCode());
			List<StockDayPrice> allThisStockCodeDayPrices = Lists.newArrayList();
			allThisStockCodeDayPrices.add(stockTpexDayPrice);
			allThisStockCodeDayPrices.addAll(stockDayPrices);
			calculateRSVValueAndLimitDownUp(stockTpexDayPrice,allThisStockCodeDayPrices,groupedtmpExDividendsExRightInfos);
			stockTpexDayPrice.setMonthOfYear((stockTpexDayPrice.getTradingDay().getYear() + 1900) + "W"
					+ (stockTpexDayPrice.getTradingDay().getMonth() + 1));
			if (stockTpexTradeVolumeInfosMap.containsKey(stockTpexDayPrice.getStockCode())) {
				TradeVolumeInfo tradeVolumeInfo = stockTpexTradeVolumeInfosMap.get(stockTpexDayPrice.getStockCode());
				stockTpexDayPrice.setTradingVolume(tradeVolumeInfo.getTradeVolume());
			}
			groupedStockDayPrices.put(stockTpexDayPrice.getStockCode(), allThisStockCodeDayPrices);
		});
		stockTpexEmergingStockPrices.stream().forEach(stockTpexDayPrice -> {
			if(!groupedStockDayPrices.containsKey(stockTpexDayPrice.getStockCode())) {
				return;
			}
			List<StockDayPrice> stockDayPrices = groupedStockDayPrices.get(stockTpexDayPrice.getStockCode());
			List<StockDayPrice> allThisStockCodeDayPrices = Lists.newArrayList();
			allThisStockCodeDayPrices.add(stockTpexDayPrice);
			allThisStockCodeDayPrices.addAll(stockDayPrices);
			calculateRSVValueAndLimitDownUp(stockTpexDayPrice,allThisStockCodeDayPrices,groupedtmpExDividendsExRightInfos);
			stockTpexDayPrice.setMonthOfYear((stockTpexDayPrice.getTradingDay().getYear() + 1900) + "W"
					+ (stockTpexDayPrice.getTradingDay().getMonth() + 1));
			if("5340".equals(stockTpexDayPrice.getStockCode())) {
				log.info("5340");
			}
			if (stockTpexTradeVolumeInfosMap.containsKey(stockTpexDayPrice.getStockCode())) {
				TradeVolumeInfo tradeVolumeInfo = stockTpexTradeVolumeInfosMap.get(stockTpexDayPrice.getStockCode());
				stockTpexDayPrice.setTradingVolume(tradeVolumeInfo.getTradeVolume());
			}
			groupedStockDayPrices.put(stockTpexDayPrice.getStockCode(), allThisStockCodeDayPrices);
		});
		List<StockDayPriceRank> allStockDayPriceRanks = Lists.newArrayList();
		List<StockWeekPriceRank> allStockWeekPriceRanks = Lists.newArrayList();
		stockDayPriceService.upsertBatch(stockTpexEmergingStockPrices);
		stockDayPriceService.upsertBatch(stockTpexDayPrices);
		stockDayPriceService.upsertBatch(stockTwseDayPrices);
		stockTpexDayPrices.stream().forEach(stockTwseDayPrice -> {
			if(groupedStockDayPrices.containsKey(stockTwseDayPrice.getStockCode())) {
				StockDayPriceRank lastStockMonthPriceRank = stockDayPriceService.buildRanks(stockTwseDayPrice.getStockCode(), stockTwseDayPrice);
				allStockDayPriceRanks.add(lastStockMonthPriceRank);
			}
		});
		stockTwseDayPrices.stream().forEach(stockTwseDayPrice -> {
			if("5340".equals(stockTwseDayPrice.getStockCode())) {
				log.info("5340");
			}
			if(!groupedStockDayPrices.containsKey(stockTwseDayPrice.getStockCode())) {
				return;
			}
			List<StockDayPrice> stockDayPrices = groupedStockDayPrices.get(stockTwseDayPrice.getStockCode());
			List<StockDayPrice> allThisStockCodeDayPrices = Lists.newArrayList();
			allThisStockCodeDayPrices.add(stockTwseDayPrice);
			allThisStockCodeDayPrices.addAll(stockDayPrices);
			calculateRSVValueAndLimitDownUp(stockTwseDayPrice,allThisStockCodeDayPrices,groupedtmpExDividendsExRightInfos);
			stockTwseDayPrice.setMonthOfYear((stockTwseDayPrice.getTradingDay().getYear() + 1900) + "W"
					+ (stockTwseDayPrice.getTradingDay().getMonth() + 1));
			if (stockTpexTradeVolumeInfosMap.containsKey(stockTwseDayPrice.getStockCode())) {
				TradeVolumeInfo tradeVolumeInfo = stockTpexTradeVolumeInfosMap.get(stockTwseDayPrice.getStockCode());
				stockTwseDayPrice.setTradingVolume(tradeVolumeInfo.getTradeVolume());
			}
			groupedStockDayPrices.put(stockTwseDayPrice.getStockCode(), allThisStockCodeDayPrices);
		});
		stockDayPriceService.upsertBatch(stockTwseDayPrices);
		stockTwseDayPrices.stream().forEach(stockTwseDayPrice -> {
			if(groupedStockDayPrices.containsKey(stockTwseDayPrice.getStockCode())) {
				if ("2330".equals(stockTwseDayPrice.getStockCode())) {
					log.info("2330");
				}
				StockDayPriceRank lastStockMonthPriceRank = stockDayPriceService.buildRanks(stockTwseDayPrice.getStockCode(), stockTwseDayPrice);
				allStockDayPriceRanks.add(lastStockMonthPriceRank);
			}
		});
		stockDayPriceService.bulkUpsertDayRankPrices(allStockDayPriceRanks, tradeDate);
		stockDayPriceService.updateRankNo();
		stockDayPriceService.deleteRankNoByStockCode();
		tradeVolumeInfoService.saveAll(stockTpexTradeVolumeInfos);
		tradeVolumeInfoService.saveAll(twseTradeVolumeInfos);
//		.findByStartDateAndEndDate(new Date("2024/05/01"), new Date("2024/12/20")).stream()
		
		
		String targetWeek = stockTpexDayPrices.get(0).getWeekOfYear();
		
		List<StockWeekPrice> stockWeekPriceForYears = stockWeekPriceService.findByYearBeforEqualLimitTwoFourty(String.valueOf(years-6) , String.valueOf(years));
		Map<String, List<StockWeekPrice>> groupedStockWeekPrice =
				stockWeekPriceForYears.stream()
			        .collect(Collectors.groupingBy(StockWeekPrice::getStockCode));
		
		List<StockDayPrice> weekDayPrices =
		    stockDayPriceService.findByWeekOfYear(targetWeek);

		Map<String, List<StockDayPrice>> stockWeekGroupMap =
		    weekDayPrices.stream()
		        .collect(Collectors.groupingBy(StockDayPrice::getStockCode));

		List<StockWeekPrice> allStockWeekPrices = new ArrayList<>();

		stockWeekGroupMap.forEach((stockCode, stockDayList) -> {
			
		    // 過濾無效資料
		    List<StockDayPrice> validDayPrices =
		        stockDayList.stream()
		            .filter(p ->
		                StringUtils.isNotBlank(p.getOpeningPrice())
		                && !p.getOpeningPrice().contains("--"))
		            .toList();

		    if (validDayPrices.isEmpty()) {
		        return;
		    }

		    // 依交易日排序（只做一次）
		    List<StockDayPrice> sortedDays =
		        validDayPrices.stream()
		            .sorted(Comparator.comparing(StockDayPrice::getTradingDay))
		            .toList();

		    StockDayPrice firstDay = sortedDays.get(0);
		    StockDayPrice lastDay  = sortedDays.get(sortedDays.size() - 1);

		    // 計算 high / low / volume
		    BigDecimal high = null;
		    BigDecimal low  = null;
		    int totalTradingVolume = 0;

		    for (StockDayPrice d : sortedDays) {
		        BigDecimal h = new BigDecimal(d.getHighPrice().replace(",", ""));
		        BigDecimal l = new BigDecimal(d.getLowPrice().replace(",", ""));

		        high = (high == null || h.compareTo(high) > 0) ? h : high;
		        low  = (low  == null || l.compareTo(low)  < 0) ? l : low;

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
		    stockWeekPrices.addAll(groupedStockWeekPrice.get(stockCode));
		    calculateRSVValueAndLimitDownUp(stockWeekPrice, stockWeekPrices);
		    allStockWeekPrices.add(stockWeekPrice);
		    groupedStockWeekPrice.put(stockCode, stockWeekPrices);
		});
		stockWeekPriceService.batchInsertWeekPrices(allStockWeekPrices);
		
		
		allStockWeekPrices.stream().forEach(allStockWeekPrice ->{
			if(groupedStockWeekPrice.containsKey(allStockWeekPrice.getStockCode())) {
				StockWeekPriceRank stockMonthPriceRanks = stockWeekPriceService.buildRanks(allStockWeekPrice.getStockCode(), allStockWeekPrice);
				allStockWeekPriceRanks.add(stockMonthPriceRanks);
			}
		});
//		
		stockWeekPriceService.batchInsertWeekRankPrices(allStockWeekPriceRanks);
		stockWeekPriceService.updateRankNo();
		stockWeekPriceService.deleteByRankNoLessThanZero();


		// 取得欲處理的月份
		String targetMonth = stockTpexDayPrices.get(0).getMonthOfYear();
		List<StockMonthPrice> twoFourtyStockMonthPrices = stockMonthPriceService
				.findByMmonthOfYearAndDesc(targetMonth);
		Map<String, List<StockMonthPrice>> groupedStockMonthPrice =
				twoFourtyStockMonthPrices.stream()
			        .collect(Collectors.groupingBy(StockMonthPrice::getStockCode));
		// 取得該月份所有日成交資料
		List<StockDayPrice> monthDayPrices =
		    stockDayPriceService.findByMonthOfYear(targetMonth);

		// 依 stockCode 分組
		Map<String, List<StockDayPrice>> stockMonthGroupMap =
		    monthDayPrices.stream()
		        .collect(Collectors.groupingBy(StockDayPrice::getStockCode));

		// 用來 batch insert 的集合
		List<StockMonthPrice> allStockMonthPrices = new ArrayList<>();

		stockMonthGroupMap.forEach((stockCode, stockDayList) -> {
			if(groupedStockMonthPrice.containsKey(stockCode)) {
				 // 過濾無效的開盤價資料
			    List<StockDayPrice> validDayPrices =
			        stockDayList.stream()
			            .filter(p ->
			                StringUtils.isNotBlank(p.getOpeningPrice())
			                && !p.getOpeningPrice().contains("--"))
			            .toList();

			    if (validDayPrices.isEmpty()) {
			        return; // 該股票該月無有效資料
			    }

			    // 依交易日排序（只做一次）
			    List<StockDayPrice> sortedDays =
			        validDayPrices.stream()
			            .sorted(Comparator.comparing(StockDayPrice::getTradingDay))
			            .toList();

			    StockDayPrice firstDay = sortedDays.get(0);
			    StockDayPrice lastDay  = sortedDays.get(sortedDays.size() - 1);

			    // 計算最高、最低、成交量
			    BigDecimal high = null;
			    BigDecimal low  = null;
			    int totalTradingVolume = 0;
			    
			    for (StockDayPrice d : sortedDays) {
			        BigDecimal h = new BigDecimal(d.getHighPrice().replace(",", ""));
			        BigDecimal l = new BigDecimal(d.getLowPrice().replace(",", ""));

			        high = (high == null || h.compareTo(high) > 0) ? h : high;
			        low  = (low  == null || l.compareTo(low)  < 0) ? l : low;

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

			    int year  = firstDay.getTradingDay().getYear() + 1900;
			    int month = firstDay.getTradingDay().getMonth() + 1;

			    stockMonthPrice.setYear(String.valueOf(year));
			    stockMonthPrice.setMonth(month);
			    stockMonthPrice.setMonthOfYear(year + "M" + month);
			    List<StockMonthPrice> stockMonthPrices = Lists.newArrayList();
			    stockMonthPrices.add(stockMonthPrice);
			    stockMonthPrices.addAll(groupedStockMonthPrice.get(stockCode));
			    calculateRSVValueAndLimitDownUp(stockMonthPrice,stockMonthPrices);
			    allStockMonthPrices.add(stockMonthPrice);
			    groupedStockMonthPrice.put(stockCode, stockMonthPrices);
			}
		   
		});

		//  真正合理的 batch insert
		stockMonthPriceService.batchInsertMonthPrices(allStockMonthPrices);
		
		List<StockMonthPriceRank> allStockMonthPriceRank = Lists.newArrayList();
		allStockMonthPrices.stream().forEach(stockMonthPrice ->{
			if(groupedStockMonthPrice.containsKey(stockMonthPrice.getStockCode())) {
				
				List<StockMonthPrice> singleStockStockMonthPrices = groupedStockMonthPrice.get(stockMonthPrice.getStockCode());
				List<StockMonthPrice> oriStockMonthPrices = Lists.newArrayList();
				oriStockMonthPrices.add(stockMonthPrice);
				oriStockMonthPrices.addAll(singleStockStockMonthPrices);
				StockMonthPriceRank stockMonthPriceRanks = stockMonthPriceService.buildRanks(stockMonthPrice.getStockCode(), oriStockMonthPrices);
				allStockMonthPriceRank.add(stockMonthPriceRanks);
			}
		});
		stockMonthPriceService.updateRankNo();
		stockMonthPriceService.batchInsertMonthRankPrices(allStockMonthPriceRank);
		stockMonthPriceService.deleteByRankNoLessThanZero();
	}

	@Scheduled(cron = "${schedule.task.scheduling.cron.expression.update-margin-trading}", zone= "Asia/Taipei")
	@Transactional
//	@PostConstruct
	public void updateMarginTradingAndShortSellingInfo() throws RestClientException, URISyntaxException,
			JsonMappingException, JsonProcessingException, InterruptedException {
		// 先抓DB裡面全部的代號資料
		List<MarginTradingAndShortSellingInfo> stockTpexarginTradingAndShortSellingInfo = ChromeDriverUtils
				.graspTpexMarginTradingAndShortSellingInfo(
						"https://www.tpex.org.tw/openapi/v1/tpex_mainboard_margin_balance");

		List<StockDayPrice> stockTpexDayPrices = ChromeDriverUtils
				.graspTpexDayPrice("https://www.tpex.org.tw/openapi/v1/tpex_mainboard_quotes");

		Date tradeDate = stockTpexDayPrices.stream().findFirst().get().getTradingDay();

		List<MarginTradingAndShortSellingInfo> stockTwseDayPrices = ChromeDriverUtils
				.graspTwseMarginTradingAndShortSellingInfo("https://openapi.twse.com.tw/v1/exchangeReport/MI_MARGN",
						tradeDate);
		marginTradingAndShortSellingInfoServices.saveAll(stockTpexarginTradingAndShortSellingInfo);
		marginTradingAndShortSellingInfoServices.saveAll(stockTwseDayPrices);
		log.info("finsh sync stockDayPrice");
	}

//
	public static String removeFileExtension(String fileName) {
		// 找到最後一個點的位置
		int lastDotIndex = fileName.lastIndexOf('.');

		// 如果找到了點，截取之前的部分；如果沒有點，則返回原始文件名
		if (lastDotIndex != -1) {
			return fileName.substring(0, lastDotIndex);
		} else {
			return fileName; // 沒有擴展名
		}
	}

	public void calculateRSVValueAndLimitDownUp(StockDayPrice stockTwseDayPrice, List<StockDayPrice> twoFourtyStockDayPrices, Map<String, List<TmpExDividendsExRightInfo>> groupedtmpExDividendsExRightInfos ) {
		if (stockTwseDayPrice.getClosingPrice().equals("---") || stockTwseDayPrice.getClosingPrice().equals("----")
				|| stockTwseDayPrice.getClosingPrice().equals("--") || stockTwseDayPrice.getLowPrice().equals("--")
				|| stockTwseDayPrice.getLowPrice().equals("---") || stockTwseDayPrice.getLowPrice().equals("----")
				|| StringUtils.isBlank(stockTwseDayPrice.getClosingPrice())) {
			return;
		}
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
		String tmpChage = stockTwseDayPrice.getChange().trim().replace("+", "").replaceAll(",", "");
		boolean isNeedSpecialDeal = groupedtmpExDividendsExRightInfos.containsKey(stockTwseDayPrice.getStockCode());
		if (tmpChage.equals("0.00")) {
			tmpChage = "0";
		}
		if (tmpChage.contains("除息") || tmpChage.contains("除權") || tmpChage.contains("X")) {
			Optional<StockDayPrice> stockDayPriceOp = stockDayPriceService.findByStockCodeAndTradingDayBeforLimitOne(
					stockTwseDayPrice.getStockCode(), stockTwseDayPrice.getTradingDay());
			if (stockDayPriceOp.isPresent()) {
				tmpChage = String.valueOf(Double
						.valueOf(stockTwseDayPrice.getClosingPrice().replace("+", "").replaceAll(",", ""))
						- Double.valueOf(stockDayPriceOp.get().getClosingPrice().replace("+", "").replaceAll(",", "")));
			} else {
				tmpChage = "0";
			}
		}
		Double standarPrice = null;
		Double upperLimitPrice = null;
		Double lowerLimitPrice = null;
		if (isNeedSpecialDeal) {
			if (groupedtmpExDividendsExRightInfos.get(stockTwseDayPrice.getStockCode()).stream().findFirst().isPresent()) {
				TmpExDividendsExRightInfo tmpExDividendsExRightInfo = groupedtmpExDividendsExRightInfos.get(stockTwseDayPrice.getStockCode()).stream().findFirst().get();
				upperLimitPrice = Double.valueOf(tmpExDividendsExRightInfo.getLimitUp().replaceAll(",", ""));
				lowerLimitPrice = Double.valueOf(tmpExDividendsExRightInfo.getLimitDown().replaceAll(",", ""));
				standarPrice = new BigDecimal(
						tmpExDividendsExRightInfo.getReferencePrice().replace(",", StringUtils.EMPTY)).doubleValue();
			} else {
				return;
			}
		} else {
			standarPrice = Double.valueOf(stockTwseDayPrice.getClosingPrice().replace("+", "").replaceAll(",", ""))
					- new BigDecimal(tmpChage).doubleValue();
			upperLimitPrice = ChromeDriverUtils.calculateLimitPrice(standarPrice, true);
			lowerLimitPrice = ChromeDriverUtils.calculateLimitPrice(standarPrice, false);
		}
		Double closingPrice = Double.valueOf(stockTwseDayPrice.getClosingPrice().replace("+", "").replaceAll(",", ""));
		stockTwseDayPrice.setLimitDown(lowerLimitPrice.toString());
		twoFourtyStockDayPrices.get(0).setLimitDown(lowerLimitPrice.toString());
		stockTwseDayPrice.setLimitUp(upperLimitPrice.toString());
		twoFourtyStockDayPrices.get(0).setLimitUp(upperLimitPrice.toString());
		Double rate = ((closingPrice - standarPrice) / standarPrice) * 100;
		rate = Math.round(rate * 100.0) / 100.0;
		stockTwseDayPrice.setChangeRate(rate);
		twoFourtyStockDayPrices.get(0).setChangeRate(rate);
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

	public void calculateRSVValueAndLimitDownUp(StockMonthPrice stockMonthPrice, List<StockMonthPrice> twoFourtyStockMonthPrices) {

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

	public void calculateRSVValueAndLimitDownUp(StockWeekPrice stockWeekPrice, List<StockWeekPrice> twoFourtyStockWeekPrices) {
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

	private double roundToThreeDecimalPlaces(double value) {
		return new BigDecimal(value).setScale(3, RoundingMode.HALF_UP).doubleValue();
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
}
