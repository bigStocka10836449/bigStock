package com.bigstock.sharedComponent.utils;

import java.net.URI;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.IsoFields;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.bigstock.sharedComponent.entity.MarginTradingAndShortSellingInfo;
import com.bigstock.sharedComponent.entity.StockDayPrice;
import com.esotericsoftware.minlog.Log;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class GrabThirdPartyStockDayPrice {

	private final RestTemplate restTemplate = new RestTemplate();
	private final ObjectMapper mapper = new ObjectMapper();

	public List<StockDayPrice> grabFromYahoo(String stockCode) {

		try {

			HttpHeaders headers = new HttpHeaders();
			headers.set("User-Agent", "Mozilla/5.0");
			headers.set("Accept", "application/json, text/plain, */*");
			headers.set("Referer", "https://tw.stock.yahoo.com/");
			headers.set("Origin", "https://tw.stock.yahoo.com");
			headers.set("Accept-Language", "zh-TW,zh;q=0.9,en;q=0.8");

			HttpEntity<Void> entityh = new HttpEntity<>(headers);

			String symbol = stockCode + ".TWO";

			String url = "https://tw.stock.yahoo.com/_td-stock/api/resource/StockServices.stockList"
					+ ";fields=avgPrice%2Corderbook" + ";symbols=" + symbol + "?lang=zh-Hant-TW&region=TW";

			ResponseEntity<String> response = restTemplate.exchange(URI.create(url), HttpMethod.GET, entityh,
					String.class);

			JsonNode root = mapper.readTree(response.getBody());

			JsonNode dataNode = root.get(0);

			StockDayPrice entity = new StockDayPrice();

			entity.setStockCode(stockCode);

			// trading time
			String marketTime = dataNode.path("regularMarketTime").asText();
			entity.setTradingDay(Date.from(Instant.parse(marketTime)));

			LocalDate today = entity.getTradingDay().toInstant().atZone(ZoneId.of("Asia/Taipei")).toLocalDate();
			// 設置本周第一天的日期
			LocalDate startOfWeekLocalDate = today.with(DayOfWeek.MONDAY);
			// 獲取系統默認時區
			ZoneId zoneId = ZoneId.of("Asia/Taipei");

			// 獲取偏移量
			ZoneOffset zoneOffset = zoneId.getRules().getOffset(startOfWeekLocalDate.atStartOfDay());

			// 設置本周最後一天的日期
			LocalDate endOfWeekLocalDate = today.with(DayOfWeek.SUNDAY);
			Date startOfWeeDate = Date.from(startOfWeekLocalDate.atStartOfDay().toInstant(zoneOffset));
			Date endOfWeekDate = Date.from(endOfWeekLocalDate.atStartOfDay().toInstant(zoneOffset));
			entity.setOpeningPrice(dataNode.path("regularMarketOpen").path("raw").asText());
			entity.setClosingPrice(dataNode.path("price").path("raw").asText());
			entity.setHighPrice(dataNode.path("regularMarketDayHigh").path("raw").asText());
			entity.setLowPrice(dataNode.path("regularMarketDayLow").path("raw").asText());
			entity.setStartOfWeekDate(startOfWeeDate);
			entity.setEndOfWeekDate(endOfWeekDate);
			entity.setWeekOfYear(endOfWeekLocalDate.getYear() + "W" + today.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
			entity.setChange(dataNode.path("change").path("raw").asText());

			String changePercent = dataNode.path("changePercent").asText();
			if (changePercent != null && changePercent.contains("%")) {
				entity.setChangeRate(Double.parseDouble(changePercent.replace("%", "")));
			}
			entity.setTradingVolume(dataNode.path("volume").asText());

			boolean limitUp = dataNode.path("limitUp").asBoolean();
			boolean limitDown = dataNode.path("limitDown").asBoolean();

			entity.setLimitUp(limitUp ? "Y" : "N");
			entity.setLimitDown(limitDown ? "Y" : "N");

			return List.of(entity);

		} catch (Exception e) {
			Log.warn(stockCode + " - error , so fast skip:" + e.getMessage());
			return Collections.emptyList();
		}
	}

//	public List<MarginTradingAndShortSellingInfo> grabMarginTradingAndShortSellingInfoFromYahoo(String stockCode) {
//		try {
//			HttpHeaders headers = new HttpHeaders();
//			headers.set("User-Agent", "Mozilla/5.0");
//			headers.set("Accept", "application/json, text/plain, */*");
//			headers.set("Referer", "https://tw.stock.yahoo.com/");
//			headers.set("Origin", "https://tw.stock.yahoo.com");
//			headers.set("Accept-Language", "zh-TW,zh;q=0.9,en;q=0.8");
//			String symbol = stockCode + ".TWO";
//			String url = "https://tw.stock.yahoo.com/_td-stock/api/resource/StockServices.creditsWithQuoteStats;limit=90;symbol="
//					+ symbol
//					+ "?bkt=%5B%22t3-stock-bts-p13n%22%2C%22t3-pc-twstock-hp-r1%22%2C%22twstock-pc-lumosv2-migration-t1%22%5D&device=desktop&ecma=modern&feature=enableGAMAds%2CenableGAMEdgeToEdge%2CenableEvPlayer%2CuseCG%2CuseCGV2%2CuseLumosV2Stock%2CuseLumosArticleP13n&intl=tw&lang=zh-Hant-TW&partner=none&prid=4nlht0pkr89p9&region=TW&site=finance&tz=Asia%2FTaipei&ver=1.4.826&returnMeta=true";
//			HttpEntity<Void> entityh = new HttpEntity<>(headers);
//			ResponseEntity<String> response = restTemplate.exchange(URI.create(url), HttpMethod.GET, entityh,
//					String.class);
//			JsonNode root = mapper.readTree(response.getBody());
//
//			JsonNode dataNode = root.get(0);
//		} catch (Exception e) {
//			Log.warn(stockCode + " - error , so fast skip:" + e.getMessage());
//			return Collections.emptyList();
//		}
//	}
}