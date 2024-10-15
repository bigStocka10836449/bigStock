package com.bigstock.schedule.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Point;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.bigstock.sharedComponent.entity.StockDayPrice;
import com.bigstock.sharedComponent.entity.StockInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffmpeg.PipeInput;
import com.github.kokorin.jaffree.ffmpeg.PipeOutput;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.speech.v1.RecognitionAudio;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.RecognizeResponse;
import com.google.cloud.speech.v1.SpeechClient;
import com.google.cloud.speech.v1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1.SpeechRecognitionResult;
import com.google.cloud.speech.v1.SpeechSettings;
import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;

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

	public static String testConvertAudio( ByteString audioBytes, String credentialsPath) throws IOException {
	    // 設置憑證文件的路徑 (將此路徑替換為你的 credentials.json 憑證文件路徑)
        String resultString = StringUtils.EMPTY;
        try {
            // 使用 GoogleCredentials 來加載憑證文件
            GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(credentialsPath));

            // 設置 SpeechClient 並將憑證文件應用到 SpeechClient 設置中
            SpeechSettings speechSettings = SpeechSettings.newBuilder()
                    .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                    .build();

            // 使用 SpeechClient 調用 API
            try (SpeechClient speechClient = SpeechClient.create(speechSettings)) {

                // 讀取音訊文件並轉換為 ByteString 格式

                // 設置音訊配置
                RecognitionConfig config = RecognitionConfig.newBuilder()
                        .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                        // 省略采样率，自动匹配音频文件的采样率
                        .setLanguageCode("en-US")
                        .build();

                // 設置音訊數據
                RecognitionAudio audio = RecognitionAudio.newBuilder()
                        .setContent(audioBytes)
                        .build();

                // 調用 API 進行語音識別
                RecognizeResponse response = speechClient.recognize(config, audio);

                // 解析 API 返回的結果
                List<SpeechRecognitionResult> results = response.getResultsList();
                
                for (SpeechRecognitionResult result : results) {
                    SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
                    System.out.printf("Transcription: %s%n", alternative.getTranscript());
                    resultString = alternative.getTranscript();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }
		return resultString;

	}
	
	public static void grepCanvas( String chromeDriverPath) {
		ChromeDriverService service = new ChromeDriverService.Builder()
				.usingDriverExecutable(new File(chromeDriverPath)).usingAnyFreePort().build();
		ChromeOptions options = new ChromeOptions();
		options.addArguments("--no-sandbox"); // 取消沙盒模式
		options.addArguments("--disable-dev-shm-usage"); // 解決共享記憶體問題
		WebDriver driver = new ChromeDriver(service, options);
		
		
			 driver.get("https://www.moneydj.com/Z/ZC/ZC0/ZC08/ZC08.djhtm?a=2330");
			    // 找到表格中的 canvas 元素
			  // 找到目标 div 元素

		        // 获取 iframe 元素
		        WebElement iframeElement = driver.findElement(By.id("SysJustStockSKV"));

		        // 获取 iframe 的位置
		        Point iframeLocation = iframeElement.getLocation();
		        int iframeX = iframeLocation.getX();
		        int iframeY = iframeLocation.getY();

		        // 定义需要点击的坐标位置（假设是 iframe 中的相对坐标）
		        int targetXInsideIframe = 720;  // 这是相对于 iframe 的 X 坐标
		        int targetYInsideIframe = 150;  // 这是相对于 iframe 的 Y 坐标
		        // 计算 iframe 最右侧的 X 坐标
		        int iframeWidth = iframeElement.getSize().getWidth();  // iframe 的宽度
		        int iframeRightX = iframeX + iframeWidth;
		        // 计算相对于整个页面的 X 和 Y 坐标
		        int targetX = iframeX + targetXInsideIframe;
		        int targetY = iframeY + targetYInsideIframe;

		        // 使用 Actions 模拟鼠标移动并点击
		        Actions actions = new Actions(driver);
		        actions.moveByOffset(targetX, targetY).click().perform();
		        actions.moveByOffset(iframeRightX-100, targetY).click().perform();


	}
	
	
	public static void grepTWSESsecuritiesFirmsDayOperate(String downloadFilepath, String chromeDriverPath, List<String> twseStockCodes, String bpythonUrl)
			throws InterruptedException {
		ChromeDriverService service = new ChromeDriverService.Builder()
				.usingDriverExecutable(new File(chromeDriverPath)).usingAnyFreePort().build();
 
        // 設定下載文件的偏好
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("profile.default_content_settings.popups", 0); // 禁止彈出窗口
        prefs.put("download.default_directory", downloadFilepath); // 設置下載目錄
        prefs.put("download.prompt_for_download", false); // 禁用下載詢問對話框
        prefs.put("download.directory_upgrade", true);
        prefs.put("safebrowsing.enabled", true); // 啟用安全下載
		ChromeOptions options = new ChromeOptions();
		options.addArguments("--headless"); // 設定無頭模式
		options.addArguments("--no-sandbox"); // 取消沙盒模式
		options.addArguments("--disable-dev-shm-usage"); // 解決共享記憶體問題
	      // 使用 setExperimentalOption 來應用這些偏好設置
        options.setExperimentalOption("prefs", prefs);
		WebDriver driver = new ChromeDriver(service, options);
		  // 打開目標頁面
        driver.get("https://bsr.twse.com.tw/bshtm/bsMenu.aspx");
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
		for(String twseStockCode:twseStockCodes) {
			boolean captchaResolved = false;
			
			while (!captchaResolved) {
				try {
					// 找到 CAPTCHA 圖片元素並截取屏幕
					WebElement captchaImage = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//img[contains(@src,'CaptchaImage.aspx')]")));
					File srcFile = captchaImage.getScreenshotAs(OutputType.FILE);
					
					// 將文件轉換為 byte[]
					byte[] fileContent = Files.readAllBytes(srcFile.toPath());
					
					// 將 byte[] 編碼為 Base64 字符串
					String base64Image = Base64.getEncoder().encodeToString(fileContent);
					
					// 構建 JSON 請求體
					String jsonRequest = "{\"image_byte\":\"" + base64Image + "\"}";
					
					// 發送 POST 請求到 Python 服務，獲取 CAPTCHA 結果
					String captchaText = sendPostRequest(jsonRequest, bpythonUrl);
					
					// 檢查 CAPTCHA 長度是否為 5
					if (captchaText != null && captchaText.length() == 5) {
						// 找到 CAPTCHA 輸入框，並將解析的文本輸入其中
						WebElement captchaInput = driver.findElement(By.name("CaptchaControl1"));
						captchaInput.clear();
						captchaInput.sendKeys(captchaText);
					
						// 找到股票輸入框並輸入股票代號
						WebElement stockInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("TextBox_Stkno")));
						stockInput.clear();
						stockInput.sendKeys(twseStockCode);
						
						// 找到提交按鈕並點擊
						WebElement submitButton = wait.until(ExpectedConditions.elementToBeClickable(By.id("btnOK")));
						submitButton.click();
						
					    // 嘗試找到錯誤訊息的 <span> 元素
			            WebElement errorMsg = driver.findElement(By.id("Label_ErrorMsg"));
			            if(StringUtils.isNotEmpty(errorMsg.getText())) {
			            	if(errorMsg.getText().contains( "查無資料") ) {
			            		break;
			            	}
			            	// CAPTCHA 長度不正確，點擊重設按鈕
							WebElement resetButton = driver.findElement(By.id("Button_Reset"));
							resetButton.click();
							continue;
			            }
						
						WebElement downloadLink = wait.until(ExpectedConditions.elementToBeClickable(By.id("HyperLink_DownloadCSV")));
						if (downloadLink != null) {
							// 點擊下載鏈接
							downloadLink.click();
							Thread.sleep(2000);
						}
						captchaResolved = true; // 退出循環
					} else {
						// CAPTCHA 長度不正確，點擊重設按鈕
						WebElement resetButton = driver.findElement(By.id("Button_Reset"));
						resetButton.click();
						continue;
					}
					
				} catch (Exception e) {
					log.warn(e.getMessage(),e);
				}
			}
		}
	}

	
	public static void grepTPEXsecuritiesFirmsDayOperate(String downloadFilepath, String chromeDriverPath, List<String> tpexStockCodes, String credentialsPath)
	        throws InterruptedException {
	    ChromeDriverService service = new ChromeDriverService.Builder()
	            .usingDriverExecutable(new File(chromeDriverPath)).usingAnyFreePort().build();

	    // 設定下載文件的偏好
	    Map<String, Object> prefs = new HashMap<>();
	    prefs.put("profile.default_content_settings.popups", 0); // 禁止彈出窗口
	    prefs.put("download.default_directory", downloadFilepath); // 設置下載目錄
	    prefs.put("download.prompt_for_download", false); // 禁用下載詢問對話框
	    prefs.put("download.directory_upgrade", true);
	    prefs.put("safebrowsing.enabled", true); // 啟用安全下載

	    ChromeOptions options = new ChromeOptions();
	    options.addArguments("--lang=zh-TW"); 
	    options.addArguments("--headless"); // 設定無頭模式
	    options.addArguments("--no-sandbox"); // 取消沙盒模式
	    options.addArguments("--disable-dev-shm-usage"); // 解決共享記憶體問題
	    options.setCapability("goog:loggingPrefs", new HashMap<String, String>() {{
	        put("performance", "ALL");
	    }});
	    options.setExperimentalOption("prefs", prefs);

	    
	    
	    
	    WebDriver driver = new ChromeDriver(service, options);
	    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
	    
	
		
		
	    driver.get("https://www.tpex.org.tw/web/stock/aftertrading/broker_trading/brokerBS.php?l=zh-tw");
	    Thread.sleep(1000);
	    log.info("start driver------------------");
	    WebElement test =  driver.findElement(By.xpath("/html/body/center/div[3]/div[2]/div[1]"));
	    
		  log.info("testElement info------------------{}",test.toString());
		// 获取所有 iframe 元素
		  List<WebElement> iframes = driver.findElements(By.tagName("iframe"));
		// 打印每个 iframe 的 title 属性
		  for (WebElement iframe : iframes) {
		      String iframeTitle = iframe.getAttribute("title");
		      System.out.println("Iframe title: " + iframeTitle);
		  }
	    // CAPTCHA 判斷與處理
	    WebElement reCAPTCHA = driver.findElement(By.cssSelector("iframe[title='reCAPTCHA']"));
	    boolean captchaResolved = ObjectUtils.isEmpty(reCAPTCHA);
	    while (!captchaResolved) {
	        try {
	            reCAPTCHA = driver.findElement(By.cssSelector("iframe[title='reCAPTCHA']"));
	            driver.switchTo().frame(reCAPTCHA);
	            WebElement recaptchaElement = wait.until(
	                ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id='recaptcha-anchor']/div[2]"))
	            );

	            if (recaptchaElement != null) {
	                Actions actions = new Actions(driver);
	                actions.moveToElement(recaptchaElement).click().perform();
	                driver.switchTo().parentFrame();

	                try {
	                    driver.switchTo().frame(wait.until(
	                        ExpectedConditions.visibilityOfElementLocated(By.xpath("//iframe[@title='recaptcha challenge expires in two minutes']"))
	                    ));
	                } catch (TimeoutException ex) {
	                	log.warn(ex.getMessage(),ex);
	                    break;
	                }

	                WebElement recaptchaAudioButton = wait.until(
	                    ExpectedConditions.visibilityOfElementLocated((By.id("recaptcha-audio-button")))
	                );

	                Actions audioActions = new Actions(driver);
	                audioActions.moveToElement(recaptchaAudioButton).click().perform();

	                WebElement audioDownloadLink = wait.until(
	                    ExpectedConditions.visibilityOfElementLocated(By.className("rc-audiochallenge-tdownload-link"))
	                );

	                String dynamicHref = audioDownloadLink.getAttribute("href");
	                RestTemplate restTemplate = new RestTemplate();
	                ResponseEntity<byte[]> response = restTemplate.exchange(dynamicHref, HttpMethod.GET, null, byte[].class);

	                byte[] mp3Bytes = response.getBody();
	                ByteString audioBytes = convertMp3ToWav(mp3Bytes);
	                String audioResult = testConvertAudio(audioBytes, credentialsPath);

	                WebElement inputField = wait.until(
	                    ExpectedConditions.visibilityOfElementLocated(By.id("audio-response"))
	                );
	                inputField.sendKeys(audioResult);
	                System.out.println("解析内容已输入到 reCAPTCHA 音频输入框中。");

	                WebElement verifyButton = wait.until(
	                    ExpectedConditions.visibilityOfElementLocated(By.id("recaptcha-verify-button"))
	                );
	                verifyButton.click();
	                driver.switchTo().parentFrame();
	                captchaResolved = true;
	            }

	        } catch (Exception e) {
	            log.warn(e.getMessage(), e);
	            break;
	        }
	    }

	    // 處理每個股票代碼
	    List<String> copyTpexStockCodes = Lists.newArrayList();
	    try {
	        for (int index = 0; index < tpexStockCodes.size(); index++) {
	            String tpexStockCode = tpexStockCodes.get(index);
	            WebElement stockCodeInput = null;

	            while (true) {
	                try {
	                    stockCodeInput = wait.until(
	                        ExpectedConditions.visibilityOfElementLocated(By.id("stk_code"))
	                    );
	                } catch (TimeoutException timeoutException) {
	                	log.info("error stkCode: {}",tpexStockCode);
	                    throw timeoutException;
	                }

	                stockCodeInput.clear();
	                stockCodeInput.sendKeys(tpexStockCode);

	                WebElement queryButton = wait.until(
	                    ExpectedConditions.visibilityOfElementLocated(By.xpath("//button[@class='btn btn-primary' and @type='button' and @onclick='checkForm()']"))
	                );
	                queryButton.click();

	                try {
	                    WebElement downloadButton = wait.until(
	                        ExpectedConditions.visibilityOfElementLocated(By.xpath("//button[@class='btn btn-default' and @type='button' and @onclick=\"downloadCSV('BIG5')\"]"))
	                    );
	                    downloadButton.click();
	                } catch (TimeoutException er) {
	                    WebElement reCAPTCHAag;
	                    try {
	                        reCAPTCHAag = driver.findElement(By.cssSelector("iframe[title='reCAPTCHA']"));
	                    } catch (NoSuchElementException err) {
	                    	 log.warn(err.getMessage(), err);
	                        break;
	                    }

	                    boolean captchaResolvedag = ObjectUtils.isEmpty(reCAPTCHAag);
	                    while (!captchaResolvedag) {
	                        try {
	                            driver.switchTo().frame(reCAPTCHAag);

	                            WebElement recaptchaElement = wait.until(
	                                ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id='recaptcha-anchor']/div[2]"))
	                            );

	                            if (recaptchaElement != null) {
	                                Actions actions = new Actions(driver);
	                                actions.moveToElement(recaptchaElement).click().perform();
	                                driver.switchTo().parentFrame();

	                                try {
	                                    driver.switchTo().frame(wait.until(
	                                        ExpectedConditions.visibilityOfElementLocated(By.xpath("//iframe[@title='reCAPTCHA 驗證問題將在兩分鐘後失效']"))
	                                    ));
	                                } catch (TimeoutException ex) {
	                                    break;
	                                }

	                                WebElement recaptchaAudioButton = wait.until(
	                                    ExpectedConditions.visibilityOfElementLocated((By.id("recaptcha-audio-button")))
	                                );

	                                Actions audioActions = new Actions(driver);
	                                audioActions.moveToElement(recaptchaAudioButton).click().perform();

	                                WebElement audioDownloadLink = wait.until(
	                                    ExpectedConditions.visibilityOfElementLocated(By.className("rc-audiochallenge-tdownload-link"))
	                                );

	                                String dynamicHref = audioDownloadLink.getAttribute("href");
	                                RestTemplate restTemplate = new RestTemplate();
	                                ResponseEntity<byte[]> response = restTemplate.exchange(dynamicHref, HttpMethod.GET, null, byte[].class);

	                                byte[] mp3Bytes = response.getBody();
	                                ByteString audioBytes = convertMp3ToWav(mp3Bytes);
	                                String audioResult = testConvertAudio(audioBytes, credentialsPath);

	                                WebElement inputField = wait.until(
	                                    ExpectedConditions.visibilityOfElementLocated(By.id("audio-response"))
	                                );
	                                inputField.sendKeys(audioResult);
	                                System.out.println("解析内容已输入到 reCAPTCHA 音频输入框中。");

	                                WebElement verifyButton = wait.until(
	                                    ExpectedConditions.visibilityOfElementLocated(By.id("recaptcha-verify-button"))
	                                );
	                                verifyButton.click();
	                                driver.switchTo().parentFrame();
	                                captchaResolvedag = true;
	                            }

	                        } catch (Exception e) {
	                        	throw e;
	                        }
	                    }

	                    WebElement stockCodeInputag = null;
	                    try {
	                        stockCodeInputag = wait.until(
	                            ExpectedConditions.visibilityOfElementLocated(By.id("stk_code"))
	                        );
	                        stockCodeInputag.clear();
	                        stockCodeInputag.sendKeys(tpexStockCode);

	                        WebElement queryButtonag = wait.until(
	                            ExpectedConditions.visibilityOfElementLocated(By.xpath("//button[@class='btn btn-primary' and @type='button' and @onclick='checkForm()']"))
	                        );
	                        queryButtonag.click();

	                        WebElement downloadButton = wait.until(
	                            ExpectedConditions.visibilityOfElementLocated(By.xpath("//button[@class='btn btn-default' and @type='button' and @onclick=\"downloadCSV('BIG5')\"]"))
	                        );
	                        downloadButton.click();
	                        File folder = new File(downloadFilepath);
	                        log.info("downloadFilepath :{}",folder.getPath());
	                        log.info("folder Sile :{}", folder.list().length);
	                    } catch (TimeoutException timeoutException) {
	                       throw timeoutException;
	                    }
	                }

	                copyTpexStockCodes.add(tpexStockCode);
	                Thread.sleep(3500);
	                break;
	            }
	        }
	    } catch (Exception e) {
	    	log.warn(e.getMessage(),e);
	    	log.info("retry grepTPEXsecuritiesFirmsDayOperate-----------------");
	        driver.quit();
	        List<String> notDownloadStockCode = tpexStockCodes.stream()
	                .filter(stockCode -> !copyTpexStockCodes.contains(stockCode))
	                .toList();
	        grepTPEXsecuritiesFirmsDayOperate(downloadFilepath, chromeDriverPath, notDownloadStockCode, credentialsPath);
	    }
	}

	
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
	
    
    // 發送 POST 請求到 Python 服務並返回 CAPTCHA 結果
    private static String sendPostRequest(String jsonRequest, String bpythonUrl) {
        RestTemplate restTemplate = new RestTemplate();

        // 設置標頭
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/json");

        // 創建HttpEntity
        HttpEntity<String> requestEntity = new HttpEntity<>(jsonRequest, headers);

        // 發送 POST 請求
        ResponseEntity<String> response = restTemplate.exchange(bpythonUrl, HttpMethod.POST, requestEntity, String.class);

        // 檢查響應狀態
        if (response.getStatusCode() == HttpStatus.OK) {
            System.out.println("Response from server: " + response.getBody());
            JSONObject responseJson = new JSONObject(response.getBody());
            return responseJson.getString("result");  // 返回服務器的 CAPTCHA 結果
        } else {
            System.out.println("Failed to send request. Status code: " + response.getStatusCode());
            return null;
        }
    }
    
    
    public static ByteString convertMp3ToWav(byte[] mp3Bytes) throws IOException {
        // 創建 PipedInputStream 和 PipedOutputStream 來連接 FFmpeg 的輸入和輸出
        // 創建輸入和輸出流
        ByteArrayInputStream inputStream = new ByteArrayInputStream(mp3Bytes);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // 使用 Jaffree 來執行 FFmpeg 轉換
		FFmpeg.atPath() // 默認會從系統路徑中尋找 ffmpeg，也可以用 atPath(Paths.get("path-to-ffmpeg")) 來指定
				.addInput(PipeInput.pumpFrom(inputStream)) // 從 ByteArrayInputStream 中讀取 MP33
				.addOutput(PipeOutput.pumpTo(outputStream) // 輸出到 ByteArrayOutputStream
						.addArguments("-ac", "1") // 設置音頻為單聲道
						.addArguments("-f", "wav")) // 設置輸出格式為 WAV
				.execute();
        // 將輸出流中的數據轉換為 ByteString
        return ByteString.copyFrom(outputStream.toByteArray());
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
