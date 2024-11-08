package com.bigstock.schedule.bstockenum;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.bigstock.sharedComponent.entity.StockExchangeDetail;
import com.google.common.collect.Lists;

/**
 * 個網站執行抓取策略
 */
public enum CandlestickChartStrategy {

	SCANTRADER("SCANTRADER", "https://scantrader.com/v2/stock/%1s") {

		@Override
		List<StockExchangeDetail> executeStrategy(WebDriver driver, String stockCode, Date tradingDate) {
			String currentUrl = String.format(baseUrl, stockCode);
			driver.get(currentUrl);
			WebElement chartDiv = driver
					.findElement(By.xpath("//*[@id='__layout']/div/div/main/div/div/div/div[1]/section/div/div/div"));
			JavascriptExecutor js = (JavascriptExecutor) driver;
			js.executeScript("arguments[0].scrollIntoView(true);", chartDiv);
			js.executeScript("arguments[0].focus();", chartDiv);
			Actions actions = new Actions(driver);
			List<StockExchangeDetail> stockExchangeDetails = Lists.newArrayList();
			for (int index = 122; index <= 835; index++) {
				if (index == 122) {
					actions.moveByOffset(index, 400).click().perform();
				} else {
					actions.moveByOffset(1, 0).click().perform();
				}
				try {
					// 找到目標 <ul> 元素
					WebElement ulElement = driver.findElement(
							By.cssSelector("ul.flex.flex-wrap.gap-x-3.px-4.py-3.font-medium.lg\\:mb-1.lg\\:p-0"));
					String text = ulElement.getText();
					if (StringUtils.isBlank(text)) {
						continue;
					}
					List<String> textList = Arrays.asList(text.split("\n"));
					StockExchangeDetail stockExchangeDetail = new StockExchangeDetail();
					stockExchangeDetail
							.setExchangePrice(textList.get(1).replaceAll("[^0-9.]", " ").trim().split(" ")[0]);
					stockExchangeDetail.setExchangeQuantity(Integer.valueOf(textList.get(2).split(" ")[1].trim()));
					stockExchangeDetail.setExchangeTime(textList.get(0).split(" ")[1]);
					stockExchangeDetail.setTradingDate(tradingDate);
					stockExchangeDetail.setStockCode(stockCode);
					stockExchangeDetails.add(stockExchangeDetail);
				} catch (NoSuchElementException e) {
					continue;
				}
			}
			return stockExchangeDetails;
		}

	},

	ESTOCK("ESTOCK", "https://www.estock.com.tw/stock_info?stock_id=%1s") {

		@Override
		List<StockExchangeDetail> executeStrategy(WebDriver driver, String stockCode, Date tradingDate) {

			String currentUrl = String.format(baseUrl, stockCode);
			driver.get(currentUrl);
			WebElement canvas = driver.findElement(By.cssSelector("canvas[data-zr-dom-id='zr_0']"));
			JavascriptExecutor js = (JavascriptExecutor) driver;
			js.executeScript("arguments[0].scrollIntoView(true);", canvas);
			js.executeScript("arguments[0].focus();", canvas);
			Actions actions = new Actions(driver);
			List<StockExchangeDetail> stockExchangeDetails = Lists.newArrayList();
			for (int index = 258; index <= 800; index++) {
				if (index == 258) {
					actions.moveByOffset(258, 230).click().perform();
				} else {
					actions.moveByOffset(1, 0).click().perform();
				}
				try {
					// 找到目標元素
					WebElement targetDiv = driver.findElement(By.xpath(
							"//div[@style='font-size: 13px; font-family:Noto Sans TC;font-weight: 600;line-height: .9rem;']"));
					String text = targetDiv.getText();
					if (StringUtils.isBlank(text)) {
						continue;
					}
					// 提取內容
					List<String> textList = Arrays.asList(text.split("\n"));
					StockExchangeDetail stockExchangeDetail = new StockExchangeDetail();
					stockExchangeDetail.setExchangeTime(textList.get(0).split(" ")[1].trim());
					stockExchangeDetail.setExchangePrice(textList.get(1).replaceAll("[^0-9]", "").trim());
					stockExchangeDetail
							.setExchangeQuantity(Integer.valueOf(textList.get(3).replaceAll("[^0-9]", "").trim()));
					stockExchangeDetail.setStockCode(stockCode);
					stockExchangeDetail.setTradingDate(tradingDate);
					stockExchangeDetail.setSeq(index);
					stockExchangeDetails.add(stockExchangeDetail);
				} catch (NoSuchElementException e) {
					continue;
				}
			}
			return stockExchangeDetails;
		}

	},

//	WANTGOO("WANTGOO") {
//
//		@Override
//		void executeStrategy() {
//			// TODO Auto-generated method stub
//
//		}
//
//	},

	CNYES("CNYES", "https://www.cnyes.com/twstock/%1s") {
		@Override
		List<StockExchangeDetail> executeStrategy(WebDriver driver, String stockCode, Date tradingDate) {
			String currentUrl = String.format(baseUrl, stockCode);
			driver.get(currentUrl);

			WebElement chartDiv = driver.findElement(By.xpath(
					"//*[@id='anue-ga-wrapper']/div[4]/div[2]/div[1]/div[1]/div[2]/div/div[2]/div[1]/div/div/div/div[2]/table/tr[1]/td[2]/div"));
			JavascriptExecutor js = (JavascriptExecutor) driver;
			js.executeScript("arguments[0].scrollIntoView(true);", chartDiv);
			js.executeScript("arguments[0].focus();", chartDiv);
			Actions actions = new Actions(driver);
			List<StockExchangeDetail> stockExchangeDetails = Lists.newArrayList();
			for (int index = 565; index <= 1110; index++) {
				if (index == 565) {
					actions.moveByOffset(index, 200).click().perform();
				} else {
					actions.moveByOffset(1, 0).click().perform();
				}
				try {
					// 找到目標 <ul> 元素
					WebElement ulElement = driver.findElement(By.cssSelector("div.jsx-1257121542.tooltip"));
					String text = ulElement.getText();
					if (StringUtils.isBlank(text)) {
						continue;
					}
					List<String> textList = Arrays.asList(text.split("\n"));
					StockExchangeDetail stockExchangeDetail = new StockExchangeDetail();
					stockExchangeDetail.setTradingDate(tradingDate);
					stockExchangeDetail.setExchangePrice(textList.get(1).split(":")[1].trim());
					stockExchangeDetail.setExchangeTime(textList.get(0).trim());
					stockExchangeDetail
							.setExchangeQuantity(Integer.valueOf(textList.get(4).replaceAll("[^0-9]", "").trim()));
					stockExchangeDetail.setStockCode(stockCode);
					stockExchangeDetail.setSeq(index);
					stockExchangeDetails.add(stockExchangeDetail);
				} catch (NoSuchElementException e) {
					continue;
				}
			}
			return stockExchangeDetails;
		}
	},

	CMONY("CMONY", "https://www.cmoney.tw/finance/%1s/f00025") {
		@Override
		List<StockExchangeDetail> executeStrategy(WebDriver driver, String stockCode, Date tradingDate) {
			String currentUrl = String.format(baseUrl, stockCode);
			driver.get(currentUrl);

			WebElement ulElement = driver.findElement(By.id("instantKSwitch"));

			WebElement instantTrendLink = ulElement.findElement(By.cssSelector("a[chartswitch='0']"));

			((JavascriptExecutor) driver).executeScript("arguments[0].click();", instantTrendLink);
			WebElement chartDiv = driver.findElement(By.id("chart0"));

			JavascriptExecutor js = (JavascriptExecutor) driver;
			js.executeScript("arguments[0].scrollIntoView(true);", chartDiv);
			js.executeScript("arguments[0].focus();", chartDiv);

			Actions actions = new Actions(driver);

			driver.switchTo().frame("iframe_0");
			List<StockExchangeDetail> stockExchangeDetails = Lists.newArrayList();
			for (int offset = 361; offset <= 634; offset++) {
				if (offset == 361) {
					actions.moveByOffset(offset, 40).click().perform();
				} else {
					actions.moveByOffset(1, 0).click().perform();
				}
				try {
					WebElement targetElement = driver.findElement(By.cssSelector("div.highcharts-tooltip"));
					String text = targetElement.getText();
					if (StringUtils.isBlank(text)) {
						continue;
					}
					List<String> textList = Arrays.asList(text.split("\n"));
					StockExchangeDetail stockExchangeDetail = new StockExchangeDetail();
					stockExchangeDetail.setStockCode(stockCode);
					stockExchangeDetail.setTradingDate(tradingDate);
					stockExchangeDetail.setExchangeQuantity(Integer.valueOf(textList.get(2).split(" ")[1]));
					stockExchangeDetail.setExchangePrice(textList.get(1).split(" ")[1]);
					stockExchangeDetail.setExchangeTime(textList.get(0));
					stockExchangeDetail.setSeq(offset);
					stockExchangeDetails.add(stockExchangeDetail);
				} catch (NoSuchElementException e) {
					continue;
				}
			}
			return stockExchangeDetails;
		}
	},
	HISTOCK("HISTOCK", "https://histock.tw/stock/%1s") {

		@Override
		List<StockExchangeDetail> executeStrategy(WebDriver driver, String stockCode, Date tradingDate) {
			String currentUrl = String.format(baseUrl, stockCode);
			driver.get(currentUrl);
			WebElement chartDiv = driver.findElement(By.xpath("//*[@id='LBlock_0']/div[3]/div"));
			JavascriptExecutor js = (JavascriptExecutor) driver;
			js.executeScript("arguments[0].scrollIntoView(true);", chartDiv);
			js.executeScript("arguments[0].focus();", chartDiv);
			Actions actions = new Actions(driver);
			List<StockExchangeDetail> stockExchangeDetails = Lists.newArrayList();
			for (int index = 218; index <= 469; index++) {
				if (index == 218) {
					actions.moveByOffset(218, 100).click().perform();
				} else {
					actions.moveByOffset(1, 0).click().perform();
				}
				try {
					WebElement tooltipGroup = driver
							.findElement(By.cssSelector("g.highcharts-label.highcharts-tooltip.highcharts-color-0"));
					String text = tooltipGroup.getText();
					String[] parts = text.split("●");

					for (int i = 0; i < parts.length; i++) {
						parts[i] = parts[i].trim();
					}

					List<String> textList = Arrays.asList(parts);
					StockExchangeDetail stockExchangeDetail = new StockExchangeDetail();
					stockExchangeDetail.setStockCode(stockCode);
					stockExchangeDetail.setTradingDate(tradingDate);
					stockExchangeDetail.setExchangeQuantity(
							Float.valueOf(textList.get(2).split(" ")[1].replaceAll(",", "").trim()).intValue());
					stockExchangeDetail.setExchangePrice(textList.get(1).split(" ")[2].replaceAll(",", "").trim());
					stockExchangeDetail.setExchangeTime(textList.get(0).split(" ")[3].trim());
					stockExchangeDetail.setSeq(index);
					stockExchangeDetails.add(stockExchangeDetail);
				} catch (NoSuchElementException e) {
					continue;
				}
			}
			return stockExchangeDetails;
		}

	},
	NSTOCK("NSTOCK", "https://www.nstock.tw/stock_info?stock_id=%1s") {

		@Override
		List<StockExchangeDetail> executeStrategy(WebDriver driver, String stockCode, Date tradingDate) {
			String currentUrl = String.format(baseUrl, stockCode);
			driver.get(currentUrl);
			try {
				WebElement closeButton = driver.findElement(By.cssSelector(".popov-ad__close-icon"));
				closeButton.click();
			} catch (NoSuchElementException e) {
			}

			WebElement canvas = driver.findElement(By.cssSelector("canvas[data-zr-dom-id='zr_0']"));
			JavascriptExecutor js = (JavascriptExecutor) driver;
			js.executeScript("arguments[0].scrollIntoView(true);", canvas);
			js.executeScript("arguments[0].focus();", canvas);
			Actions actions = new Actions(driver);
			List<StockExchangeDetail> stockExchangeDetails = Lists.newArrayList();
			for (int index = 335; index <= 870; index++) {
				if (index == 335) {

					actions.moveByOffset(335, 200).click().perform();
				} else {
					actions.moveByOffset(1, 0).click().perform();

				}
				try {
					WebElement targetDiv = driver.findElement(By.xpath(
							"//div[@style='font-size: 13px; font-family:Noto Sans TC;font-weight: 600;line-height: .9rem;']"));

					String text = targetDiv.getText();
					if (StringUtils.isBlank(text)) {
						continue;
					}
					List<String> textList = Arrays.asList(text.split("\n"));
					StockExchangeDetail stockExchangeDetail = new StockExchangeDetail();
					stockExchangeDetail.setExchangeTime(textList.get(0).replaceAll(".*：", "").trim());
					stockExchangeDetail.setExchangeQuantity(Integer.valueOf(textList.get(2).replaceAll("[^0-9]", "")));
					stockExchangeDetail
							.setExchangePrice(textList.get(1).replaceAll("[^0-9]", " ").trim().split(" ")[0]);
					stockExchangeDetail.setStockCode(stockCode);
					stockExchangeDetail.setTradingDate(tradingDate);
					stockExchangeDetail.setSeq(index);
					stockExchangeDetails.add(stockExchangeDetail);
				} catch (NoSuchElementException e) {
					continue;
				}
			}
			return stockExchangeDetails;
		}

	},
	INEWS("INEWS", "https://inews.setn.com/stock/individual/%1s") {

		@Override
		List<StockExchangeDetail> executeStrategy(WebDriver driver, String stockCode, Date tradingDate) {
			String currentUrl = String.format(baseUrl, stockCode);
			driver.get(currentUrl);
			WebElement parentDiv = driver.findElement(By.xpath("//span[text()='價量明細']/parent::div"));
			WebElement nextSiblingDiv = parentDiv.findElement(By.xpath("following-sibling::div[1]"));

			// Find the nested div with classes "iframeBox" and "iframeLayBox"
			WebElement iframe = nextSiblingDiv.findElement(By.tagName("iframe"));

			driver.switchTo().frame(iframe);

			WebElement body = driver.findElement(By.tagName("body"));
			WebElement tbody = body.findElement(By.tagName("tbody"));
			JavascriptExecutor jsExecutor = (JavascriptExecutor) driver;
			String tbodyHtml = (String) jsExecutor.executeScript("return arguments[0].outerHTML;", tbody);
			Document tbodyDocument = Jsoup.parse(tbodyHtml);

			Pattern timePattern = Pattern.compile("\\b\\d{2}:\\d{2}:\\d{2}\\b");

			// List to store each cycle's elements as a map
			List<Map<String, String>> cycles = new ArrayList<>();
			Map<String, String> currentCycle = new HashMap<>();
			int elementCounter = 1;

			Element body2 = tbodyDocument.body();
			for (Node node : body2.childNodes()) {
				String textContent = null;

				if (node instanceof TextNode) {
					textContent = ((TextNode) node).text().trim();
				} else if (node instanceof Element) {
					textContent = ((Element) node).text().trim();
				}

				if (textContent != null && !textContent.isEmpty()) {
					if (timePattern.matcher(textContent).matches()) {
						if (!currentCycle.isEmpty()) {
							cycles.add(currentCycle); // Add the previous cycle to the list
						}
						currentCycle = new HashMap<>();
						elementCounter = 1; // Reset the counter for each new cycle
					}

					// Add the text content to the current cycle map with a unique key
					currentCycle.put("Text" + elementCounter++, textContent);
				}
			}
			List<StockExchangeDetail> stockExchangeDetails = Lists.newArrayList();
			for (int index = 0; index < cycles.size(); index++) {
				Map<String, String> cycle = cycles.get(index);
				StockExchangeDetail stockExchangeDetail = new StockExchangeDetail();
				stockExchangeDetail.setExchangePrice(cycle.get("Text5"));
				stockExchangeDetail.setExchangeQuantity(Integer.valueOf(cycle.get("Text6")));
				stockExchangeDetail.setExchangeTime(cycle.get("Text1"));
				stockExchangeDetail.setSeq(index + 1);
				stockExchangeDetail.setStockCode(stockCode);
				stockExchangeDetail.setTradingDate(tradingDate);
				stockExchangeDetails.add(stockExchangeDetail);
			}
			return stockExchangeDetails;
		}

	},
	PCHOME("PCHOME", "https://pchome.megatime.com.tw/stock/sto0/ock3/sid/%1s.html") {

		@Override
		List<StockExchangeDetail> executeStrategy(WebDriver driver, String stockCode, Date tradingDate) {
			String currentUrl = String.format(baseUrl, stockCode);
			driver.get(currentUrl);
			// 找到表格中的 canvas 元素
			// 找到目标 div 元素

			//
			WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
			try {
				WebElement fancybox = wait.until(
						ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id='fancybox-container-1']")));
				JavascriptExecutor js = (JavascriptExecutor) driver;
				js.executeScript("arguments[0].parentNode.removeChild(arguments[0]);", fancybox);
			} catch (Exception e) {
				
			}
			WebElement searchText = wait
					.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id='search_text']")));
			searchText.clear();
			// 使用 JavaScript 清空 value
			searchText.sendKeys(Keys.CONTROL + "a");
			searchText.sendKeys(Keys.BACK_SPACE);
			searchText.sendKeys(stockCode);
			WebElement inputButton = wait
					.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id='srch']/input")));
			inputButton.click();

//			boolean isExsits = stockExchangeDetailService.checkIsStockExchangeDetailExsits(stockCode, tradingDate);
//			if (isExsits || stockCode.startsWith("00")) {
//				continue;
//			}
			wait = new WebDriverWait(driver, Duration.ofSeconds(15));
			searchText = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id='search_text']")));
			searchText.clear();
			// 使用 JavaScript 清空 value
			searchText.sendKeys(Keys.CONTROL + "a");
			searchText.sendKeys(Keys.BACK_SPACE);
			searchText.sendKeys(stockCode);
			inputButton = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id='srch']/input")));
			inputButton.click();
			WebElement candlestick = wait.until(ExpectedConditions
					.visibilityOfElementLocated(By.xpath("//*[@id='cont-area']/div/div[3]/div[3]/ul/li[1]/a")));
			candlestick.click();
			//
			WebElement operateDetail = wait.until(ExpectedConditions
					.visibilityOfElementLocated(By.xpath("//*[@id='cont-area']/div/div[3]/div[4]/ul/li[4]/a")));
			operateDetail.click();
			// 從第二筆開始遍歷（排除掉第一筆資料）
			AtomicInteger uniqueSeqGenerator = new AtomicInteger(1);

			// Get the entire HTML source of the page
			String pageSource = driver.getPageSource();

			// Use Jsoup to parse the HTML content
			Document document = Jsoup.parse(pageSource);

			// Now you can use Jsoup to efficiently parse and extract data
			Elements tablerows = document.select("#tb_chart tbody tr"); // Adjust the selector as needed

			List<StockExchangeDetail> singleStockExchangeDetails = tablerows.stream().map(row -> {
				Elements cells = row.select("td");
				if (cells.size() >= 6) {
					String exchangeTime = cells.get(0).text(); // 第 4 個 td
					String exchangePrice = cells.get(3).text(); // 第 4 個 td
					String exchangeQuantity = cells.get(5).text(); // 第 6 個 td
					if ("分量(張)".equals(exchangeQuantity) || "成交價".equals(exchangePrice) || "時間".equals(exchangeTime)) {
						return null;
					}
					StockExchangeDetail stockExchangeDetail = new StockExchangeDetail();
					stockExchangeDetail.setStockCode(stockCode);
					stockExchangeDetail.setTradingDate(tradingDate);
					// 生成唯一碼
					int seq = uniqueSeqGenerator.getAndAdd(1);
					stockExchangeDetail.setExchangeQuantity(Integer.valueOf(exchangeQuantity));
					stockExchangeDetail.setSeq(seq);
					stockExchangeDetail.setExchangePrice(exchangePrice);
					stockExchangeDetail.setExchangeTime(exchangeTime);
					return stockExchangeDetail;
				} else {
					return null;
				}
			}).filter(stockExchangeDetail -> Optional.ofNullable(stockExchangeDetail).isPresent()).toList();
			return singleStockExchangeDetails;
		}
	};

	abstract List<StockExchangeDetail> executeStrategy(WebDriver driver, String stockCode, Date tradingDate);

	String stragegyName;

	String baseUrl;

	CandlestickChartStrategy(String stragegyName, String baseUrl) {
		this.stragegyName = stragegyName;
		this.baseUrl = baseUrl;
	}

	public String getStrategyName() {
		return this.stragegyName;
	}

	public static CandlestickChartStrategy valueOfStrategy(String name) {
		for (CandlestickChartStrategy strategy : values()) {
			if (strategy.getStrategyName().equalsIgnoreCase(name)) {
				return strategy;
			}
		}
		throw new IllegalArgumentException("No enum constant with name: " + name);
	}
}
