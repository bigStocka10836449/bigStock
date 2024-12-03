package com.bigstock.schedule.service;

import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import com.bigstock.schedule.utils.ChromeDriverUtils;
import com.bigstock.sharedComponent.entity.MarginTradingAndShortSellingInfo;
import com.bigstock.sharedComponent.entity.StockDayPrice;
import com.bigstock.sharedComponent.entity.TradeVolumeInfo;
import com.bigstock.sharedComponent.service.MarginTradingAndShortSellingInfoService;
import com.bigstock.sharedComponent.service.SecuritiesFirmsDayOperateService;
import com.bigstock.sharedComponent.service.StockDayPriceService;
import com.bigstock.sharedComponent.service.StockExchangeDetailService;
import com.bigstock.sharedComponent.service.StockInfoService;
import com.bigstock.sharedComponent.service.TradeVolumeInfoService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.common.collect.Maps;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class GraspStockPrice {
	@Value("${schedule.chromeDriverPath.windows.active}")
	private boolean windowsActive;

	@Value("${schedule.chromeDriverPath.windows.path}")
	private String windowsChromeDriverPath;

	@Value("${schedule.chromeDriverPath.linux.active}")
	private boolean linuxActive;

	@Value("${schedule.chromeDriverPath.linux.driver-path}")
	private String linuxChromeDriverPath;

	@Value("${schedule.chromeDriverPath.download-path}")
	private String downloadPath;

	@Value("${schedule.bpython-url}")
	private String bpythonUrl;

	@Value("${schedule.credentials-pathl}")
	private String credentialsPath;

	private final GraspHistoryStockPrice graspHistoryStockPrice;

	private final SecuritiesFirmsDayOperateService securitiesFirmsDayOperateService;

	private final StockInfoService stockInfoService;

	private final StockExchangeDetailService stockExchangeDetailService;

	private final StockDayPriceService stockDayPriceService;

	private final MarginTradingAndShortSellingInfoService marginTradingAndShortSellingInfoServices;

	private final TradeVolumeInfoService tradeVolumeInfoService;

//	private final RedissonClient redissonClient;
//	
//	private static final String GRASPSTOCK_REDIS_ENABLE_KEY = "bstock:schedule:GraspStock:enable";
//	private static final String GRASPSTOCK_REDIS_ENABLE_IS_SHUTDOWN_KEY = "bstock:schedule:GraspStock:isSutDown";

	// 爬蟲暫時不做，取消抓取蠟燭圖方法，帶未來真的規模擴大，再走實際正常串接作法
//	@PostConstruct
//	public void grepCandlestickChart() throws InterruptedException, JsonMappingException, JsonProcessingException, RestClientException, URISyntaxException {
//		Date tradeDate = graspHistoryStockPrice.getLastTradeDate();
//		List<String> stockCodes = ChromeDriverUtils
//				.getStockInfoByTdccApi("https://openapi.tdcc.com.tw/v1/opendata/1-2").stream().filter(stockInfo -> StringUtils.isNotBlank(stockInfo.getStockType()))
//				.filter(stockInfo -> List.of("1", "0").contains(stockInfo.getStockType()))
//				.map(stockInfo -> stockInfo.getStockCode()).toList();
//		ChromeDriverUtils.grepCanvas(windowsActive ? windowsChromeDriverPath : linuxChromeDriverPath, stockCodes, tradeDate, stockExchangeDetailService);
//	}

	// 每天下午5點更新
//	@Scheduled(cron = "${schedule.task.scheduling.cron.expression.grasp-stock-price}")
	// 每周日早上8点触发更新
//	@Scheduled(cron = "${schedule.task.scheduling.cron.expression.grasp-stock-price}")
//	@Transactional
	@PostConstruct
	public void updateStockDayPrice() throws RestClientException, URISyntaxException, JsonMappingException,
			JsonProcessingException, InterruptedException {
		// 先抓DB裡面全部的代號資料
		List<StockDayPrice> stockTpexDayPrices = ChromeDriverUtils
				.graspTpexDayPrice("https://www.tpex.org.tw/openapi/v1/tpex_mainboard_quotes");

//		   List<StockDayPrice> singleStockDayPrices = entry.getValue();

		stockTpexDayPrices.stream().forEach(stockDayPrice -> {
			calculateRSVValueAndLimitDownUp(stockDayPrice);
		});

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

		stockDayPriceService.saveAll(stockTpexDayPrices);
		stockTpexDayPrices.stream().forEach(stockTpexDayPrice -> {
			calculateRSVValueAndLimitDownUp(stockTpexDayPrice);
		});
		stockDayPriceService.saveAll(stockTpexDayPrices);
		stockDayPriceService.saveAll(stockTwseDayPrices);
		stockTwseDayPrices.stream().forEach(stockTwseDayPrice -> {
			
		});
		stockDayPriceService.saveAll(stockTwseDayPrices);
		tradeVolumeInfoService.saveAll(stockTpexTradeVolumeInfos);
		tradeVolumeInfoService.saveAll(twseTradeVolumeInfos);
//		
//		List<StockDayPrice> stockDayPrices = 
//		stockDayPriceService.findPreviousFiftyTowDaysBeforeLastestDayInfo("2330");
//		
//		List<StockDayPrice> lastStockDayPrices = stockDayPrices.subList(stockDayPrices.size() - 10 ,stockDayPrices.size()-1);
//		Date lastestTradingDate = lastStockDayPrices.get(lastStockDayPrices.size() -1).getTradingDay();
//		Date olddestTradingDate = lastStockDayPrices.get(0).getTradingDay();
//		Map<String, List<StockDayPrice>> stockDayPricesMap = stockDayPriceService
//				.findByStartDateAndEndDate(olddestTradingDate, lastestTradingDate).stream()
//				.collect(Collectors.groupingBy(StockDayPrice::getStockCode));
//		List<StockDayPrice> lastStockDayPriceWithCalculateRsvKDs = stockDayPricesMap.entrySet().stream()
//			    .map(entry -> {
//			        List<StockDayPrice> singleStockDayPrices = entry.getValue();
//			        if (singleStockDayPrices.size() < 9) {
//			            return null; // 如果不满足条件，返回 null
//			        }
//
//			        int period = 9;
//			        List<Double> closingPrices = singleStockDayPrices.stream()
//			                .map(stockDayPrice -> Double.valueOf(stockDayPrice.getClosingPrice().replaceAll(",", "")))
//			                .toList();
//			        List<Double> highPrices = singleStockDayPrices.stream()
//			                .map(stockDayPrice -> Double.valueOf(stockDayPrice.getHighPrice().replaceAll(",", "")))
//			                .toList();
//			        List<Double> lowPrices = singleStockDayPrices.stream()
//			                .map(stockDayPrice -> Double.valueOf(stockDayPrice.getLowPrice().replaceAll(",", "")))
//			                .toList();
//
//			        double highestHigh = Double.MIN_VALUE;
//			        double lowestLow = Double.MAX_VALUE;
//
//			        for (int i = 0; i < period; i++) {
//			            highestHigh = Math.max(highestHigh, highPrices.get(i));
//			            lowestLow = Math.min(lowestLow, lowPrices.get(i));
//			        }
//
//			        double latestClosingPrice = closingPrices.get(period - 1);
//			        Double rsv = (latestClosingPrice - lowestLow) / (highestHigh - lowestLow) * 100.0;
//
//			        double previousK = 50.0; // Default initial K value
//			        double previousD = 50.0; // Default initial D value
//			        double smoothingFactor = 1.0 / 3.0;
//
//			        Double k = previousK * (1 - smoothingFactor) + rsv * smoothingFactor;
//			        Double d = previousD * (1 - smoothingFactor) + k * smoothingFactor;
//
//			        StockDayPrice lastStockDayPrice = singleStockDayPrices.get(singleStockDayPrices.size() - 1);
//			        lastStockDayPrice.setLineDvalue(d.toString());
//			        lastStockDayPrice.setLineKvalue(k.toString());
//			        lastStockDayPrice.setLineRSVvalue(rsv.toString());
//
//			        return lastStockDayPrice; 
//			    })
//			    .filter(Objects::nonNull) 
//			    .collect(Collectors.toList());
//		stockDayPriceService.saveAll(lastStockDayPriceWithCalculateRsvKDs);
//		List<StockDayPrice>  stockps = stockDayPriceService.findByStartDateAndEndDate(new Date("2024/05/01"), new Date("2024/12/01"));
//		stockps.stream().forEach(stockDayPrice ->{
//			if(stockDayPrice.getClosingPrice().contains("-") || stockDayPrice.getChange().contains("X")) {
//				return;
//			}
//			if(StringUtils.isBlank(stockDayPrice.getClosingPrice())) {
//				return;
//			}
//			String chage = stockDayPrice.getChange().trim().replace("+", "").replaceAll(",", "");
//			if(chage.equals("0.00")) {
//				chage = "0";
//			} 
//			if(chage.equals("除息") || chage.equals("除權")) {
//				Optional<StockDayPrice> stockDayPriceOp = stockDayPriceService.findByStockCodeAndTradingDayBeforLimitOne(stockDayPrice.getStockCode(), stockDayPrice.getTradingDay());
//				if(stockDayPriceOp.isPresent()) {
//					chage = String.valueOf(Double
//							.valueOf(stockDayPrice.getClosingPrice().replace("+", "").replaceAll(",", ""))
//							- Double.valueOf(
//									stockDayPriceOp.get().getClosingPrice().replace("+", "").replaceAll(",", "")));
//				} else {
//					chage = "0";
//				}
//			}
//			Double standarPrice = Double.valueOf(stockDayPrice.getClosingPrice().replace("+", "").replaceAll(",", "")) - new BigDecimal(chage).doubleValue();
//			Double upperLimitPrice = ChromeDriverUtils.calculateLimitPrice(standarPrice,
//			true);
//	Double lowerLimitPrice = ChromeDriverUtils.calculateLimitPrice(standarPrice,
//			false);
//	stockDayPrice.setLimitDown(lowerLimitPrice.toString());
//	stockDayPrice.setLimitUp(upperLimitPrice.toString());
//	Double closingPrice = Double.valueOf(stockDayPrice.getClosingPrice().replace("+", "").replaceAll(",", ""));
//	stockDayPrice.setLimitDown(lowerLimitPrice.toString());
//	stockDayPrice.setLimitUp(upperLimitPrice.toString());
//	Double rate = ((closingPrice - standarPrice) / standarPrice) * 100;
//	rate = Math.round(rate * 100.0) / 100.0;
//	stockDayPrice.setChangeRate(rate);
//		});
//		for(StockDayPrice stockpsDayPrice : stockps) {
//			stockDayPriceService.save(stockpsDayPrice);
//		}
		log.info("finsh sync stockDayPrice");
	}

	@Scheduled(cron = "${schedule.task.scheduling.cron.expression.update-margin-trading}")
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
//    @PostConstruct
//	@Scheduled(cron = "${schedule.task.scheduling.cron.expression.grasp-securitiesfirms-dayoperate}")
//	public void grepSecuritiesFirmsDayOperate() throws InterruptedException, RestClientException, URISyntaxException,
//			UnsupportedAudioFileException, IOException, LineUnavailableException, JavaLayerException {
//        RBucket<Boolean> graspStockEnableKeyBucket = redissonClient.getBucket(GRASPSTOCK_REDIS_ENABLE_KEY);
//        RBucket<Boolean> graspStockIsSutDownKeyBucket = redissonClient.getBucket(GRASPSTOCK_REDIS_ENABLE_IS_SHUTDOWN_KEY);
//        Boolean graspStockEnable = graspStockEnableKeyBucket.get();
//        File downloadPathFolder = new File(downloadPath);
//        if(ObjectUtils.isEmpty(graspStockEnable) || Boolean.FALSE.equals(graspStockEnable)) {
//        	graspStockEnableKeyBucket.set(Boolean.TRUE);
//        	graspStockIsSutDownKeyBucket.set(Boolean.FALSE);
//			if(!downloadPathFolder.exists()) {
//				downloadPathFolder.mkdirs();
//			}
//			try {
//				doGrepSecuritiesFirmsDayOperate(downloadPathFolder, graspStockEnableKeyBucket,
//						graspStockIsSutDownKeyBucket, Lists.newArrayList());
//			} catch (Exception e) {
//				log.warn(e.getMessage(), e);
//			} finally {
//				graspStockEnableKeyBucket.set(Boolean.FALSE);
//				graspStockIsSutDownKeyBucket.set(Boolean.TRUE);
//			}
//        } else {
//        	return;
//        }
//	}

	// 手動重跑抓取買賣日報表資訊，執行外框
//	public void tryRedoGrepSecuritiesFirmsDayOperate() throws JsonMappingException, JsonProcessingException,
//			RestClientException, InterruptedException, URISyntaxException {
//		RBucket<Boolean> graspStockEnableKeyBucket = redissonClient.getBucket(GRASPSTOCK_REDIS_ENABLE_KEY);
//		RBucket<Boolean> graspStockIsSutDownKeyBucket = redissonClient
//				.getBucket(GRASPSTOCK_REDIS_ENABLE_IS_SHUTDOWN_KEY);
//		File downloadPathFolder = new File(downloadPath);
//		if (!(Boolean.FALSE.equals(graspStockEnableKeyBucket.get())
//				&& Boolean.TRUE.equals(graspStockIsSutDownKeyBucket.get()))) {
//			graspStockEnableKeyBucket.set(Boolean.FALSE);
//			while (Boolean.FALSE.equals(graspStockIsSutDownKeyBucket.get())) {
//				Thread.sleep(5000);
//			}
//		}
//		// 使用 CompletableFuture 啟動異步任務
//		CompletableFuture.runAsync(() -> {
//			try {
//				graspStockEnableKeyBucket.set(Boolean.TRUE);
//				graspStockIsSutDownKeyBucket.set(Boolean.FALSE);
//
//				if (!downloadPathFolder.exists()) {
//					downloadPathFolder.mkdirs();
//				}
//
//				List<String> finshedStockCodes = List.of(downloadPathFolder.listFiles()).stream().map(downloadFile -> {
//					String stockCode = removeFileExtension(downloadFile.getName());
//					return (stockCode.contains("_") ? stockCode.split("_")[0] : stockCode);
//				}).toList();
//
//				doGrepSecuritiesFirmsDayOperate(downloadPathFolder, graspStockEnableKeyBucket,
//						graspStockIsSutDownKeyBucket, finshedStockCodes);
//			} catch (Exception e) {
//				log.warn(e.getMessage(), e);
//			} finally {
//				graspStockEnableKeyBucket.set(Boolean.FALSE);
//				graspStockIsSutDownKeyBucket.set(Boolean.TRUE);
//			}
//		});
//	}

//	//執行買賣日報表抓取流程
//    private void doGrepSecuritiesFirmsDayOperate(File downloadPathFolder, RBucket<Boolean> graspStockEnableKeyBucket, RBucket<Boolean> graspStockIsSutDownKeyBucket, List<String> filterStockCodes) throws InterruptedException, JsonMappingException, JsonProcessingException, RestClientException, URISyntaxException {
//    	//------過濾如果真的執行retry的時候，要把已經抓下來的stockCode排除掉
//    	List<String> TPEXStockCodes = stockInfoService.findByStockType("0").stream()
//				.filter(stockInfo -> !filterStockCodes.contains(stockInfo.getStockCode()))
//				.map(stockInfo -> stockInfo.getStockCode()).toList();
//		List<String> TESEtockCodes = stockInfoService.findByStockType("1").stream()
//				.filter(stockInfo -> !filterStockCodes.contains(stockInfo.getStockCode()))
//				.map(stockInfo -> stockInfo.getStockCode()).toList();
//    	log.info("exec grepSecuritiesFirmsDayOperate");
//    	//執行TPEX的買賣日報表抓取
//    	ChromeDriverUtils.grepTPEXsecuritiesFirmsDayOperate(downloadPath,
//    			windowsActive ? windowsChromeDriverPath : linuxChromeDriverPath, TPEXStockCodes, credentialsPath, graspStockEnableKeyBucket);
//    	//如果有發現redis的flag改變的時候，要結束
//    	if(Boolean.FALSE.equals(graspStockEnableKeyBucket.get())) {
//    		return;
//    	}
//    	//執行TWSE的買賣日報表抓取
//    	ChromeDriverUtils.grepTWSESsecuritiesFirmsDayOperate(downloadPath,
//    			windowsActive ? windowsChromeDriverPath : linuxChromeDriverPath, TESEtockCodes, bpythonUrl, graspStockEnableKeyBucket);
//    	//如果有發現redis的flag改變的時候，要結束
//    	if(Boolean.FALSE.equals(graspStockEnableKeyBucket.get())) {
//    		return;
//    	}
//    	//抓取最新的交易日期
////    	List<StockDayPrice> stockTpexDayPrices = ChromeDriverUtils
////    			.graspTpexDayPrice("https://www.tpex.org.tw/openapi/v1/tpex_mainboard_quotes");
//    	Date tradeDate = graspHistoryStockPrice.getLastTradeDate();
//    	List.of(downloadPathFolder.listFiles()).stream().filter(downloadFile -> downloadFile.getName().contains(".csv")).forEach(downloadFile -> {
//    		// 讀取CSV文件
//    		CSVReader reader = null;
//    		List<JSONObject> jsonArray = Lists.newArrayList();
//    		List<String[]> csvData = Lists.newArrayList();
//    		try {
//    			reader = new CSVReader(new InputStreamReader(new FileInputStream(downloadFile), "Big5"));
//    			csvData = reader.readAll();
//    			reader.close();
//    		} catch (IOException | CsvException e) {
//    			log.info(e.getMessage(),e);
//    			return;
//    		}
//    		
//    		jsonArray = convertTESECSVtoJSON(csvData);
//    		List<SecuritiesFirmsDayOperate> securitiesFirmsDayOperates = jsonArray.stream().map(jsb -> {
//    			SecuritiesFirmsDayOperate securitiesFirmsDayOperate = new SecuritiesFirmsDayOperate();
//    			String stockCode = removeFileExtension(downloadFile.getName());
//    			securitiesFirmsDayOperate.setPrice(jsb.getString("價格"));
//    			securitiesFirmsDayOperate.setSeq(jsb.getInt("序號"));
//    			securitiesFirmsDayOperate.setStockCode(stockCode.contains("_") ? stockCode.split("_")[0] : stockCode);
//    			securitiesFirmsDayOperate.setSecuritiesFirms(jsb.getString("券商"));
//    			securitiesFirmsDayOperate
//    			.setStockBuyAmount(Long.valueOf(jsb.getString("買進股數").trim().replace(",", "")));
//    			securitiesFirmsDayOperate.setTradingDate(tradeDate);
//    			securitiesFirmsDayOperate.setStockSellAmount(Long.valueOf(jsb.getString("賣出股數").trim().replace(",", "")));
//    			return securitiesFirmsDayOperate;
//    		}).sorted((x1, x2) -> x1.getSeq().compareTo(x2.getSeq())).toList();
//    		securitiesFirmsDayOperateService.insertAll(securitiesFirmsDayOperates);
//    	});
//    	//若當日的買賣日報表都已經抓完，則資料夾清空
//    	downloadPathFolder.delete();
//    }

	// 把買賣日報表的csv轉成bean
//	private List<JSONObject> convertTESECSVtoJSON(List<String[]> csvData) {
//		// 跳過前 3 行表頭數據
//		// 創建 JSON 數組
//		List<JSONObject> jsonArray = Lists.newArrayList();
//		// 跳過前 3 行表頭數據
//		for (int i = 3; i < csvData.size(); i++) {
//			String[] row = csvData.get(i);
//
//			// 檢查第一部分（列 1 到列 5 是否有數據）
//			if (row[0] != null && !row[0].trim().isEmpty()) {
//				JSONObject jsonObject = new JSONObject();
//				jsonObject.put("序號", Integer.parseInt(row[0].trim()));
//				jsonObject.put("券商", row[1].trim());
//				jsonObject.put("價格", row[2].trim());
//				jsonObject.put("買進股數", row[3].trim());
//				jsonObject.put("賣出股數", row[4].trim());
//
//				jsonArray.add(jsonObject); // 將第一部分的 JSON 對象添加到列表中
//			}
//
//			// 檢查第二部分（列 7 到列 11 是否有數據）
//			if (row.length > 6 && row[6] != null && !row[6].trim().isEmpty()) {
//				JSONObject jsonObject = new JSONObject();
//				jsonObject.put("序號", Integer.parseInt(row[6].trim())); // 從第 7 列開始
//				jsonObject.put("券商", row[7].trim());
//				jsonObject.put("價格", row[8].trim());
//				jsonObject.put("買進股數", row[9].trim());
//				jsonObject.put("賣出股數", row[10].trim());
//
//				jsonArray.add(jsonObject); // 將第二部分的 JSON 對象添加到列表中
//			}
//		}
//		return jsonArray;
//	}
//
//	public static String removeFileExtension(String fileName) {
//		// 找到最後一個點的位置
//		int lastDotIndex = fileName.lastIndexOf('.');
//
//		// 如果找到了點，截取之前的部分；如果沒有點，則返回原始文件名
//		if (lastDotIndex != -1) {
//			return fileName.substring(0, lastDotIndex);
//		} else {
//			return fileName; // 沒有擴展名
//		}
//	}

	public void calculateRSVValueAndLimitDownUp(StockDayPrice stockTwseDayPrice){
		if(stockTwseDayPrice.getClosingPrice().equals("---") || stockTwseDayPrice.getClosingPrice().equals("----") || stockTwseDayPrice.getClosingPrice().equals("--")  ) {
			return;
		}
		List<StockDayPrice> twelfthStockDayPrices = stockDayPriceService
				.findByStockCodeAndTradingDayBeforEqualLimitNimeth(stockTwseDayPrice.getStockCode(),
						stockTwseDayPrice.getTradingDay());
		if (twelfthStockDayPrices.size() < 9) {
			return; // 如果不满足条件，返回 null
		}

		int period = 9;
		List<Double> closingPrices = twelfthStockDayPrices.stream().map(innerTwelfthStockDayPrice -> Double
				.valueOf(innerTwelfthStockDayPrice.getClosingPrice().replaceAll(",", ""))).toList();
		List<Double> highPrices = twelfthStockDayPrices.stream().map(innerTwelfthStockDayPrice -> Double
				.valueOf(innerTwelfthStockDayPrice.getHighPrice().replaceAll(",", ""))).toList();
		List<Double> lowPrices = twelfthStockDayPrices.stream().map(innerTwelfthStockDayPrice -> Double
				.valueOf(innerTwelfthStockDayPrice.getLowPrice().replaceAll(",", ""))).toList();

		double highestHigh = Double.MIN_VALUE;
		double lowestLow = Double.MAX_VALUE;

		for (int i = 0; i < period; i++) {
			highestHigh = Math.max(highestHigh, highPrices.get(i));
			lowestLow = Math.min(lowestLow, lowPrices.get(i));
		}

		double latestClosingPrice = closingPrices.get(0);
		Double rsv = (latestClosingPrice - lowestLow) / (highestHigh - lowestLow) * 100.0;

		Double previousK =  StringUtils.isNotBlank( twelfthStockDayPrices.get(1).getLineKvalue())  ? Double.valueOf(twelfthStockDayPrices.get(1).getLineKvalue())  : 50; // Default initial K value
		Double previousD = StringUtils.isNotBlank( twelfthStockDayPrices.get(1).getLineDvalue())  ? Double.valueOf(twelfthStockDayPrices.get(1).getLineDvalue())  : 50;// Default initial D value
		double smoothingFactor = 1.0 / 3.0;

		Double k = previousK * (1 - smoothingFactor) + rsv * smoothingFactor;
		Double d = previousD * (1 - smoothingFactor) + k * smoothingFactor;

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
	}
}
