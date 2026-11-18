package com.bigstock.schedule.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.bigstock.schedule.job.core.JobContext;
import com.bigstock.sharedComponent.dto.StockTrendCache;
import com.bigstock.sharedComponent.entity.StockDayPrice;
import com.bigstock.sharedComponent.redis.StockTrendRedisService;
import com.bigstock.sharedComponent.service.StockDayPriceService;
import com.bigstock.sharedComponent.service.StockInfoService;
import com.google.common.collect.Lists;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CalculateCosineSimilarityVectorService {

	private static final RestTemplate restTemplate = new RestTemplate();
	private final StockDayPriceService stockDayPriceService;
	private final StockTrendRedisService stockTrendRedisService;
	private final StockInfoService stockInfoService;

	@Value("${schedule.task.scheduling.similiar-search-reload}")
	private String similiarSearchReloadUrl;

	private static final int VECTOR_CHUNK_SIZE = 100;

	/**
	 * 舊入口：維持非同步與原本的例外記錄行為。 不可讓需要等待向量任務完成的 StockDailyJob 使用此方法。
	 */
	@Async("stockVectorExecutor")
	public void calculateAsDailyAspect() {
		try {
			calculateAsDailyAspectManaged(null);
		} catch (Exception e) {
			log.error("Stock vector calculation failed", e);
		}
	}

	/**
	 * Job Management 入口：同步執行、完成後才返回、失敗往外拋。 不加 @Async，讓日 K 主 Job 能正確等待此階段完成。
	 */
	public void calculateAsDailyAspectManaged(JobContext context) throws Exception {

		List<String> tpexStockCodes = stockInfoService.getStockCodeByStockType("0").stream()
				.filter(code -> !code.matches(".*[a-zA-Z].*")).toList();

		List<String> twseStockCodes = stockInfoService.getStockCodeByStockType("1").stream()
				.filter(code -> !code.matches(".*[a-zA-Z].*")).toList();

		List<String> allStockCodes = new ArrayList<>();
		allStockCodes.addAll(tpexStockCodes);
		allStockCodes.addAll(twseStockCodes);

		final int totalStocks = allStockCodes.size();

		log.info("Start stock vector calculation, total={}", totalStocks);

		if (context != null) {
			context.checkCancellation();
			// 在向量階段開始時加入真實向量股票數 + Python Cache reload 1 單位。
			// 不重複計入日 K 原有的股票處理數量。
			context.addTotal(totalStocks + 1L);
			context.stage("VECTOR_CALCULATING");
		}

		for (int i = 0; i < totalStocks; i += VECTOR_CHUNK_SIZE) {
			if (context != null) {
				context.checkCancellation();
			}

			int end = Math.min(i + VECTOR_CHUNK_SIZE, totalStocks);
			List<String> chunk = allStockCodes.subList(i, end);

			// 完全保留原有 Stream、calculateOneStock 與 null 過濾行為。
			List<StockTrendCache> stockTrendCaches = chunk.stream().map(this::calculateOneStock)
					.filter(Objects::nonNull).toList();

			// 保留原有每 100 檔結果寫入 Redis 的位置。
			if (!stockTrendCaches.isEmpty()) {
				stockTrendRedisService.saveBatch(stockTrendCaches);
			}

			if (context != null) {
				int processed = chunk.size();
				int succeeded = stockTrendCaches.size();
				int skipped = processed - succeeded;
				// 只在本批 Redis saveBatch 正常返回後才認列完成。
				context.batchResult(processed, succeeded, 0, skipped, "VECTOR_CHUNK_DONE " + end + "/" + totalStocks);
			}

			log.info("Stock vector chunk completed, progress={}/{}, saved={}", end, totalStocks,
					stockTrendCaches.size());
		}

		log.info("Finished stock vector calculation");

		if (context != null) {
			context.checkCancellation();
			context.stage("VECTOR_PYTHON_CACHE_RELOAD");
		}

		// 保留原有最終 Python Cache reload 呼叫位置。
		// 注意：此方法必須同步等待 Python 端完成並在錯誤時拋例外，
		// 才能把本行返回當成『Reload 成功』。
		reloadPythonCache();

		if (context != null) {
			context.batchResult(1, 1, 0, 0, "VECTOR_PYTHON_CACHE_RELOAD_DONE");
			context.stage("VECTOR_COMPLETED");
		}

		log.info("Stock vector calculation and Python cache reload completed");
	}

	private StockTrendCache calculateOneStock(String stockCode) {

		try {

			log.debug("Calculate stock vector, stockCode={}", stockCode);

			List<StockDayPrice> prices = stockDayPriceService.findLastest600StockDayPriceByStockCodeCache(stockCode);

			if (CollectionUtils.isEmpty(prices)) {

				log.warn("Skip stock vector calculation, no prices, stockCode={}", stockCode);

				return null;
			}

			/*
			 * Deduplicate by trading date.
			 *
			 * If multiple records exist on the same trading date, keep the record with the
			 * latest tradingDay time.
			 *
			 * Example:
			 *
			 * 2026-09-30 00:00:00 2026-09-30 14:59:43 <- keep this one
			 */
			Map<LocalDate, StockDayPrice> latestPriceByDate = prices.stream().collect(Collectors.toMap(

					// Key: trading date
					item -> item.getTradingDay().toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),

					// Value: StockDayPrice itself
					Function.identity(),

					// Duplicate trading date
					(existing, incoming) -> {

						StockDayPrice keep;
						StockDayPrice discard;

						if (incoming.getTradingDay().after(existing.getTradingDay())) {

							keep = incoming;
							discard = existing;

						} else {

							keep = existing;
							discard = incoming;
						}

						log.warn(
								"Duplicate trading date found, " + "stockCode={}, " + "tradingDate={}, "
										+ "keepTradingDay={}, " + "discardTradingDay={}, " + "keepClose={}, "
										+ "discardClose={}",
								stockCode,
								keep.getTradingDay().toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
								keep.getTradingDay(), discard.getTradingDay(), keep.getClosingPrice(),
								discard.getClosingPrice());

						return keep;
					}));

			/*
			 * Convert back to List and restore chronological order.
			 *
			 * Do not rely on Map iteration order because calculationVectors() works with
			 * time-series data.
			 */
			List<StockDayPrice> deduplicatedPrices = Lists.newArrayList(latestPriceByDate.values().stream()
					.sorted(Comparator.comparing(StockDayPrice::getTradingDay)).toList());

			if (prices.size() != deduplicatedPrices.size()) {

				log.warn(
						"Stock prices deduplicated, " + "stockCode={}, " + "originalSize={}, " + "deduplicatedSize={}, "
								+ "removed={}",
						stockCode, prices.size(), deduplicatedPrices.size(), prices.size() - deduplicatedPrices.size());
			}

			return stockDayPriceService.calculationVectors(deduplicatedPrices);

		} catch (Exception e) {

			log.error("Failed to calculate stock vector, stockCode={}", stockCode, e);

			return null;
		}
	}

	private void reloadPythonCache() {

		try {

			log.info("Calling Python cache reload");

			ResponseEntity<String> response = restTemplate.postForEntity(similiarSearchReloadUrl, null, String.class);

			log.info("Python cache reload completed, status={}, response={}", response.getStatusCode(),
					response.getBody());

		} catch (Exception e) {

			log.error("Failed to reload Python cache", e);
		}
	}
}