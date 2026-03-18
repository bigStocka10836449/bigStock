package com.bigstock.sharedComponent.utils;

import java.math.BigDecimal;
import java.net.URI;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
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

	public List<MarginTradingAndShortSellingInfo> grabMarginTradingAndShortSellingInfoFromYahoo(String stockCode) {
	    try {
	        HttpHeaders headers = new HttpHeaders();
	        headers.set("User-Agent", "Mozilla/5.0");
	        headers.set("Accept", "application/json, text/plain, */*");
	        headers.set("Referer", "https://tw.stock.yahoo.com/");
	        headers.set("Origin", "https://tw.stock.yahoo.com");
	        headers.set("Accept-Language", "zh-TW,zh;q=0.9,en;q=0.8");

	        String symbol = stockCode + ".TWO";
	        String url = "https://tw.stock.yahoo.com/_td-stock/api/resource/StockServices.creditsWithQuoteStats;limit=90;symbol="
	                + symbol
	                + "?bkt=%5B%22t3-stock-bts-p13n%22%2C%22t3-pc-twstock-hp-r1%22%2C%22twstock-pc-lumosv2-migration-t1%22%5D&device=desktop&ecma=modern&feature=enableGAMAds%2CenableGAMEdgeToEdge%2CenableEvPlayer%2CuseCG%2CuseCGV2%2CuseLumosV2Stock%2CuseLumosArticleP13n&intl=tw&lang=zh-Hant-TW&partner=none&prid=4nlht0pkr89p9&region=TW&site=finance&tz=Asia%2FTaipei&ver=1.4.826&returnMeta=true";

	        HttpEntity<Void> entity = new HttpEntity<>(headers);
	        ResponseEntity<String> response = restTemplate.exchange(
	                URI.create(url),
	                HttpMethod.GET,
	                entity,
	                String.class
	        );

	        JsonNode root = mapper.readTree(response.getBody());

	        JsonNode creditsNode = root.path("data")
	                .path("data")
	                .path("result")
	                .path("credits");

	        if (creditsNode.isMissingNode() || !creditsNode.isArray() || creditsNode.isEmpty()) {
	            return Collections.emptyList();
	        }

	        List<MarginTradingAndShortSellingInfo> result = new ArrayList<>();

	        for (JsonNode node : creditsNode) {
	            MarginTradingAndShortSellingInfo info = new MarginTradingAndShortSellingInfo();

	            String tradingDayStr = text(node, "date");
	            if (tradingDayStr != null && tradingDayStr.length() >= 10) {
					info.setTradingDay(Date.from(LocalDate.parse(tradingDayStr.substring(0, 10))
							.atStartOfDay(ZoneId.systemDefault()).toInstant()));
	            }

	            info.setStockCode(stockCode);

	            info.setOffsetting(text(node, "dayTradingVolK"));                  // offsetting
	            info.setMarginPurchase(text(node, "financingBuyVolK"));           // margin_purchase
	            info.setMarginSales(text(node, "financingSellVolK"));             // margin_sales
	            info.setCashRedemption(text(node, "financingPaybackVolK"));       // cash_redemption
	            info.setMarginPurchaseBalance(text(node, "financingTotalVolK"));  // margin_purchase_balance
	            info.setMarginPurchaseQuota(text(node, "financingLimitVolK"));    // margin_purchase_quota

	            info.setShortSale(text(node, "shortBuyVolK"));                    // short_sale
	            info.setShortConvering(text(node, "shortSellVolK"));              // short_convering
	            info.setShortSaleBalance(text(node, "shortTotalVolK"));           // short_sale_balance
	            info.setShortSaleQuota(text(node, "shortLimitVolK"));             // short_sale_quota

	            info.setStockRedemption(text(node, "shortRepayVolK"));            // stock_redemption

	            // 融資前日餘額 = 今日餘額 - 今日買進 + 今日賣出 + 今日現償
	            info.setMarginPurchaseBalancePreviousDay(
	                    calculatePreviousBalance(
	                            text(node, "financingTotalVolK"),
	                            text(node, "financingBuyVolK"),
	                            text(node, "financingSellVolK"),
	                            text(node, "financingPaybackVolK")
	                    )
	            );

	            // 融券前日餘額 = 今日餘額 - 今日融券賣出 + 今日買進券償還 + 今日現券償還
	            info.setShortSaleBalancePreviousDay(
	                    calculatePreviousBalance(
	                            text(node, "shortTotalVolK"),
	                            text(node, "shortSellVolK"),
	                            text(node, "shortBuyVolK"),
	                            text(node, "shortRepayVolK")
	                    )
	            );

	            result.add(info);
	        }

	        return result;
	    } catch (Exception e) {
	        Log.warn(stockCode + "- grabMarginTradingAndShortSellingInfoFromYahoo - error , so fast skip:" + e.getMessage(), e);
	        return Collections.emptyList();
	    }
	}
	
	private String text(JsonNode node, String fieldName) {
	    JsonNode field = node.get(fieldName);
	    if (field == null || field.isNull()) {
	        return null;
	    }
	    String value = field.asText();
	    return value == null || value.isBlank() ? null : value;
	}

	private String calculatePreviousBalance(String totalStr, String increaseStr, String decreaseStr, String redeemStr) {
	    try {
	        BigDecimal total = toBigDecimal(totalStr);
	        BigDecimal increase = toBigDecimal(increaseStr);
	        BigDecimal decrease = toBigDecimal(decreaseStr);
	        BigDecimal redeem = toBigDecimal(redeemStr);

	        // previous = total - increase + decrease + redeem
	        BigDecimal previous = total.subtract(increase).add(decrease).add(redeem);

	        // 去掉多餘小數，例如 1098 -> "1098"
	        return previous.stripTrailingZeros().toPlainString();
	    } catch (Exception e) {
	        return null;
	    }
	}

	private BigDecimal toBigDecimal(String value) {
	    if (value == null || value.isBlank()) {
	        return BigDecimal.ZERO;
	    }
	    return new BigDecimal(value.trim());
	}
}