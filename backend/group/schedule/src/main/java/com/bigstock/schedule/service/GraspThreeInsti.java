package com.bigstock.schedule.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.compress.utils.Lists;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.bigstock.sharedComponent.dto.ThreeInstitutionalTradingResponse;
import com.bigstock.sharedComponent.entity.StockDayPrice;
import com.bigstock.sharedComponent.entity.StockDayPriceRank;
import com.bigstock.sharedComponent.entity.StockInfo;
import com.bigstock.sharedComponent.entity.StockThreeInstitutionalTrading;
import com.bigstock.sharedComponent.service.RankStockChangeService;
import com.bigstock.sharedComponent.service.StockDayPriceService;
import com.bigstock.sharedComponent.service.StockInfoService;
import com.bigstock.sharedComponent.service.StockThreeInstitutionalTradingService;
import com.bigstock.sharedComponent.utils.ChromeDriverUtils;
import com.bigstock.sharedComponent.utils.GrabThirdPartyStockDayPrice;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class GraspThreeInsti {

	private final StockThreeInstitutionalTradingService stockThreeInstitutionalTradingService;

	private final StockDayPriceService stockDayPriceService;

	private final GrabThirdPartyStockDayPrice grabThirdPartyStockDayPrice;
	
	private final StockInfoService stockInfoService;
	
	private final RankStockChangeService rankStockChangeService;

	@Scheduled(cron = "${schedule.task.scheduling.cron.expression.update-threeInsti}", zone = "Asia/Taipei")
	@Transactional
//	@PostConstruct
	public void updateThreeInsti() throws Exception {

		List<StockInfo> allStockInfos =  stockInfoService.getAllStockCode().stream().filter(data -> {
			return !data.getStockCode().matches(".*[a-zA-Z].*");
		}).toList();
		Map<String, List<StockInfo>> allStockInfoMap =
				allStockInfos.stream()
			        .collect(Collectors.groupingBy(StockInfo::getStockCode));
		// 抓台積電今日交易資訊當作日期參考
		List<StockDayPrice> stockTpexDayPrices = grabThirdPartyStockDayPrice.grabFromYahoo("2330");
		Date tradeDate = stockTpexDayPrices.stream().findFirst().get().getTradingDay();
		LocalDate lod = tradeDate.toInstant().atZone(ZoneId.of("Asia/Taipei")).toLocalDate();
		String yyyyMMdd = lod.getYear()
				+ (lod.getMonthValue() <= 9 ? "0" + lod.getMonthValue() : String.valueOf(lod.getMonthValue()))
				+ (lod.getDayOfMonth() <= 9 ? "0" + lod.getDayOfMonth() : String.valueOf(lod.getDayOfMonth()));

		List<ThreeInstitutionalTradingResponse> twseThreeInstitutionalTradingResponses = ChromeDriverUtils
				.grabThreeInstiTwse(yyyyMMdd);
		List<ThreeInstitutionalTradingResponse> tpexThreeInstitutionalTradingResponses = ChromeDriverUtils
				.grabThreeInstiTpex(yyyyMMdd);
		stockThreeInstitutionalTradingService.syncFromRedisToDb(yyyyMMdd, twseThreeInstitutionalTradingResponses,
				tpexThreeInstitutionalTradingResponses);

		// 刪除100個日期前的資料(可能會因為假日而有誤差，但是只要保留作新的60天左右的資料即可)
		LocalDate tradeDateMinus100 = lod.minusDays(100);
		stockThreeInstitutionalTradingService.deleteByRankNoLessThanZero(tradeDateMinus100);
		List<StockThreeInstitutionalTrading> stockThreeInstitutionalTradings = stockThreeInstitutionalTradingService
				.getLastThreeSingleStockData("2330");
		LocalDate endLocalDate = stockThreeInstitutionalTradings.get(0).getId().getTradeDate();
		LocalDate beginLocalDate = stockThreeInstitutionalTradings.get(2).getId().getTradeDate();
		List<StockThreeInstitutionalTrading> dataRangeStockThreeInstitutionalTradings = stockThreeInstitutionalTradingService
				.getDateRangMarketStockData(beginLocalDate, endLocalDate);
		Map<String, List<StockThreeInstitutionalTrading>> dataRangeStockThreeInstitutionalTradingsMap = dataRangeStockThreeInstitutionalTradings
				.stream().collect(Collectors.groupingBy(e -> e.getId().getStockCode()));
		List<StockThreeInstitutionalTrading> foreignOverSells = Lists.newArrayList();
		List<StockThreeInstitutionalTrading> foreignOverBuys = Lists.newArrayList();
		List<StockThreeInstitutionalTrading> investmentTrustOverSells = Lists.newArrayList();
		List<StockThreeInstitutionalTrading> investmentTrustOverBuys = Lists.newArrayList();
		List<StockThreeInstitutionalTrading> dealerOverSells = Lists.newArrayList();
		List<StockThreeInstitutionalTrading> dealerOverBuys = Lists.newArrayList();
		dataRangeStockThreeInstitutionalTradingsMap.entrySet().stream().forEach(entry -> {
			String stockCode = entry.getKey();
			List<StockThreeInstitutionalTrading> stockCodeStockThreeInstitutionalTradings = entry.getValue();
			Long foreignTotal = stockCodeStockThreeInstitutionalTradings.stream()
					.mapToLong(data -> data.getForeignBuy() - data.getForeignSell()).sum();
			Long dealerTotal = stockCodeStockThreeInstitutionalTradings.stream()
					.mapToLong(data -> data.getDealerBuy() - data.getDealerSell()).sum();
			Long investmentTrustTotal = stockCodeStockThreeInstitutionalTradings.stream()
					.mapToLong(data -> data.getInvestmentTrustBuy() - data.getInvestmentTrustSell()).sum();
			if (foreignTotal > 0L) {
				foreignOverBuys.add(stockCodeStockThreeInstitutionalTradings.stream().findFirst().get());
			}

			if (foreignTotal < 0L) {
				foreignOverSells.add(stockCodeStockThreeInstitutionalTradings.stream().findFirst().get());
			}
			if (investmentTrustTotal > 0L) {
				investmentTrustOverBuys.add(stockCodeStockThreeInstitutionalTradings.stream().findFirst().get());
			}
			if (investmentTrustTotal < 0L) {
				investmentTrustOverSells.add(stockCodeStockThreeInstitutionalTradings.stream().findFirst().get());
			}
			if (dealerTotal > 0L) {
				dealerOverBuys.add(stockCodeStockThreeInstitutionalTradings.stream().findFirst().get());
			}
			if (dealerTotal < 0L) {
				dealerOverSells.add(stockCodeStockThreeInstitutionalTradings.stream().findFirst().get());
			}
		});
		Map<String, List<StockDayPriceRank>> stockDayPriceRanksMap = stockDayPriceService.findByIdTradingDay(lod)
				.stream().collect(Collectors.groupingBy(StockDayPriceRank::getStockCode));
		List<StockDayPriceRank> foreignOverSellStockDayPriceRanks = Lists.newArrayList();
		List<StockDayPriceRank> foreignOverBuyStockDayPriceRanks = Lists.newArrayList();
		List<StockDayPriceRank> investmentTrustOverSellStockDayPriceRanks = Lists.newArrayList();
		List<StockDayPriceRank> investmentTrustOverBuyStockDayPriceRanks = Lists.newArrayList();
		List<StockDayPriceRank> dealerOverSellStockDayPriceRanks = Lists.newArrayList();
		List<StockDayPriceRank> dealerOverBuyStockDayPriceRanks = Lists.newArrayList();
		foreignOverBuys.forEach(data -> {
			List<StockDayPriceRank> innerStockDayPriceRanks = stockDayPriceRanksMap.get(data.getId().getStockCode());
			if (CollectionUtils.isNotEmpty(innerStockDayPriceRanks)) {
				foreignOverBuyStockDayPriceRanks.addAll(innerStockDayPriceRanks);
			}
		});
		foreignOverSells.forEach(data -> {
			List<StockDayPriceRank> innerStockDayPriceRanks = stockDayPriceRanksMap.get(data.getId().getStockCode());
			if (CollectionUtils.isNotEmpty(innerStockDayPriceRanks)) {
				foreignOverSellStockDayPriceRanks.addAll(innerStockDayPriceRanks);
			}
		});
		investmentTrustOverBuys.forEach(data -> {
			List<StockDayPriceRank> innerStockDayPriceRanks = stockDayPriceRanksMap.get(data.getId().getStockCode());
			if (CollectionUtils.isNotEmpty(innerStockDayPriceRanks)) {
				investmentTrustOverBuyStockDayPriceRanks.addAll(innerStockDayPriceRanks);
			}
		});
		investmentTrustOverSells.forEach(data -> {
			List<StockDayPriceRank> innerStockDayPriceRanks = stockDayPriceRanksMap.get(data.getId().getStockCode());
			if (CollectionUtils.isNotEmpty(innerStockDayPriceRanks)) {
				investmentTrustOverSellStockDayPriceRanks.addAll(innerStockDayPriceRanks);
			}
		});
		dealerOverBuys.forEach(data -> {
			List<StockDayPriceRank> innerStockDayPriceRanks = stockDayPriceRanksMap.get(data.getId().getStockCode());
			if (CollectionUtils.isNotEmpty(innerStockDayPriceRanks)) {
				dealerOverBuyStockDayPriceRanks.addAll(innerStockDayPriceRanks);
			}
		});
		dealerOverSells.forEach(data -> {
			List<StockDayPriceRank> innerStockDayPriceRanks = stockDayPriceRanksMap.get(data.getId().getStockCode());
			if (CollectionUtils.isNotEmpty(innerStockDayPriceRanks)) {
				dealerOverSellStockDayPriceRanks.addAll(innerStockDayPriceRanks);
			}
		});
		rankStockChangeService.writeStockListRankToRedis(foreignOverSellStockDayPriceRanks, allStockInfoMap, "foreignOverSell");
		rankStockChangeService.writeStockListRankToRedis(foreignOverBuyStockDayPriceRanks, allStockInfoMap,"foreignOverBuy");
		rankStockChangeService.writeStockListRankToRedis(investmentTrustOverBuyStockDayPriceRanks, allStockInfoMap,"investmentTrustOverBuy");
		rankStockChangeService.writeStockListRankToRedis(investmentTrustOverSellStockDayPriceRanks, allStockInfoMap, "investmentTrustOverSell");
		rankStockChangeService.writeStockListRankToRedis(dealerOverBuyStockDayPriceRanks, allStockInfoMap, "dealerOverBuy");
		rankStockChangeService.writeStockListRankToRedis(dealerOverSellStockDayPriceRanks, allStockInfoMap,"dealerOverSell");
	}
}
