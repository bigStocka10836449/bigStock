package com.bigstock.schedule.utils;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.bigstock.sharedComponent.entity.StockDayPrice;
import com.bigstock.sharedComponent.entity.StockInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;

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

//	public static List<StockInfo> grepStockInfo(String chromeDriverPath, String overTheCounterUrl)
//			throws InterruptedException {
//		ChromeDriverService service = new ChromeDriverService.Builder()
//				.usingDriverExecutable(new File(chromeDriverPath)).usingAnyFreePort().build();
//
//		List<StockInfo> stockInfos = Lists.newArrayList();
//		ChromeOptions options = new ChromeOptions();
////		options.setBinary(linuxChromePath); // 指定Chrome的路徑
//		options.addArguments("--headless"); // 設定無頭模式
//		options.addArguments("--no-sandbox"); // 取消沙盒模式
//		options.addArguments("--disable-dev-shm-usage"); // 解決共享記憶體問題
//		WebDriver driver = new ChromeDriver(service, options);
//		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
//		try {
//			log.info("begining sync grepStockInfo ");
//			driver.get(overTheCounterUrl);
//
//			Thread.sleep(4000);
//			WebElement siiSelectElement = wait
//					.until(ExpectedConditions.presenceOfElementLocated((By.cssSelector("tbody select[name='TYPEK']"))));
//
//			// 使用 Select 类初始化
//			Select siiTypekSelect = new Select(siiSelectElement);
//
//			// 通过 value 属性设置选项值为 "otc"
//			siiTypekSelect.selectByValue("sii");
//			Thread.sleep(2000);
//			WebElement siiCcodeSelectElement = wait
//					.until(ExpectedConditions.presenceOfElementLocated((By.name("code"))));
//
//			// 使用 Select 类初始化
//			Select siiCodeSelect = new Select(siiCcodeSelectElement);
//
//			// 通过可见文本选择空白选项
//			siiCodeSelect.selectByVisibleText("");
//
//			WebElement siiSearchButton = wait.until(
//					ExpectedConditions.presenceOfElementLocated((By.cssSelector("div.search input[type='button']"))));
//			siiSearchButton.click();
//			Thread.sleep(2000);
////			// 点击按钮
////			searchButton.click();
//			wait.until(ExpectedConditions
//					.presenceOfElementLocated((By.xpath("//th[@class='tblHead' and contains(text(), '產業類別')]"))));
//			JavascriptExecutor siijs = (JavascriptExecutor) driver;
//			siijs.executeScript("window.scrollTo(0, document.body.scrollHeight);");
//			List<WebElement> siiEvenAndOldRows = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
//					By.xpath("//tr[contains(@class, 'even') or contains(@class, 'odd')]")));
//			for (WebElement webElement : siiEvenAndOldRows) {
//				List<WebElement> cells = webElement.findElements(By.tagName("td"));
//				StockInfo stockInfo = new StockInfo();
//				stockInfo.setStockCode(cells.get(0).getText().trim());
//				stockInfo.setStockName(cells.get(1).getText().trim());
//				stockInfo.setStockType("1");
//				stockInfos.add(stockInfo);
//			}
//
//			driver.get(overTheCounterUrl);
//			Thread.sleep(4000);
//			WebElement otcSelectElement = wait
//					.until(ExpectedConditions.presenceOfElementLocated((By.cssSelector("tbody select[name='TYPEK']"))));
//
//			// 使用 Select 类初始化
//			Select otcTypekSelect = new Select(otcSelectElement);
//
//			// 通过 value 属性设置选项值为 "otc"
//			otcTypekSelect.selectByValue("otc");
//			Thread.sleep(2000);
//			WebElement otcCcodeSelectElement = wait
//					.until(ExpectedConditions.presenceOfElementLocated((By.name("code"))));
//
//			// 使用 Select 类初始化
//			Select otcCodeSelect = new Select(otcCcodeSelectElement);
//
//			// 通过可见文本选择空白选项
//			otcCodeSelect.selectByVisibleText("");
//
//			WebElement otcSearchButton = wait.until(
//					ExpectedConditions.presenceOfElementLocated((By.cssSelector("div.search input[type='button']"))));
//			otcSearchButton.click();
//			Thread.sleep(2000);
////			// 点击按钮
////			searchButton.click();
//			wait.until(ExpectedConditions
//					.presenceOfElementLocated((By.xpath("//th[@class='tblHead' and contains(text(), '產業類別')]"))));
//			JavascriptExecutor js = (JavascriptExecutor) driver;
//			js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
//			List<WebElement> evenAndOldRows = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
//					By.xpath("//tr[contains(@class, 'even') or contains(@class, 'odd')]")));
//			for (WebElement webElement : evenAndOldRows) {
//				List<WebElement> cells = webElement.findElements(By.tagName("td"));
//				StockInfo stockInfo = new StockInfo();
//				stockInfo.setStockCode(cells.get(0).getText().trim());
//				stockInfo.setStockName(cells.get(1).getText().trim());
//				stockInfo.setStockType("0");
//				stockInfos.add(stockInfo);
//			}
//		} catch (Exception e) {
//			log.error(e.getMessage(), e);
//		} finally {
//			driver.quit();
//			log.info("finshed add stockInfos ");
//		}
//		return stockInfos;
//	}

	public static List<StockDayPrice> graspTwseDayPrice(String url, Date tradeDate) throws InterruptedException, JsonMappingException, JsonProcessingException, RestClientException, URISyntaxException {
		String jsonResponse = fetchApiData(url);

		ObjectMapper objectMapper = new ObjectMapper();
		List<Map<String, String>> responseList = objectMapper.readValue(jsonResponse,
				new TypeReference<List<Map<String, String>>>() {
				}).stream().filter(data -> {
					String code = data.get("Code").toString();
					return code.length() < 5 && !code.matches(".*[a-zA-Z].*");
				}).collect(Collectors.toList());
		LocalDate today = tradeDate.toInstant()
			      .atZone(ZoneId.systemDefault())
			      .toLocalDate();

		// 設置本周第一天的日期
		LocalDate startOfWeekLocalDate = today.with(DayOfWeek.MONDAY);

		// 設置本周最後一天的日期
		LocalDate endOfWeekLocalDate = today.with(DayOfWeek.SUNDAY);
		// 獲取系統默認時區
		ZoneId zoneId = ZoneId.systemDefault();

		// 獲取偏移量
		ZoneOffset zoneOffset = zoneId.getRules().getOffset(startOfWeekLocalDate.atStartOfDay());

		// 將 LocalDate 轉換為 Date
		Date startOfWeeDate = Date.from(startOfWeekLocalDate.atStartOfDay().toInstant(zoneOffset));
		Date endOfWeekDate = Date.from(endOfWeekLocalDate.atStartOfDay().toInstant(zoneOffset));

		return responseList.stream().map(map -> {
			StockDayPrice stockDayPrice = new StockDayPrice();
			stockDayPrice.setStockCode(map.get("Code"));
			stockDayPrice.setOpeningPrice(map.get("OpeningPrice"));
			stockDayPrice.setClosingPrice(map.get("ClosingPrice"));
			stockDayPrice.setHighPrice(map.get("HighestPrice"));
			stockDayPrice.setLowPrice(map.get("LowestPrice"));
			stockDayPrice.setChange(map.get("Change").replace("+", ""));
			stockDayPrice.setTradingDay(tradeDate);
			stockDayPrice.setStartOfWeekDate(startOfWeeDate);
			stockDayPrice.setEndOfWeekDate(endOfWeekDate);
			stockDayPrice.setWeekOfYear(today.getYear() + "W" + today.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
			return stockDayPrice;
		}).toList();
	}


	public static List<StockDayPrice> graspTpexDayPrice(String url) throws InterruptedException, JsonMappingException, JsonProcessingException, RestClientException, URISyntaxException {
		String jsonResponse = fetchApiData("https://www.tpex.org.tw/openapi/v1/tpex_mainboard_quotes");

		ObjectMapper objectMapper = new ObjectMapper();
		List<Map<String, String>> responseList = objectMapper.readValue(jsonResponse,
				new TypeReference<List<Map<String, String>>>() {
				}).stream().filter(data -> {
					String code = data.get("SecuritiesCompanyCode").toString();
					return code.length() < 5 && !code.matches(".*[a-zA-Z].*");
				}).collect(Collectors.toList());
		

		return responseList.stream().map(map -> {
			// 指定日期字符串格式
			DateTimeFormatter dateStringformatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");

			String monthAndDate = map.get("Date").substring(map.get("Date").length() - 4);
			int year = Integer.parseInt(map.get("Date").replace(monthAndDate, "")) + 1911; // 民国转换为西元
			String standardDateString = year + "/" + monthAndDate.substring(0, 2) + "/" + monthAndDate.substring(2, 4);

			// 解析标准日期字符串为 LocalDate 对象
			LocalDate localDate = LocalDate.parse(standardDateString, dateStringformatter);
			Date date = Date.from(localDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
			LocalDate today = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

			// 設置本周第一天的日期
			LocalDate startOfWeekLocalDate = today.with(DayOfWeek.MONDAY);

			// 設置本周最後一天的日期
			LocalDate endOfWeekLocalDate = today.with(DayOfWeek.SUNDAY);
			// 獲取系統默認時區
			ZoneId zoneId = ZoneId.systemDefault();

			// 獲取偏移量
			ZoneOffset zoneOffset = zoneId.getRules().getOffset(startOfWeekLocalDate.atStartOfDay());

			// 將 LocalDate 轉換為 Date
			Date startOfWeeDate = Date.from(startOfWeekLocalDate.atStartOfDay().toInstant(zoneOffset));
			Date endOfWeekDate = Date.from(endOfWeekLocalDate.atStartOfDay().toInstant(zoneOffset));
			StockDayPrice stockDayPrice = new StockDayPrice();
			stockDayPrice.setStockCode(map.get("SecuritiesCompanyCode"));
			stockDayPrice.setOpeningPrice(map.get("Open"));
			stockDayPrice.setClosingPrice(map.get("Close"));
			stockDayPrice.setHighPrice(map.get("High"));
			stockDayPrice.setLowPrice(map.get("Low"));
			stockDayPrice.setChange(map.get("Change"));
			stockDayPrice.setTradingDay(date);
			stockDayPrice.setStartOfWeekDate(startOfWeeDate);
			stockDayPrice.setEndOfWeekDate(endOfWeekDate);
			stockDayPrice.setWeekOfYear(today.getYear() + "W" + today.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
			return stockDayPrice;
		}).toList();
	}

	

	public static List<Map<Integer, String>> graspShareholderStructureFromTDCCApi(String tdccOpenApiUrl)
			throws JsonMappingException, JsonProcessingException, RestClientException, URISyntaxException {
		String jsonResponse = fetchApiData(tdccOpenApiUrl);

		ObjectMapper objectMapper = new ObjectMapper();
		List<Map<String, String>> responseList = objectMapper
				.readValue(jsonResponse, new TypeReference<List<Map<String, String>>>() {
				}).stream().filter(data -> {
					String code = data.get("證券代號").toString();
					return code.length() < 5 && !code.matches(".*[a-zA-Z].*");
				}).collect(Collectors.toList());

		Map<String, List<Map<String, String>>> groupResponseMap = new HashMap<>();
		for (Map<String, String> response : responseList) {
			groupResponseMap.computeIfAbsent(response.get("證券代號"), k -> new ArrayList<>()).add(response);
		}

		return groupResponseMap.entrySet().stream().map(set -> {
			List<Map<String, String>> innerList = set.getValue();
			Map<String, String> map = innerList.stream().findFirst().orElse(new HashMap<>());
			String date = map.get("﻿資料日期");
			String stockCode = map.get("證券代號");
			LocalDate localDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyyMMdd"));
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
			return code.length() < 5 && !code.matches(".*[a-zA-Z].*") && !market.contains("（終止上市(櫃)、興櫃)");
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
	
	public static List<StockDayPrice> getTpexStockHistory(Date startDate, Date endDate, String baseUrl, String stockCode) throws RestClientException, URISyntaxException, JsonMappingException, JsonProcessingException{
		Calendar startCalendar = Calendar.getInstance();
		startCalendar.setTime(startDate);
		Calendar endCalendar = Calendar.getInstance();
		endCalendar.setTime(endDate);
		List<StockDayPrice> allStockDayPrices = Lists.newArrayList();
		int totalStartDateMonth =  startCalendar.get(Calendar.YEAR) * 12 + startCalendar.get(Calendar.MONTH);
		int totalendDateMonth =  endCalendar.get(Calendar.YEAR) * 12 + endCalendar.get(Calendar.MONTH);
		int monthDiff = totalendDateMonth - totalStartDateMonth;
		if(monthDiff == 0) {
			monthDiff = 1 ;
		}
		for(int index = 0 ; index < monthDiff ; index++) {
			  SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
		        String formattedDate = dateFormat.format(startCalendar.getTime());
		        String realUrl = String.format(baseUrl, formattedDate, stockCode);
		        String jsonResponse = fetchApiData(realUrl);

				ObjectMapper objectMapper = new ObjectMapper();
				Map<String, Object> responseList = objectMapper.readValue(jsonResponse,
						new TypeReference<Map<String, Object>>() {
						});
				
				List<List<String>> stockPrices = (List<List<String>>) responseList.get("data");
				//[111/08/01, 6, 647, 106.00, 107.00, 106.00, 107.00, 0.00, 11]
				List<StockDayPrice> singleMonthStockDayPrices = stockPrices.stream().map(data -> {
					StockDayPrice stockPrice = new StockDayPrice();
					String tradingDateStr = data.get(0);
					  // 拆分民国日期字符串
			        String[] parts = tradingDateStr.split("/");
			        int innerTaiwanYear = Integer.parseInt(parts[0]); // 民国年份
			        int month = Integer.parseInt(parts[1]); // 月
			        int day = Integer.parseInt(parts[2]); // 日

			        // 将民国年份转换为公历年份
			        int year = innerTaiwanYear + 1911;

			        // 构造公历日期字符串
			        String gregorianDateStr = year + "/" + month + "/" + day;
			        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
			        Date tradingDate;
					try {
						tradingDate = sdf.parse(gregorianDateStr);
					} catch (ParseException e) {
						log.warn(e.getMessage(), e);
						tradingDate = new Date();
					}
			        
			        LocalDate today = tradingDate.toInstant()
			        		.atZone(ZoneId.systemDefault())
			        		.toLocalDate();

					// 設置本周第一天的日期
					LocalDate startOfWeekLocalDate = today.with(DayOfWeek.MONDAY);

					// 設置本周最後一天的日期
					LocalDate endOfWeekLocalDate = today.with(DayOfWeek.SUNDAY);
					// 獲取系統默認時區
					ZoneId zoneId = ZoneId.systemDefault();

					// 獲取偏移量
					ZoneOffset zoneOffset = zoneId.getRules().getOffset(startOfWeekLocalDate.atStartOfDay());

					// 將 LocalDate 轉換為 Date
					Date startOfWeeDate = Date.from(startOfWeekLocalDate.atStartOfDay().toInstant(zoneOffset));
					Date endOfWeekDate = Date.from(endOfWeekLocalDate.atStartOfDay().toInstant(zoneOffset));
			        
			        // 定义日期格式
					stockPrice.setTradingDay(tradingDate);
					stockPrice.setWeekOfYear(today.getYear() + "W" + today.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
					stockPrice.setStockCode(stockCode);
					stockPrice.setStartOfWeekDate(startOfWeeDate);
					stockPrice.setEndOfWeekDate(endOfWeekDate);
					stockPrice.setOpeningPrice(data.get(3));
					stockPrice.setClosingPrice(data.get(6));
					stockPrice.setHighPrice(data.get(4));
					stockPrice.setLowPrice(data.get(5));
					stockPrice.setChange(data.get(7).replace("+", ""));
					return stockPrice;
				}).toList();
				allStockDayPrices.addAll(singleMonthStockDayPrices);
		        startCalendar.add(Calendar.MONTH, 1);
		}
		return allStockDayPrices;
	}
	
	public static List<StockDayPrice> getTwseStockHistory(Date startDate, Date endDate, String baseUrl, String stockCode) throws RestClientException, URISyntaxException, JsonMappingException, JsonProcessingException{
		Calendar startCalendar = Calendar.getInstance();
		startCalendar.setTime(startDate);
		Calendar endCalendar = Calendar.getInstance();
		endCalendar.setTime(endDate);
		List<StockDayPrice> allStockDayPrices = Lists.newArrayList();
		int totalStartDateMonth =  startCalendar.get(Calendar.YEAR) * 12 + startCalendar.get(Calendar.MONTH);
		int totalendDateMonth =  endCalendar.get(Calendar.YEAR) * 12 + endCalendar.get(Calendar.MONTH);
		int monthDiff = totalendDateMonth - totalStartDateMonth;
		if(monthDiff == 0) {
			monthDiff = 1 ;
		}
		for(int index = 0 ; index < monthDiff ; index++) {
			  int taiwanYear = startCalendar.get(Calendar.YEAR) - 1911;
			  SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd");
		        String formattedDate = dateFormat.format(startCalendar.getTime());
		        String searchDateString = taiwanYear  + "/" +formattedDate;
		        String realUrl = String.format(baseUrl, searchDateString, stockCode);
		        String jsonResponse = fetchApiData(realUrl);

				ObjectMapper objectMapper = new ObjectMapper();
				Map<String, Object> responseList = objectMapper.readValue(jsonResponse,
						new TypeReference<Map<String, Object>>() {
						});
				
				List<List<String>> stockPrices = (List<List<String>>) responseList.get("aaData");
				//[111/08/01, 6, 647, 106.00, 107.00, 106.00, 107.00, 0.00, 11]
				List<StockDayPrice> singleMonthStockDayPrices = stockPrices.stream().map(data -> {
					StockDayPrice stockPrice = new StockDayPrice();
					String tradingDateStr = data.get(0);
					  // 拆分民国日期字符串
			        String[] parts = tradingDateStr.split("/");
			        int innerTaiwanYear = Integer.parseInt(parts[0]); // 民国年份
			        int month = Integer.parseInt(parts[1]); // 月
			        int day = Integer.parseInt(parts[2]); // 日

			        // 将民国年份转换为公历年份
			        int year = innerTaiwanYear + 1911;

			        // 构造公历日期字符串
			        String gregorianDateStr = year + "/" + month + "/" + day;
			        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
			        Date tradingDate;
					try {
						tradingDate = sdf.parse(gregorianDateStr);
					} catch (ParseException e) {
						log.warn(e.getMessage(), e);
						tradingDate = new Date();
					}
			        
			        LocalDate today = tradingDate.toInstant()
			        		.atZone(ZoneId.systemDefault())
			        		.toLocalDate();

					// 設置本周第一天的日期
					LocalDate startOfWeekLocalDate = today.with(DayOfWeek.MONDAY);

					// 設置本周最後一天的日期
					LocalDate endOfWeekLocalDate = today.with(DayOfWeek.SUNDAY);
					// 獲取系統默認時區
					ZoneId zoneId = ZoneId.systemDefault();

					// 獲取偏移量
					ZoneOffset zoneOffset = zoneId.getRules().getOffset(startOfWeekLocalDate.atStartOfDay());

					// 將 LocalDate 轉換為 Date
					Date startOfWeeDate = Date.from(startOfWeekLocalDate.atStartOfDay().toInstant(zoneOffset));
					Date endOfWeekDate = Date.from(endOfWeekLocalDate.atStartOfDay().toInstant(zoneOffset));
			        
			        // 定义日期格式
					stockPrice.setTradingDay(tradingDate);
					stockPrice.setWeekOfYear(today.getYear() + "W" + today.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
					stockPrice.setStockCode(stockCode);
					stockPrice.setStartOfWeekDate(startOfWeeDate);
					stockPrice.setEndOfWeekDate(endOfWeekDate);
					stockPrice.setOpeningPrice(data.get(3));
					stockPrice.setClosingPrice(data.get(6));
					stockPrice.setHighPrice(data.get(4));
					stockPrice.setLowPrice(data.get(5));
					String change = "--";
					if( !"--".equals(stockPrice.getClosingPrice()) && !"--".equals(stockPrice.getOpeningPrice())) {
						change = String.format("%.2f", Float.valueOf(stockPrice.getClosingPrice()) - Float.valueOf(stockPrice.getOpeningPrice()));
					}
					stockPrice.setChange(change);
					return stockPrice;
				}).toList();
				allStockDayPrices.addAll(singleMonthStockDayPrices);
		        startCalendar.add(Calendar.MONTH, 1);
		}
		return allStockDayPrices;
	}

	private static String fetchApiData(String url) throws URISyntaxException, RestClientException {
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.getMessageConverters().stream()
				.filter(converter -> converter instanceof org.springframework.http.converter.StringHttpMessageConverter)
				.forEach(converter -> ((org.springframework.http.converter.StringHttpMessageConverter) converter)
						.setDefaultCharset(StandardCharsets.UTF_8));
		ResponseEntity<String> responseEntity = restTemplate.exchange(new URI(url), HttpMethod.GET, null, String.class);
		return responseEntity.getBody();
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
