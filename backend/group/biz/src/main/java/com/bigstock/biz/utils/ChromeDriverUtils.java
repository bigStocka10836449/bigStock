package com.bigstock.biz.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.redisson.api.RBucket;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.bigstock.sharedComponent.entity.MarginTradingAndShortSellingInfo;
import com.bigstock.sharedComponent.entity.StockDayPrice;
import com.bigstock.sharedComponent.entity.StockInfo;
import com.bigstock.sharedComponent.entity.TmpExDividendsExRightInfo;
import com.bigstock.sharedComponent.entity.TradeVolumeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChromeDriverUtils {

	private static final Map<Integer, String> SHAREHOLDER_STRUCTURE_COLUMN_NAME = new HashMap<>();

	private static final Map<Integer, String> STOCK_DAY_PRICE_COLUMN_NAME = new HashMap<>();

	private static final List<String> TWSE_TYPE_LIST = Arrays.asList("01", "02", "03", "04", "05", "06", "07", "21",
			"22", "08", "09", "10", "11", "12", "13", "24", "25", "26", "27", "28", "29", "30", "31", "14", "15", "16",
			"17", "18", "9299", "23", "19", "20");

	static {
		initializeColumnNames();
	}

	
	public static List<TmpExDividendsExRightInfo> grepTmpExDividendsExRightInfo(Date tradingMonth) throws RestClientException, URISyntaxException, JsonMappingException, JsonProcessingException{
		List<TmpExDividendsExRightInfo> allInfos = Lists.newArrayList();
		ObjectMapper objectMapper = new ObjectMapper();
		LocalDate today = tradingMonth.toInstant().atZone(ZoneId.of("Asia/Taipei")).toLocalDate();
		DateTimeFormatter tpexDateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd").withZone(ZoneId.of("Asia/Taipei"));
		 // 設置本月的第一天
        LocalDate startOfMonth = today.withDayOfYear(1);

        // 設置本月的最後一天
        LocalDate endOfMonth = today.withDayOfMonth(today.lengthOfMonth());
        //https://www.tpex.org.tw/www/zh-tw/bulletin/exDailyQ
        Map<String, String> parameter = Maps.newHashMap();
        parameter.put("startDate", startOfMonth.format(tpexDateFormatter));
        parameter.put("endDate", endOfMonth.format(tpexDateFormatter));
        parameter.put("id", "");
        parameter.put("response", "json");
        String tpexResult = fetchApiData("https://www.tpex.org.tw/www/zh-tw/bulletin/exDailyQ", parameter);
    	Map<String, Object> tpexResultMap = objectMapper.readValue(tpexResult,
				new TypeReference<Map<String, Object>>() {
				});
		List<List<String>> tpexDatas = (List) ((Map<String, Object>) ((List) tpexResultMap.get("tables")).get(0))
				.get("data");
		List<TmpExDividendsExRightInfo> tpexInfos = tpexDatas.stream().map(entry -> {
			String tradingDateStr = entry.get(0);
			// 拆分民国日期字符串
			String[] parts = tradingDateStr.split("/");
			int innerTaiwanYear = Integer.parseInt(parts[0]); // 民国年份
			int month = Integer.parseInt(parts[1]); // 月
			int day = Integer.parseInt(parts[2].replaceAll("\\*", "")); // 日

			// 将民国年份转换为公历年份
			int year = innerTaiwanYear + 1911;

			// 构造公历日期字符串
			String gregorianDateStr = year + "/" + month + "/" + day;
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
			sdf.setTimeZone(TimeZone.getTimeZone(ZoneId.of("Asia/Taipei")));
			Date tradingDate;
			try {
				tradingDate = sdf.parse(gregorianDateStr);
			} catch (ParseException e) {
				log.warn(e.getMessage(), e);
				tradingDate = new Date();
			}
			TmpExDividendsExRightInfo tmpExDividendsExRightInfo = new TmpExDividendsExRightInfo();
			tmpExDividendsExRightInfo.setTradingDay(tradingDate);
			tmpExDividendsExRightInfo.setLimitDown(entry.get(10));
			tmpExDividendsExRightInfo.setLimitUp(entry.get(9));
			tmpExDividendsExRightInfo.setStockCode(entry.get(1));
			tmpExDividendsExRightInfo.setReferencePrice(entry.get(11));
			return tmpExDividendsExRightInfo;
		}).toList();
		allInfos.addAll(tpexInfos);
		//https://www.twse.com.tw/rwd/zh/exRight/TWT49U?startDate=20241205&endDate=20241212&response=json&_=1733885812391
		DateTimeFormatter twseDateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneId.of("Asia/Taipei"));
		String teseUrl = String.format(
				"https://www.twse.com.tw/rwd/zh/exRight/TWT49U?startDate=%1s&endDate=%2s&response=json&_=1733885812391",
				startOfMonth.format(twseDateFormatter), endOfMonth.format(twseDateFormatter));
		String twseResult = fetchApiData(teseUrl);
		Map<String, Object> twseResultMap = objectMapper.readValue(twseResult,
				new TypeReference<Map<String, Object>>() {
				});
		if(twseResultMap.get("stat").equals("OK")) {
			List<List<String>> twseDatas = (List) twseResultMap.get("data");
			List<TmpExDividendsExRightInfo> twseInfos = twseDatas.stream().map(twseData ->{
				String tradingDateStr = twseData.get(0).replace("年", "/").replace("月", "/").replace("日", StringUtils.EMPTY);
				String[] parts = tradingDateStr.split("/");
				int innerTaiwanYear = Integer.parseInt(parts[0]); // 民国年份
				int month = Integer.parseInt(parts[1]); // 月
				int day = Integer.parseInt(parts[2].replaceAll("\\*", "")); // 日

				// 将民国年份转换为公历年份
				int year = innerTaiwanYear + 1911;

				// 构造公历日期字符串
				String gregorianDateStr = year + "/" + month + "/" + day;
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
				sdf.setTimeZone(TimeZone.getTimeZone(ZoneId.of("Asia/Taipei")));
				Date tradingDate;
				try {
					tradingDate = sdf.parse(gregorianDateStr);
				} catch (ParseException e) {
					log.warn(e.getMessage(), e);
					tradingDate = new Date();
				}
				TmpExDividendsExRightInfo tmpExDividendsExRightInfo = new TmpExDividendsExRightInfo();
				tmpExDividendsExRightInfo.setTradingDay(tradingDate);
				tmpExDividendsExRightInfo.setLimitUp(twseData.get(7));
				tmpExDividendsExRightInfo.setLimitDown(twseData.get(8));
				tmpExDividendsExRightInfo.setStockCode(twseData.get(1));
				tmpExDividendsExRightInfo.setReferencePrice(twseData.get(9));
				return tmpExDividendsExRightInfo;
			}).toList();
			allInfos.addAll(twseInfos);
		}
		return allInfos;
	}
	
	

	public static String getRandomStockCode(List<String> stockCodes) {
		if (stockCodes == null || stockCodes.isEmpty()) {
			throw new IllegalArgumentException("The stockCodes list cannot be null or empty");
		}
		Random random = new Random();
		int randomIndex = random.nextInt(stockCodes.size());
		return stockCodes.get(randomIndex);
	}

	

	public static List<StockDayPrice> graspTwseDayPrice(String url, Date tradeDate) throws InterruptedException,
			JsonMappingException, JsonProcessingException, RestClientException, URISyntaxException {
		String jsonResponse = fetchApiData(url);

		ObjectMapper objectMapper = new ObjectMapper();
		List<Map<String, String>> responseList = objectMapper
				.readValue(jsonResponse, new TypeReference<List<Map<String, String>>>() {
				}).stream()
				.filter(data -> {
					String code = data.get("Code").toString();
					return code.trim().length() < 5 && !code.matches(".*[a-zA-Z].*");
				})
				.collect(Collectors.toList());
		LocalDate today = tradeDate.toInstant().atZone(ZoneId.of("Asia/Taipei")).toLocalDate();

		// 設置本周第一天的日期
		LocalDate startOfWeekLocalDate = today.with(DayOfWeek.MONDAY);

		// 設置本周最後一天的日期
		LocalDate endOfWeekLocalDate = today.with(DayOfWeek.SUNDAY);
		// 獲取系統默認時區
		ZoneId zoneId = ZoneId.of("Asia/Taipei");

		// 獲取偏移量
		ZoneOffset zoneOffset = zoneId.getRules().getOffset(startOfWeekLocalDate.atStartOfDay());

		// 將 LocalDate 轉換為 Date
		Date startOfWeeDate = Date.from(startOfWeekLocalDate.atStartOfDay().toInstant(zoneOffset));
		Date endOfWeekDate = Date.from(endOfWeekLocalDate.atStartOfDay().toInstant(zoneOffset));

		return responseList.stream().map(map -> {
			StockDayPrice stockDayPrice = new StockDayPrice();
			stockDayPrice.setStockCode(map.get("Code"));
			stockDayPrice.setOpeningPrice(map.get("OpeningPrice").replaceAll(",", ""));
			stockDayPrice.setClosingPrice(map.get("ClosingPrice").replaceAll(",", ""));
			stockDayPrice.setHighPrice(map.get("HighestPrice").replaceAll(",", ""));
			stockDayPrice.setLowPrice(map.get("LowestPrice"));
			stockDayPrice.setChange(map.get("Change").replace("+", "").replaceAll(",", ""));
			stockDayPrice.setTradingDay(tradeDate);
			stockDayPrice.setStartOfWeekDate(startOfWeeDate);
			stockDayPrice.setEndOfWeekDate(endOfWeekDate);
			stockDayPrice.setWeekOfYear(endOfWeekLocalDate.getYear() + "W" + today.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
			stockDayPrice.setTradingVolume(map.get("TradeVolume"));
			return stockDayPrice;
		}).toList();
	}

	public static List<MarginTradingAndShortSellingInfo> graspTpexMarginTradingAndShortSellingInfo(String url)
			throws InterruptedException, JsonMappingException, JsonProcessingException, RestClientException,
			URISyntaxException {
		String jsonResponse = fetchApiData(url);

		ObjectMapper objectMapper = new ObjectMapper();
		List<Map<String, String>> responseList = objectMapper
				.readValue(jsonResponse, new TypeReference<List<Map<String, String>>>() {
				}).stream().filter(data -> {
					String code = data.get("SecuritiesCompanyCode").toString();
					return code.length() < 5 && !code.matches(".*[a-zA-Z].*");
				}).collect(Collectors.toList());
		return responseList.stream().map(map -> {
			// 指定日期字符串格式
			DateTimeFormatter dateStringformatter = DateTimeFormatter.ofPattern("yyyy/MM/dd").withZone(ZoneId.of("Asia/Taipei"));

			String monthAndDate = map.get("Date").substring(map.get("Date").length() - 4);
			int year = Integer.parseInt(map.get("Date").replace(monthAndDate, "")) + 1911; // 民国转换为西元
			String standardDateString = year + "/" + monthAndDate.substring(0, 2) + "/" + monthAndDate.substring(2, 4);

			// 解析标准日期字符串为 LocalDate 对象
			LocalDate localDate = LocalDate.parse(standardDateString, dateStringformatter);
			Date date = Date.from(localDate.atStartOfDay().atZone(ZoneId.of("Asia/Taipei")).toInstant());

			MarginTradingAndShortSellingInfo marginTradingAndShortSellingInfo = new MarginTradingAndShortSellingInfo();
			marginTradingAndShortSellingInfo.setTradingDay(date);
			marginTradingAndShortSellingInfo.setStockCode(map.get("SecuritiesCompanyCode"));
			marginTradingAndShortSellingInfo
					.setMarginPurchaseBalancePreviousDay(map.get("MarginPurchaseBalancePreviousDay"));
			marginTradingAndShortSellingInfo.setMarginPurchase(map.get("MarginPurchase"));
			marginTradingAndShortSellingInfo.setMarginSales(map.get("MarginSales"));
			marginTradingAndShortSellingInfo.setCashRedemption(map.get("CashRedemption"));
			marginTradingAndShortSellingInfo
					.setMarginPurchaseBalance(map.get("MarginPurchaseBalance"));
			marginTradingAndShortSellingInfo.setMarginPurchaseQuota(map.get("MarginPurchaseQuota"));
			marginTradingAndShortSellingInfo
					.setShortSaleBalancePreviousDay(map.get("ShortSaleBalancePreviousDay"));
			marginTradingAndShortSellingInfo.setShortSale(map.get("ShortSale"));
			marginTradingAndShortSellingInfo.setShortConvering(map.get("ShortConvering"));
			marginTradingAndShortSellingInfo.setStockRedemption(map.get("StockRedemption"));
			marginTradingAndShortSellingInfo.setShortSaleBalance(map.get("ShortSaleBalance"));
			marginTradingAndShortSellingInfo.setShortSaleQuota(map.get("ShortSaleQuota"));
			marginTradingAndShortSellingInfo.setOffsetting(map.get("Offsetting"));
			return marginTradingAndShortSellingInfo;
		}).toList();

	}

	public static List<MarginTradingAndShortSellingInfo> graspTwseMarginTradingAndShortSellingInfo(String url,
			Date tradeDate) throws InterruptedException, JsonMappingException, JsonProcessingException,
			RestClientException, URISyntaxException {
		String jsonResponse = fetchApiData(url);

		ObjectMapper objectMapper = new ObjectMapper();
		List<Map<String, String>> responseList = objectMapper
				.readValue(jsonResponse, new TypeReference<List<Map<String, String>>>() {
				}).stream()
				.filter(data -> {
					String code = data.get("股票名稱").toString();
					return (code.length() < 5 && !code.matches(".*[a-zA-Z].*"));
				})
				.collect(Collectors.toList());
		return responseList.stream().map(map -> {
			MarginTradingAndShortSellingInfo marginTradingAndShortSellingInfo = new MarginTradingAndShortSellingInfo();
			marginTradingAndShortSellingInfo.setTradingDay(tradeDate);
			marginTradingAndShortSellingInfo.setStockCode(map.get("股票代號"));
			marginTradingAndShortSellingInfo
					.setMarginPurchaseBalancePreviousDay(map.get("融資前日餘額"));
			marginTradingAndShortSellingInfo.setMarginPurchase(map.get("融資買進"));
			marginTradingAndShortSellingInfo.setMarginSales(map.get("融資賣出"));
			marginTradingAndShortSellingInfo.setCashRedemption(map.get("融資現金償還"));
			marginTradingAndShortSellingInfo
					.setMarginPurchaseBalance(map.get("融資今日餘額"));
			marginTradingAndShortSellingInfo.setMarginPurchaseQuota(map.get("融資限額"));
			marginTradingAndShortSellingInfo
					.setShortSaleBalancePreviousDay(map.get("融券前日餘額"));
			marginTradingAndShortSellingInfo.setShortSale(map.get("融券買進"));
			marginTradingAndShortSellingInfo.setShortConvering(map.get("融券賣出"));
			marginTradingAndShortSellingInfo.setStockRedemption(map.get("融券現券償還"));
			marginTradingAndShortSellingInfo.setShortSaleBalance(map.get("融資今日餘額"));
			marginTradingAndShortSellingInfo.setShortSaleQuota(map.get("融券限額"));
			marginTradingAndShortSellingInfo.setOffsetting(map.get("資券互抵"));
			return marginTradingAndShortSellingInfo;
		}).toList();
	}

	public static List<TradeVolumeInfo> graspTpexTtradeVolume(String url) throws InterruptedException,
			JsonMappingException, JsonProcessingException, RestClientException, URISyntaxException {
		String jsonResponse = fetchApiData("https://www.tpex.org.tw/openapi/v1/tpex_volume_rank");

		ObjectMapper objectMapper = new ObjectMapper();
		List<Map<String, String>> responseList = objectMapper
				.readValue(jsonResponse, new TypeReference<List<Map<String, String>>>() {
				}).stream()
				.filter(data -> {
					String code = data.get("SecuritiesCompanyCode").toString();
					return code.trim().length() < 5 && !code.matches(".*[a-zA-Z].*");
				})
				.collect(Collectors.toList());

		return responseList.stream().map(map -> {
			// 指定日期字符串格式
			DateTimeFormatter dateStringformatter = DateTimeFormatter.ofPattern("yyyy/MM/dd").withZone(ZoneId.of("Asia/Taipei"));

			String monthAndDate = map.get("Date").substring(map.get("Date").length() - 4);
			int year = Integer.parseInt(map.get("Date").replace(monthAndDate, "")) + 1911; // 民国转换为西元
			String standardDateString = year + "/" + monthAndDate.substring(0, 2) + "/" + monthAndDate.substring(2, 4);

			// 解析标准日期字符串为 LocalDate 对象
			LocalDate localDate = LocalDate.parse(standardDateString, dateStringformatter);
			Date date = Date.from(localDate.atStartOfDay().atZone(ZoneId.of("Asia/Taipei")).toInstant());

			TradeVolumeInfo tradeVolumeInfo = new TradeVolumeInfo();
			tradeVolumeInfo.setStockCode(map.get("SecuritiesCompanyCode"));
			tradeVolumeInfo.setTradingDay(date);
			String tradingVolume = map.get("TradingVolume");
			if (tradingVolume != null) {
				try {
					// Convert to number
					int volumeNumber = Integer.parseInt(tradingVolume);

					// Append "000" and set as string
					tradeVolumeInfo.setTradeVolume(volumeNumber + "000");
				} catch (NumberFormatException e) {
					tradeVolumeInfo.setTradeVolume("000");
				}
			}
			return tradeVolumeInfo;
		}).toList();
	}
	
	
	public static List<StockDayPrice> graspTpexEmergingStockDayPrice() throws InterruptedException, JsonMappingException,
			JsonProcessingException, RestClientException, URISyntaxException {
		String jsonResponse = fetchApiData("https://www.tpex.org.tw/openapi/v1/tpex_esb_latest_statistics");
		System.out.println("Length of response: " + jsonResponse.length());
		System.out.println("Ends with ']': " + jsonResponse.trim().endsWith("]")); // 應該為 true
		log.info(jsonResponse);
		ObjectMapper objectMapper = new ObjectMapper();
		List<Map<String, String>> responseList = objectMapper
				.readValue(jsonResponse, new TypeReference<List<Map<String, String>>>() {
				}).stream()
		.filter(data -> {
			String code = data.get("SecuritiesCompanyCode").toString();
			return code.trim().length() < 5 && !code.matches(".*[a-zA-Z].*");
		})
				.collect(Collectors.toList());
		return responseList.stream().map(map -> {
			// 指定日期字符串格式
			DateTimeFormatter dateStringformatter = DateTimeFormatter.ofPattern("yyyy/MM/dd").withZone(ZoneId.of("Asia/Taipei"));

			String monthAndDate = map.get("Date").substring(map.get("Date").length() - 4);
			int year = Integer.parseInt(map.get("Date").replace(monthAndDate, "")) + 1911; // 民国转换为西元
			String standardDateString = year + "/" + monthAndDate.substring(0, 2) + "/" + monthAndDate.substring(2, 4);

			// 解析标准日期字符串为 LocalDate 对象
			LocalDate localDate = LocalDate.parse(standardDateString, dateStringformatter);
			Date date = Date.from(localDate.atStartOfDay().atZone(ZoneId.of("Asia/Taipei")).toInstant());
			LocalDate today = date.toInstant().atZone(ZoneId.of("Asia/Taipei")).toLocalDate();

			// 設置本周第一天的日期
			LocalDate startOfWeekLocalDate = today.with(DayOfWeek.MONDAY);

			// 設置本周最後一天的日期
			LocalDate endOfWeekLocalDate = today.with(DayOfWeek.SUNDAY);
			// 獲取系統默認時區
			ZoneId zoneId = ZoneId.of("Asia/Taipei");

			// 獲取偏移量
			ZoneOffset zoneOffset = zoneId.getRules().getOffset(startOfWeekLocalDate.atStartOfDay());

			// 將 LocalDate 轉換為 Date
			Date startOfWeeDate = Date.from(startOfWeekLocalDate.atStartOfDay().toInstant(zoneOffset));
			Date endOfWeekDate = Date.from(endOfWeekLocalDate.atStartOfDay().toInstant(zoneOffset));
			StockDayPrice stockDayPrice = new StockDayPrice();
			stockDayPrice.setStockCode(map.get("SecuritiesCompanyCode"));
			//Average
			if (ObjectUtils.isNotEmpty(map.get("PreviousAveragePrice"))) {
				stockDayPrice.setOpeningPrice(map.get("PreviousAveragePrice").replaceAll(",", ""));
			} else {
				stockDayPrice.setOpeningPrice(map.get("Average").replaceAll(",", ""));
			}
			stockDayPrice.setClosingPrice(map.get("LatestPrice").replaceAll(",", ""));

			BigDecimal open = new BigDecimal(
			        Optional.ofNullable(stockDayPrice.getOpeningPrice()).filter(s -> !s.isBlank()).orElse("0")
			);
			BigDecimal close = new BigDecimal(
			        Optional.ofNullable(stockDayPrice.getClosingPrice()).filter(s -> !s.isBlank()).orElse("0")
			);


			BigDecimal diff = close.subtract(open).setScale(2, RoundingMode.HALF_UP);
			// 0.00 或 -0.00 都顯示成 "0"
			String changeStr = (diff.compareTo(BigDecimal.ZERO) == 0) ? "0.00" : diff.toPlainString();
		
			stockDayPrice.setChange(diff.toPlainString());
			
			stockDayPrice.setHighPrice(map.get("Highest").replaceAll(",", ""));
			stockDayPrice.setLowPrice(map.get("Lowest").replaceAll(",", ""));
			stockDayPrice.setChange(changeStr);
			stockDayPrice.setTradingDay(date);
			stockDayPrice.setStartOfWeekDate(startOfWeeDate);
			stockDayPrice.setEndOfWeekDate(endOfWeekDate);
			stockDayPrice
					.setWeekOfYear(endOfWeekLocalDate.getYear() + "W" + today.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
			stockDayPrice.setTradingVolume(map.get("TransactionVolume").toString());
			return stockDayPrice;
		}).toList();
	}
	
	public static List<StockDayPrice> graspTpexDayPrice(String url) throws InterruptedException, JsonMappingException,
			JsonProcessingException, RestClientException, URISyntaxException {
		String jsonResponse = fetchApiData("https://www.tpex.org.tw/openapi/v1/tpex_mainboard_quotes");
		System.out.println("Length of response: " + jsonResponse.length());
		System.out.println("Ends with ']': " + jsonResponse.trim().endsWith("]")); // 應該為 true
		log.info(jsonResponse);
		ObjectMapper objectMapper = new ObjectMapper();
		List<Map<String, String>> responseList = objectMapper
				.readValue(jsonResponse, new TypeReference<List<Map<String, String>>>() {
				}).stream()
				.filter(data -> {
					String code = data.get("SecuritiesCompanyCode").toString();
					return code.trim().length() < 5 && !code.matches(".*[a-zA-Z].*");
				})
				.collect(Collectors.toList());
		return responseList.stream().map(map -> {
			// 指定日期字符串格式
			DateTimeFormatter dateStringformatter = DateTimeFormatter.ofPattern("yyyy/MM/dd").withZone(ZoneId.of("Asia/Taipei"));

			String monthAndDate = map.get("Date").substring(map.get("Date").length() - 4);
			int year = Integer.parseInt(map.get("Date").replace(monthAndDate, "")) + 1911; // 民国转换为西元
			String standardDateString = year + "/" + monthAndDate.substring(0, 2) + "/" + monthAndDate.substring(2, 4);

			// 解析标准日期字符串为 LocalDate 对象
			LocalDate localDate = LocalDate.parse(standardDateString, dateStringformatter);
			Date date = Date.from(localDate.atStartOfDay().atZone(ZoneId.of("Asia/Taipei")).toInstant());
			LocalDate today = date.toInstant().atZone(ZoneId.of("Asia/Taipei")).toLocalDate();

			// 設置本周第一天的日期
			LocalDate startOfWeekLocalDate = today.with(DayOfWeek.MONDAY);

			// 設置本周最後一天的日期
			LocalDate endOfWeekLocalDate = today.with(DayOfWeek.SUNDAY);
			// 獲取系統默認時區
			ZoneId zoneId = ZoneId.of("Asia/Taipei");

			// 獲取偏移量
			ZoneOffset zoneOffset = zoneId.getRules().getOffset(startOfWeekLocalDate.atStartOfDay());

			// 將 LocalDate 轉換為 Date
			Date startOfWeeDate = Date.from(startOfWeekLocalDate.atStartOfDay().toInstant(zoneOffset));
			Date endOfWeekDate = Date.from(endOfWeekLocalDate.atStartOfDay().toInstant(zoneOffset));
			StockDayPrice stockDayPrice = new StockDayPrice();
			stockDayPrice.setStockCode(map.get("SecuritiesCompanyCode"));
			stockDayPrice.setOpeningPrice(map.get("Open").replaceAll(",", ""));
			stockDayPrice.setClosingPrice(map.get("Close").replaceAll(",", ""));
			stockDayPrice.setHighPrice(map.get("High").replaceAll(",", ""));
			stockDayPrice.setLowPrice(map.get("Low").replaceAll(",", ""));
			stockDayPrice.setChange(map.get("Change").replace("+", "").replaceAll(",", ""));
			stockDayPrice.setTradingDay(date);
			stockDayPrice.setStartOfWeekDate(startOfWeeDate);
			stockDayPrice.setEndOfWeekDate(endOfWeekDate);
			stockDayPrice.setWeekOfYear(endOfWeekLocalDate.getYear() + "W" + today.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
			return stockDayPrice;
		}).toList();
	}

	
	
	public static List<Map<Integer, String>> graspShareholderStructureFromTDCCApi(String tdccOpenApiUrl)
			throws JsonMappingException, JsonProcessingException, RestClientException, URISyntaxException {
		String jsonResponse = fetchApiData(tdccOpenApiUrl);

		ObjectMapper objectMapper = new ObjectMapper();
		List<Map<String, String>> responseList = objectMapper
				.readValue(jsonResponse, new TypeReference<List<Map<String, String>>>() {
				}).stream()
//				.filter(data -> {
//					String code = data.get("證券代號").toString();
//					return code.length() < 5 && !code.matches(".*[a-zA-Z].*");
//				})
				.collect(Collectors.toList());

		Map<String, List<Map<String, String>>> groupResponseMap = new HashMap<>();
		for (Map<String, String> response : responseList) {
			groupResponseMap.computeIfAbsent(response.get("證券代號"), k -> new ArrayList<>()).add(response);
		}

		return groupResponseMap.entrySet().stream().map(set -> {
			List<Map<String, String>> innerList = set.getValue();
			Map<String, String> map = innerList.stream().findFirst().orElse(new HashMap<>());
			String date = map.get("﻿資料日期");
			String stockCode = map.get("證券代號");
			LocalDate localDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneId.of("Asia/Taipei")));
			Integer weeksOfYear = localDate.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
			String weeksOfYearString = localDate.getYear() + "W" + weeksOfYear;
			String countDate = localDate.getMonthValue() + "/" + localDate.getDayOfMonth();
			Map<Integer, String> innerMap = new HashMap<>();
			innerMap.put(0, weeksOfYearString);
			innerMap.put(1, countDate);
			for (int index = 0; index < innerList.size(); index++) {
				Map<String, String> data = innerList.get(index);
				if (index <= 16) {
					innerMap.put((index < 16 ? index + 6 : index + 5), data.get("股數"));
				}
				innerMap.put(index + 22, data.get("人數"));
			}
			innerMap.put(37, stockCode);
			return innerMap;
		}).collect(Collectors.toList());
	}

	public static List<StockInfo> getStockInfoByTdccApi(String tdccOpenApiUrl)
			throws RestClientException, URISyntaxException, JsonMappingException, JsonProcessingException {
		String jsonResponse = fetchApiData(tdccOpenApiUrl);

		ObjectMapper objectMapper = new ObjectMapper();
		List<Map<String, Object>> responseList = objectMapper.readValue(jsonResponse,
				new TypeReference<List<Map<String, Object>>>() {
				});

		return responseList.stream().filter(data -> {
			String code = data.get("證券代號").toString();
			String market = data.get("市場別").toString();
			return !market.contains("（終止上市(櫃)、興櫃)");
//					code.length() < 5 && !code.matches(".*[a-zA-Z].*") &&
//					!market.contains("（終止上市(櫃)、興櫃)");
		}).map(data -> {
			StockInfo stockInfo = new StockInfo();
			String name = decodeHtmlEntities(data.get("證券名稱").toString());
			stockInfo.setStockCode(data.get("證券代號").toString());
			stockInfo.setStockName(name);
			String marketType = data.get("市場別").toString();
			if ("上市".equals(marketType)) {
				stockInfo.setStockType("1");
			} else if ("上櫃".equals(marketType)) {
				stockInfo.setStockType("0");
			} else if ("興櫃".equals(marketType)) {
				stockInfo.setStockType("2");
			}
			return stockInfo;
		}).collect(Collectors.toList());
	}

//	private static final String baseUrl = "https://www.tpex.org.tw/web/stock/aftertrading/daily_trading_info/st43_result.php?l=zh-tw&d=%1s&stkno=6272&_=17225";



	private static String fetchApiData(String url) {
		int maxRetries = 3;
		int retryDelayMs = 1000;

		for (int attempt = 1; attempt <= maxRetries; attempt++) {
			try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
				HttpGet request = new HttpGet(url);
				HttpResponse response = httpClient.execute(request);
				org.apache.http.HttpEntity entity = response.getEntity();

				if (entity != null) {
					String body = EntityUtils.toString(entity, StandardCharsets.UTF_8);

					if (body != null) {
						return body;
					} else {
						log.warn("Attempt {}/{}: JSON malformed or incomplete", attempt, maxRetries);
					}
				}

			} catch (Exception e) {
				log.warn("Attempt {}/{}: Exception while fetching data: {}", attempt, maxRetries, e.getMessage());
			}

			try {
				Thread.sleep(retryDelayMs);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new RuntimeException("Thread was interrupted during retry wait", e);
			}
		}
		throw new RuntimeException("Failed to fetch valid JSON after " + maxRetries + " attempts.");
	}
	
	public static String fetchApiData(String url, Map<String, String> formParameters) {
	    int maxRetries = 3;
	    int retryDelayMs = 1000;

	    for (int attempt = 1; attempt <= maxRetries; attempt++) {
	        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
	            HttpPost httpPost = new HttpPost(url);

	            // Prepare form parameters
	            List<org.apache.http.message.BasicNameValuePair> params = new ArrayList<>();
	            for (Map.Entry<String, String> entry : formParameters.entrySet()) {
	                params.add(new org.apache.http.message.BasicNameValuePair(entry.getKey(), entry.getValue()));
	            }

	            httpPost.setEntity(new org.apache.http.client.entity.UrlEncodedFormEntity(params, StandardCharsets.UTF_8));

	            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
	                int statusCode = response.getStatusLine().getStatusCode();
	                String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

	                if ((statusCode == 200) && responseBody != null) {
	                    return responseBody;
	                } else {
	                    log.warn("Attempt {}/{}: Bad response (status: {})", attempt, maxRetries, statusCode);
	                }
	            }

	        } catch (Exception e) {
	            log.warn("Attempt {}/{}: Exception during POST request: {}", attempt, maxRetries, e.getMessage());
	        }

	        try {
	            Thread.sleep(retryDelayMs);
	        } catch (InterruptedException ie) {
	            Thread.currentThread().interrupt();
	            throw new RuntimeException("Thread was interrupted during retry wait", ie);
	        }
	    }

	    throw new RuntimeException("POST request failed or response is invalid after " + maxRetries + " attempts.");
	}

	private static String decodeHtmlEntities(String input) {
		Pattern pattern = Pattern.compile("&#(\\d+);");
		Matcher matcher = pattern.matcher(input);
		StringBuilder decodedString = new StringBuilder();
		while (matcher.find()) {
			int codePoint = Integer.parseInt(matcher.group(1));
			matcher.appendReplacement(decodedString, new String(Character.toChars(codePoint)));
		}
		matcher.appendTail(decodedString);
		return decodedString.toString();
	}

	// 發送 POST 請求到 Python 服務並返回 CAPTCHA 結果
	private static String sendPostRequest(String jsonRequest, String bpythonUrl) {
		RestTemplate restTemplate = new RestTemplate();

		// 設置標頭
		HttpHeaders headers = new HttpHeaders();
		headers.add("Content-Type", "application/json");

		// 創建HttpEntity
		HttpEntity<String> requestEntity = new HttpEntity<>(jsonRequest, headers);

		// 發送 POST 請求
		ResponseEntity<String> response = restTemplate.exchange(bpythonUrl, HttpMethod.POST, requestEntity,
				String.class);

		// 檢查響應狀態
		if (response.getStatusCode() == HttpStatus.OK) {
			log.debug("Response from server: " + response.getBody());
			JSONObject responseJson = new JSONObject(response.getBody());
			return responseJson.getString("result"); // 返回服務器的 CAPTCHA 結果
		} else {
			log.info("Failed to send request. Status code: " + response.getStatusCode());
			return null;
		}
	}


	
	// 計算漲停或跌停價
	public static Double calculateLimitPrice(double closingPrice, boolean isUpper) {
	    // 計算理論價格
	    double limitPrice = closingPrice * (isUpper ? 1.10 : 0.90);
	    // 根據 tick 單位調整
	    double tickSize = getTickSize(limitPrice);

	    double adjustedPrice;
	    if (isUpper) {
	        // 漲停：向下取最近的 tick 單位
	        adjustedPrice = Math.floor(limitPrice / tickSize) * tickSize;
	    } else {
	        // 跌停：向上取最近的 tick 單位
	        adjustedPrice = Math.ceil(limitPrice / tickSize) * tickSize;
	    }

	    // 格式化到小數點第 2 位
	    return Double.valueOf(String.format("%.2f", adjustedPrice));
	}

	// 根據價格區間返回 tick 單位
	public static double getTickSize(double price) {
	    if (price >= 0 && price < 10) {
	        return 0.01;
	    } else if (price >= 10 && price < 50) {
	        return 0.05;
	    } else if (price >= 50 && price < 100) {
	        return 0.1;
	    } else if (price >= 100 && price < 500) {
	        return 0.5;
	    } else if (price >= 500 && price < 1000) {
	        return 1.0;
	    } else if (price >= 1000 && price < 2000) {
	        return 5.0;
	    } else if (price >= 2000 && price < 5000) {
	        return 10.0;
	    } else if (price >= 5000 && price < 10000) {
	        return 50.0;
	    } else if (price >= 10000) {
	        return 100.0;
	    } else {
	        throw new IllegalArgumentException("無效的價格：" + price);
	    }
	}
	
	private static void initializeColumnNames() {
		SHAREHOLDER_STRUCTURE_COLUMN_NAME.put(0, "周別");
		SHAREHOLDER_STRUCTURE_COLUMN_NAME.put(1, "統計日期");
		SHAREHOLDER_STRUCTURE_COLUMN_NAME.put(2, "收盤");
		SHAREHOLDER_STRUCTURE_COLUMN_NAME.put(3, "漲跌(元)");
		SHAREHOLDER_STRUCTURE_COLUMN_NAME.put(4, "漲跌(%)");
		SHAREHOLDER_STRUCTURE_COLUMN_NAME.put(5, "集保庫存(萬張)");
		SHAREHOLDER_STRUCTURE_COLUMN_NAME.put(6, "<1張");
		SHAREHOLDER_STRUCTURE_COLUMN_NAME.put(7, "≧1張≦5張");
		SHAREHOLDER_STRUCTURE_COLUMN_NAME.put(8, ">5張≦10張");
		SHAREHOLDER_STRUCTURE_COLUMN_NAME.put(9, ">10張≦15張");
		SHAREHOLDER_STRUCTURE_COLUMN_NAME.put(10, ">15張≦20張");
		SHAREHOLDER_STRUCTURE_COLUMN_NAME.put(11, ">20張≦30張");
		SHAREHOLDER_STRUCTURE_COLUMN_NAME.put(12, ">30張≦40張");
		SHAREHOLDER_STRUCTURE_COLUMN_NAME.put(13, ">40張≦50張");
		SHAREHOLDER_STRUCTURE_COLUMN_NAME.put(14, ">50張≦100張");
		SHAREHOLDER_STRUCTURE_COLUMN_NAME.put(15, ">100張≦200張");
		SHAREHOLDER_STRUCTURE_COLUMN_NAME.put(16, ">200張≦400張");
		SHAREHOLDER_STRUCTURE_COLUMN_NAME.put(17, ">400張≦600張");
		SHAREHOLDER_STRUCTURE_COLUMN_NAME.put(18, ">600張≦800張");
		SHAREHOLDER_STRUCTURE_COLUMN_NAME.put(19, ">800張≦1千張");
		SHAREHOLDER_STRUCTURE_COLUMN_NAME.put(20, ">1千張");
		SHAREHOLDER_STRUCTURE_COLUMN_NAME.put(21, "總計");
		SHAREHOLDER_STRUCTURE_COLUMN_NAME.put(22, "<1張人數");
		SHAREHOLDER_STRUCTURE_COLUMN_NAME.put(23, "≧1張≦5張人數");
		SHAREHOLDER_STRUCTURE_COLUMN_NAME.put(24, ">5張≦10張人數");
		SHAREHOLDER_STRUCTURE_COLUMN_NAME.put(25, ">10張≦15張人數");
		SHAREHOLDER_STRUCTURE_COLUMN_NAME.put(26, ">15張≦20張人數");
		SHAREHOLDER_STRUCTURE_COLUMN_NAME.put(27, ">20張≦30張人數");
		SHAREHOLDER_STRUCTURE_COLUMN_NAME.put(28, ">30張≦40張人數");
		SHAREHOLDER_STRUCTURE_COLUMN_NAME.put(29, ">40張≦50張人數");
		SHAREHOLDER_STRUCTURE_COLUMN_NAME.put(30, ">50張≦100張人數");
		SHAREHOLDER_STRUCTURE_COLUMN_NAME.put(31, ">100張≦200張人數");
		SHAREHOLDER_STRUCTURE_COLUMN_NAME.put(32, ">200張≦400張人數");
		SHAREHOLDER_STRUCTURE_COLUMN_NAME.put(33, ">400張≦600張人數");
		SHAREHOLDER_STRUCTURE_COLUMN_NAME.put(34, ">600張≦800張人數");
		SHAREHOLDER_STRUCTURE_COLUMN_NAME.put(35, ">800張≦1千張人數");
		SHAREHOLDER_STRUCTURE_COLUMN_NAME.put(36, ">1千張人數");
		SHAREHOLDER_STRUCTURE_COLUMN_NAME.put(37, "股票代號");
		SHAREHOLDER_STRUCTURE_COLUMN_NAME.put(38, "持股總人數");
		STOCK_DAY_PRICE_COLUMN_NAME.put(0, "stock_code");
		STOCK_DAY_PRICE_COLUMN_NAME.put(1, "trading_day");
		STOCK_DAY_PRICE_COLUMN_NAME.put(2, "opening_price");
		STOCK_DAY_PRICE_COLUMN_NAME.put(3, "closing_price");
		STOCK_DAY_PRICE_COLUMN_NAME.put(4, "high_price");
		STOCK_DAY_PRICE_COLUMN_NAME.put(5, "low_price");

	}
}
