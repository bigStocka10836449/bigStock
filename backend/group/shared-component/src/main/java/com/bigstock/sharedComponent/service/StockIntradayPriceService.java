package com.bigstock.sharedComponent.service;

import java.io.IOException;
import java.io.Reader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bigstock.sharedComponent.entity.StockIntradayPrice;
import com.bigstock.sharedComponent.redis.CacheOperatorService;
import com.bigstock.sharedComponent.repository.StockIntradayPriceRepository;
import com.bigstock.sharedComponent.service.support.CacheAsideExecutor;
import com.bigstock.sharedComponent.service.support.CacheAsideService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockIntradayPriceService implements CacheAsideService {

	private final StockIntradayPriceRepository repository;

	private final DataSource dataSource;

	private final CacheAsideExecutor cacheAsideExecutor;

	private final CacheOperatorService cacheOperatorService;

	private static final String INTRADAY_CACHE_NAME = "ultraLongLivedCache";

	@Transactional(readOnly = true)
	public List<StockIntradayPrice> getTopByPeriodAndStockCodes(String period, List<String> stockCodes, int limit) {

		if (stockCodes == null || stockCodes.isEmpty()) {
			return Collections.emptyList();
		}

		return repository.findTopByPeriodAndStockCodes(period, stockCodes, limit);
	}

	@Transactional(readOnly = true)
	public List<StockIntradayPrice> getLatest60Prices(String stockCode, String period) {

		List<StockIntradayPrice> prices = repository.findTop60ByStockCodeAndPeriodOrderByTradingTimeDesc(stockCode,
				period);

		Collections.reverse(prices);

		return prices;
	}

	@Transactional(readOnly = true)
	public List<StockIntradayPrice> getPrices(String stockCode, String period) {

		return repository.findByStockCodeAndPeriodOrderByTradingTimeAsc(stockCode, period);
	}

	@Transactional(readOnly = true)
	public StockIntradayPrice getLatestPrice(String stockCode, String period) {

		return repository.findFirstByStockCodeAndPeriodOrderByTradingTimeDesc(stockCode, period).orElse(null);
	}

	@Transactional(readOnly = true)
	public StockIntradayPrice getOldestPrice(String stockCode, String period) {

		return repository.findFirstByStockCodeAndPeriodOrderByTradingTimeAsc(stockCode, period).orElse(null);
	}

	@Transactional
	public List<StockIntradayPrice> saveAll(List<StockIntradayPrice> prices) {

		if (prices == null || prices.isEmpty()) {
			return Collections.emptyList();
		}

		return repository.saveAll(prices);
	}

	@Transactional
	public int deleteBefore(String period, LocalDateTime cutoffTime) {

		return repository.deleteBefore(period, cutoffTime);
	}

	@Transactional
	public int cleanupExpiredData() {

		ZoneId zoneId = ZoneId.of("Asia/Taipei");

		LocalDateTime now = LocalDateTime.now(zoneId);

		int deleted = 0;

		deleted += repository.deleteBefore("5m", now.minusDays(60));

		deleted += repository.deleteBefore("60m", now.minusMonths(4));

		return deleted;
	}

	/**
	 * 5分K：Redis -> DB -> Redis
	 */
	public List<StockIntradayPrice> getFiveMinutePricesCache(String stockCode) {

		return getIntradayPricesCache(stockCode, "5m");
	}

	/**
	 * 60分K：Redis -> DB -> Redis
	 */
	public List<StockIntradayPrice> getSixtyMinutePricesCache(String stockCode) {

		return getIntradayPricesCache(stockCode, "60m");
	}

	/**
	 * 共用快取查詢流程
	 */

	private List<StockIntradayPrice> getIntradayPricesCache(String stockCode, String period) {

		if (stockCode == null || stockCode.isBlank()) {
			throw new IllegalArgumentException("stockCode must not be blank");
		}

		String cacheKey = "stock-intraday:" + period + ":" + stockCode;

		return getOrLoadWithLock(INTRADAY_CACHE_NAME, cacheKey,

				// 1. Redis Reader
				() -> cacheOperatorService.getSnapshotDataList(INTRADAY_CACHE_NAME, cacheKey, StockIntradayPrice.class),

				// 2. DB Loader
				() -> repository.findByStockCodeAndPeriodOrderByTradingTimeAsc(stockCode, period),

				// 3. Redis Writer
				data -> cacheOperatorService.putSnapshotDataListAtomic(INTRADAY_CACHE_NAME, cacheKey, data));
	}

	public void bulkUpsertIntradayPrices(List<StockIntradayPrice> data) throws Exception {

		if (data == null || data.isEmpty()) {
			return;
		}

		long startTime = System.nanoTime();

		try (Connection connection = dataSource.getConnection()) {

			connection.setAutoCommit(false);

			try {

				CopyManager copyManager = new CopyManager(connection.unwrap(BaseConnection.class));

				// =====================================================
				// 1. CREATE TEMP TABLE
				// =====================================================
				// 不複製 UNIQUE INDEX，避免 COPY 重複資料時直接失敗
				// =====================================================

				try (Statement stmt = connection.createStatement()) {

					stmt.execute("""

							    CREATE TEMP TABLE tmp_stock_intraday_price
							    (
							        LIKE bstock.stock_intraday_price
							        INCLUDING DEFAULTS
							    )
							    ON COMMIT DROP

							""");

				}

				// =====================================================
				// 2. STREAM DATA -> COPY
				// =====================================================

				long copyStart = System.nanoTime();

				long copiedRows;

				try (Reader reader = new Reader() {

					private final Iterator<StockIntradayPrice> iterator = data.iterator();

					private String currentLine;

					private int index = 0;

					@Override
					public int read(char[] cbuf, int off, int len) throws IOException {

						if (off < 0 || len < 0 || off > cbuf.length - len) {

							throw new IndexOutOfBoundsException();

						}

						if (len == 0) {
							return 0;
						}

						int count = 0;

						while (count < len) {

							if (currentLine == null || index >= currentLine.length()) {

								if (!iterator.hasNext()) {
									break;
								}

								try {

									currentLine = buildIntradayCsvLine(iterator.next()) + "\n";

								} catch (Exception e) {

									throw new IOException("Failed to build intraday CSV", e);

								}

								index = 0;

							}

							cbuf[off + count] = currentLine.charAt(index++);

							count++;

						}

						return count == 0 ? -1 : count;

					}

					@Override
					public void close() {
					}

				}) {

					copiedRows = copyManager.copyIn("""

							    COPY tmp_stock_intraday_price (

							        stock_code,
							        trading_time,
							        period,

							        opening_price,
							        closing_price,
							        high_price,
							        low_price,

							        trading_volume,

							        five_ma,
							        ten_ma,
							        twenty_ma,
							        sixty_ma,

							        line_rsv_value,
							        line_k_value,
							        line_d_value

							    )

							    FROM STDIN

							    WITH (
							        FORMAT csv,
							        NULL ''
							    )

							""", reader);

				}

				long copyElapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - copyStart);

				log.info("Intraday COPY completed, rows={}, elapsed={}ms", copiedRows, copyElapsed);

				// =====================================================
				// 3. REMOVE DUPLICATES
				// =====================================================
				//
				// 相同：
				// stock_code + trading_time + period
				//
				// 只保留一筆
				// =====================================================

				long dedupStart = System.nanoTime();

				int duplicateCount;

				try (Statement stmt = connection.createStatement()) {

					duplicateCount = stmt.executeUpdate("""

							    DELETE FROM tmp_stock_intraday_price t

							    USING (

							        SELECT

							            ctid,

							            ROW_NUMBER() OVER (

							                PARTITION BY
							                    stock_code,
							                    trading_time,
							                    period

							                ORDER BY ctid DESC

							            ) AS rn

							        FROM tmp_stock_intraday_price

							    ) duplicates

							    WHERE t.ctid = duplicates.ctid
							      AND duplicates.rn > 1

							""");

				}

				long dedupElapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - dedupStart);

				if (duplicateCount > 0) {

					log.warn("Intraday duplicates removed, count={}", duplicateCount);

				}

				// =====================================================
				// 4. FIND STOCKS WITH NEW RECORDS
				// =====================================================
				//
				// 只針對真正新增 K 線的股票進行歷史清理
				//
				// 使用 TEMP TABLE 紀錄
				// =====================================================

				long detectStart = System.nanoTime();

				try (Statement stmt = connection.createStatement()) {

					stmt.execute("""

							    CREATE TEMP TABLE tmp_intraday_cleanup_targets
							    ON COMMIT DROP
							    AS

							    SELECT DISTINCT

							        tmp.stock_code,
							        tmp.period

							    FROM tmp_stock_intraday_price tmp

							    WHERE tmp.period IN ('5m', '60m')

							      AND NOT EXISTS (

							          SELECT 1

							          FROM bstock.stock_intraday_price t

							          WHERE t.stock_code = tmp.stock_code
							            AND t.trading_time = tmp.trading_time
							            AND t.period = tmp.period

							      )

							""");

				}

				long detectElapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - detectStart);

				// =====================================================
				// 5. UPSERT ONLY DIFFERENT RECORDS
				// =====================================================

				long upsertStart = System.nanoTime();

				int affectedRows;

				try (Statement stmt = connection.createStatement()) {

					affectedRows = stmt.executeUpdate("""

							    INSERT INTO bstock.stock_intraday_price AS target (

							        stock_code,
							        trading_time,
							        period,

							        opening_price,
							        closing_price,
							        high_price,
							        low_price,

							        trading_volume,

							        five_ma,
							        ten_ma,
							        twenty_ma,
							        sixty_ma,

							        line_rsv_value,
							        line_k_value,
							        line_d_value

							    )

							    SELECT

							        stock_code,
							        trading_time,
							        period,

							        opening_price,
							        closing_price,
							        high_price,
							        low_price,

							        trading_volume,

							        five_ma,
							        ten_ma,
							        twenty_ma,
							        sixty_ma,

							        line_rsv_value,
							        line_k_value,
							        line_d_value

							    FROM tmp_stock_intraday_price

							    ORDER BY
							        stock_code,
							        period,
							        trading_time

							    ON CONFLICT (

							        stock_code,
							        trading_time,
							        period

							    )

							    DO UPDATE SET

							        opening_price = EXCLUDED.opening_price,
							        closing_price = EXCLUDED.closing_price,

							        high_price = EXCLUDED.high_price,
							        low_price = EXCLUDED.low_price,

							        trading_volume = EXCLUDED.trading_volume,

							        five_ma = EXCLUDED.five_ma,
							        ten_ma = EXCLUDED.ten_ma,
							        twenty_ma = EXCLUDED.twenty_ma,
							        sixty_ma = EXCLUDED.sixty_ma,

							        line_rsv_value = EXCLUDED.line_rsv_value,
							        line_k_value = EXCLUDED.line_k_value,
							        line_d_value = EXCLUDED.line_d_value

							    WHERE (

							        target.opening_price,
							        target.closing_price,
							        target.high_price,
							        target.low_price,

							        target.trading_volume,

							        target.five_ma,
							        target.ten_ma,
							        target.twenty_ma,
							        target.sixty_ma,

							        target.line_rsv_value,
							        target.line_k_value,
							        target.line_d_value

							    )

							    IS DISTINCT FROM (

							        EXCLUDED.opening_price,
							        EXCLUDED.closing_price,
							        EXCLUDED.high_price,
							        EXCLUDED.low_price,

							        EXCLUDED.trading_volume,

							        EXCLUDED.five_ma,
							        EXCLUDED.ten_ma,
							        EXCLUDED.twenty_ma,
							        EXCLUDED.sixty_ma,

							        EXCLUDED.line_rsv_value,
							        EXCLUDED.line_k_value,
							        EXCLUDED.line_d_value

							    )

							""");

				}

				long upsertElapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - upsertStart);

				log.info("Intraday UPSERT completed, affectedRows={}, elapsed={}ms", affectedRows, upsertElapsed);

				// =====================================================
				// 6. CLEANUP OLD INTRADAY PRICES
				// =====================================================
				//
				// 5m -> 2880 records
				// 60m -> 240 records
				//
				// Only process stocks with new records
				//
				// LATERAL + OFFSET
				// =====================================================

				long cleanupStart = System.nanoTime();

				int deletedCount;

				try (Statement stmt = connection.createStatement()) {

					deletedCount = stmt.executeUpdate("""

							    WITH expired AS (

							        SELECT old_prices.ctid AS row_id

							        FROM tmp_intraday_cleanup_targets a

							        CROSS JOIN LATERAL (

							            SELECT t.ctid

							            FROM bstock.stock_intraday_price t

							            WHERE t.stock_code = a.stock_code
							              AND t.period = a.period

							            ORDER BY t.trading_time DESC

							            OFFSET (

							                CASE

							                    WHEN a.period = '5m'
							                        THEN 2880

							                    WHEN a.period = '60m'
							                        THEN 240

							                END

							            )

							        ) old_prices

							    )

							    DELETE FROM bstock.stock_intraday_price t

							    USING expired e

							    WHERE t.ctid = e.row_id

							""");

				}

				long cleanupElapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - cleanupStart);

				if (deletedCount > 0) {

					log.info("Intraday cleanup completed, deletedRecords={}, elapsed={}ms", deletedCount,
							cleanupElapsed);

				}

				// =====================================================
				// 7. COMMIT
				// =====================================================

				connection.commit();

				long totalElapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);

				// =====================================================
				// 8. PERFORMANCE LOG
				// =====================================================

				log.info("""

						==========================================
						Intraday Bulk UPSERT Performance
						==========================================
						Input Records   : {}
						Copied Records  : {}
						Duplicates      : {}
						Affected Rows   : {}
						Deleted Records : {}
						------------------------------------------
						COPY Time       : {} ms
						Dedup Time      : {} ms
						Detect Time     : {} ms
						UPSERT Time     : {} ms
						Cleanup Time    : {} ms
						------------------------------------------
						Total Time      : {} ms
						==========================================
						""", data.size(), copiedRows, duplicateCount, affectedRows, deletedCount, copyElapsed,
						dedupElapsed, detectElapsed, upsertElapsed, cleanupElapsed, totalElapsed);

			} catch (Exception e) {

				try {

					connection.rollback();

				} catch (SQLException rollbackException) {

					e.addSuppressed(rollbackException);

				}

				log.error("Intraday bulk UPSERT failed, records={}", data.size(), e);

				throw e;

			}

		}

	}

	private String buildIntradayCsvLine(StockIntradayPrice data) {

		StringBuilder sb = new StringBuilder(256);

		appendCsv(sb, data.getStockCode());

		sb.append(',');

		appendCsv(sb, data.getTradingTime() == null ? null : data.getTradingTime().toString());

		sb.append(',');

		appendCsv(sb, data.getPeriod());

		sb.append(',');

		appendCsv(sb, data.getOpeningPrice());

		sb.append(',');

		appendCsv(sb, data.getClosingPrice());

		sb.append(',');

		appendCsv(sb, data.getHighPrice());

		sb.append(',');

		appendCsv(sb, data.getLowPrice());

		sb.append(',');

		appendCsv(sb, data.getTradingVolume());

		sb.append(',');

		appendCsv(sb, data.getFiveMa());

		sb.append(',');

		appendCsv(sb, data.getTenMa());

		sb.append(',');

		appendCsv(sb, data.getTwentyMa());

		sb.append(',');

		appendCsv(sb, data.getSixtyMa());

		sb.append(',');

		appendCsv(sb, data.getLineRsvValue());

		sb.append(',');

		appendCsv(sb, data.getLineKValue());

		sb.append(',');

		appendCsv(sb, data.getLineDValue());

		return sb.toString();
	}

	private void appendCsv(StringBuilder sb, Object value) {

		if (value == null) {

			// PostgreSQL COPY NULL ''
			sb.append("");

			return;
		}

		String text = String.valueOf(value);

		boolean needQuote = text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r");

		if (!needQuote) {

			sb.append(text);

			return;
		}

		sb.append('"');

		sb.append(text.replace("\"", "\"\""));

		sb.append('"');
	}

	@Override
	public CacheAsideExecutor getCacheAsideExecutor() {
	    return cacheAsideExecutor;
	}
}