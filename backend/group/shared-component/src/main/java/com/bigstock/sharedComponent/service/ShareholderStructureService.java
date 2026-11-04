package com.bigstock.sharedComponent.service;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.sql.Connection;
import java.sql.Statement;
import java.time.Duration;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.apache.commons.collections4.CollectionUtils;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.bigstock.sharedComponent.entity.MarginTradingAndShortSellingInfo;
import com.bigstock.sharedComponent.entity.ShareholderStructure;
import com.bigstock.sharedComponent.entity.StockDayPrice;
import com.bigstock.sharedComponent.redis.CacheOperatorService;
import com.bigstock.sharedComponent.repository.ShareholderStructureRepository;
import com.bigstock.sharedComponent.repository.StockDayPriceRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShareholderStructureService {
	private static final String LATEST_QUERY_CACHE = "shareholderQueryCache";
	private static final Duration LATEST_QUERY_TTL = Duration.ofMinutes(1);
	private final ShareholderStructureRepository shareholderStructureRepository;

	
	private final ApplicationContext ctx;
	
	private final DataSource dataSource;
	
	private final CacheOperatorService cacheOperatorService;
	
	private final RedissonClient redissonClient;

	public List<ShareholderStructure> getAll() {
		return shareholderStructureRepository.findAll();
	}

	public Optional<ShareholderStructure> getById(String id) {
		return getSelf().getByIdWithDataBase(id);
	}

	public ShareholderStructure insert(ShareholderStructure shareholderStructure) {
		return shareholderStructureRepository.save(shareholderStructure);
	}

	public List<ShareholderStructure> insert(List<ShareholderStructure> shareholderStructures) {
		return shareholderStructureRepository.saveAll(shareholderStructures);
	}

	public void delete(String id) {
		shareholderStructureRepository.deleteById(id);
	}

	public void delete(ShareholderStructure shareholderStructure) {
		shareholderStructureRepository.delete(shareholderStructure);
	}

	public List<String> getAllShareholderStructureStockCode() {
		return shareholderStructureRepository.getAllShareholderStructureStockCode();
	}

	public List<ShareholderStructure> getShareholderStructureLastTwoWeeks(String firstWeekOfYear,
			String secondWeekOfYear, String thirdWeekOfYear) {

		return getSelf().getShareholderStructureLastTwoWeeksWithDataBase(firstWeekOfYear, secondWeekOfYear,
				thirdWeekOfYear);
	}

//	@Cacheable(value = "longLivedCache", key = "#stockCode")
	public List<ShareholderStructure> getShareholderStructureByStockCodeDesc(String stockCode) {
		List<ShareholderStructure> shareholderStructures = cacheOperatorService.getCompressedZSetAllScore(
				"ultraLongLivedCache", "shareholderStructure:compressed:" + stockCode, ShareholderStructure.class);
		if (CollectionUtils.isNotEmpty(shareholderStructures)) {
			return shareholderStructures;

		} else {
			String lockKey = "lock:getShareholderStructureByStockCodeDesc:cacheName:ultraLongLivedCache:shareholderStructure:compressed:"
					+ stockCode;

			RLock lock = redissonClient.getLock(lockKey);
			boolean lockAcquired = false;
			try {

				lockAcquired = lock.tryLock(10, TimeUnit.MINUTES);

				if (lockAcquired) {
					List<ShareholderStructure> nonCacheShareholderStructures = getSelf()
							.getShareholderStructureByStockCodeDescWithDataBase(stockCode);
					cacheOperatorService.batchUpsertCompressedZSetSeries("ultraLongLivedCache",
							"shareholderStructure:compressed:" + stockCode, nonCacheShareholderStructures,
							shareholderStructure -> ShareholderStructureService
									.weekOfYearToScore(shareholderStructure.getWeekOfYear()),
							CacheOperatorService.DEFAULT_SERIES_MAX_SIZE);
					return nonCacheShareholderStructures;
				} else {
					throw new RuntimeException("Could not acquire lock for " + lockKey);
				}

			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new RuntimeException("Interrupted while trying to acquire lock", e);
			} finally {

				if (lockAcquired && lock.isHeldByCurrentThread()) {
					lock.unlock();
				}

			}

				}
		}
	
		/** Read the API's bounded history without truncating the shared full-history cache. */
		public List<ShareholderStructure> getLatestShareholderStructureByStockCode(String stockCode, int limit) {
			if (stockCode == null || stockCode.isBlank() || limit <= 0) return List.of();
			String code = stockCode.trim();
			int safeLimit = Math.min(limit, CacheOperatorService.DEFAULT_SERIES_MAX_SIZE);
			String queryKey = code + ":" + safeLimit;
			List<ShareholderStructure> cached = readLatestShareholderCache(code, queryKey, safeLimit);
			if (cached != null) return cached;
	
			RLock lock = null;
			boolean acquired = false;
			try {
				lock = redissonClient.getLock("lock:shareholder-query:" + queryKey);
				acquired = lock.tryLock(2, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException("Interrupted while loading shareholder history", e);
			} catch (RuntimeException e) {
				log.warn("Shareholder query lock unavailable, stockCode={}", code, e);
			}

			try {
				// Recheck even after a lock timeout; a previous reader may just have finished.
				cached = readLatestShareholderCache(code, queryKey, safeLimit);
				if (cached != null) return cached;
				List<ShareholderStructure> rows = shareholderStructureRepository
						.getShareholderStructureByStockCodeDesc(code, PageRequest.of(0, safeLimit));
				try {
				// A separate, short-lived result cache also caches an empty DB result.
					cacheOperatorService.putObject(LATEST_QUERY_CACHE, queryKey, rows, LATEST_QUERY_TTL);
				} catch (Exception e) {
					log.warn("Shareholder query cache write failed, stockCode={}", code, e);
				}
				return rows;
			} finally {
				if (acquired) {
					try {
						if (lock.isHeldByCurrentThread()) lock.unlock();
					} catch (RuntimeException e) {
						log.warn("Shareholder query lock release failed, stockCode={}", code, e);
					}
				}
			}
		}
	
		private List<ShareholderStructure> readLatestShareholderCache(String code, String queryKey, int limit) {
			try {
				List<ShareholderStructure> rows = cacheOperatorService.getLatestCompressedZSetSeries(
						"ultraLongLivedCache", "shareholderStructure:compressed:" + code,
						ShareholderStructure.class, limit);
			if (CollectionUtils.isNotEmpty(rows)) return rows;
				ShareholderStructure[] snapshot = cacheOperatorService.getObject(
						LATEST_QUERY_CACHE, queryKey, ShareholderStructure[].class);
				return snapshot == null ? null : Arrays.asList(snapshot);
			} catch (Exception e) {
				log.warn("Shareholder query cache read failed, stockCode={}", code, e);
				return null;
		}
	}

	public boolean checkWeekExist(String weekOfYear) {
		return shareholderStructureRepository.countByWeekOfYear(weekOfYear) > 0;
	}

//	@BigStockCacheableWithLock(value = "longLivedCache", key = "#id")
	public Optional<ShareholderStructure> getByIdWithDataBase(String id) {
		return shareholderStructureRepository.findById(id);
	}

//	@BigStockCacheableWithLock(value = "longLivedCache", key = "#p0 + '-' + #p1 + '-' + #p2")
	public List<ShareholderStructure> getShareholderStructureLastTwoWeeksWithDataBase(String firstWeekOfYear,
			String secondWeekOfYear, String thirdWeekOfYear) {
		return shareholderStructureRepository.getByOverFourHundreLotContinueIncrease(firstWeekOfYear, secondWeekOfYear,
				thirdWeekOfYear);
	}

//	@BigStockCacheableWithLock(value = "longLivedCache", key = "#id")
	public List<ShareholderStructure> getShareholderStructureByStockCodeDescWithDataBase(String stockCode) {
		return shareholderStructureRepository.getShareholderStructureByStockCodeDesc(stockCode);
	}

	@Transactional
	public void bulkUpsertShareholderStructure(List<ShareholderStructure> data)
	        throws Exception {

	    if (data == null || data.isEmpty()) {
	        return;
	    }

	    try (Connection connection = dataSource.getConnection()) {

	        connection.setAutoCommit(false);

	        CopyManager copyManager =
	                new CopyManager(connection.unwrap(BaseConnection.class));

	        // 1️⃣ TEMP TABLE
	        try (Statement stmt = connection.createStatement()) {
	            stmt.execute("""
	                CREATE TEMP TABLE tmp_shareholder_structure
	                (LIKE bstock.shareholder_structure INCLUDING ALL)
	                ON COMMIT DROP
	            """);
	        }

	        // 2️⃣ COPY
	        String copySql = """
	            COPY tmp_shareholder_structure (
	                id,
	                stock_code,
	                stock_name,
	                week_of_year,
	                count_date,
	                opening_price,
	                closing_price,
	                price_change,
	                price_change_percent,
	                tdcc_stock,
	                less_1_board_lot,
	                between_1_and_5_board_lot,
	                between_5_and_10_board_lot,
	                between_10_and_15_board_lot,
	                between_15_and_20_board_lot,
	                between_20_and_30_board_lot,
	                between_30_and_40_board_lot,
	                between_40_and_50_board_lot,
	                between_50_and_100_board_lot,
	                between_100_and_200_board_lot,
	                between_200_and_400_board_lot,
	                between_400_and_600_board_lot,
	                between_600_and_800_board_lot,
	                between_800_and_1000_board_lot,
	                over_1000_board_lot,
	                stock_total,
	                less_1_board_lot_people,
	                between_1_and_5_board_lot_people,
	                between_5_and_10_board_lot_people,
	                between_10_and_15_board_lot_people,
	                between_15_and_20_board_lot_people,
	                between_20_and_30_board_lot_people,
	                between_30_and_40_board_lot_people,
	                between_40_and_50_board_lot_people,
	                between_50_and_100_board_lot_people,
	                between_100_and_200_board_lot_people,
	                between_200_and_400_board_lot_people,
	                between_400_and_600_board_lot_people,
	                between_600_and_800_board_lot_people,
	                between_800_and_1000_board_lot_people,
	                over_1000_board_lot_people,
	                total_people
	            )
	            FROM STDIN WITH (FORMAT csv)
	        """;

	        PipedOutputStream pos = new PipedOutputStream();
	        PipedInputStream pis = new PipedInputStream(pos, 65536);

	        ExecutorService executor = Executors.newSingleThreadExecutor();

	        executor.submit(() -> {
	            try (BufferedWriter writer =
	                         new BufferedWriter(new OutputStreamWriter(pos))) {

	                for (ShareholderStructure s : data) {
	                    writer.write(buildCsvLine(s));
	                    writer.newLine();
	                }

	            }
	            return null;
	        });

	        copyManager.copyIn(copySql, pis);
	        executor.shutdown();

	        // ⭐ 3️⃣ UPSERT
	        try (Statement stmt = connection.createStatement()) {
	        	stmt.execute("""
	        		    INSERT INTO bstock.shareholder_structure AS t (
	        		        id,
	        		        stock_code,
	        		        stock_name,
	        		        week_of_year,
	        		        count_date,
	        		        opening_price,
	        		        closing_price,
	        		        price_change,
	        		        price_change_percent,
	        		        tdcc_stock,
	        		        less_1_board_lot,
	        		        between_1_and_5_board_lot,
	        		        between_5_and_10_board_lot,
	        		        between_10_and_15_board_lot,
	        		        between_15_and_20_board_lot,
	        		        between_20_and_30_board_lot,
	        		        between_30_and_40_board_lot,
	        		        between_40_and_50_board_lot,
	        		        between_50_and_100_board_lot,
	        		        between_100_and_200_board_lot,
	        		        between_200_and_400_board_lot,
	        		        between_400_and_600_board_lot,
	        		        between_600_and_800_board_lot,
	        		        between_800_and_1000_board_lot,
	        		        over_1000_board_lot,
	        		        stock_total,
	        		        less_1_board_lot_people,
	        		        between_1_and_5_board_lot_people,
	        		        between_5_and_10_board_lot_people,
	        		        between_10_and_15_board_lot_people,
	        		        between_15_and_20_board_lot_people,
	        		        between_20_and_30_board_lot_people,
	        		        between_30_and_40_board_lot_people,
	        		        between_40_and_50_board_lot_people,
	        		        between_50_and_100_board_lot_people,
	        		        between_100_and_200_board_lot_people,
	        		        between_200_and_400_board_lot_people,
	        		        between_400_and_600_board_lot_people,
	        		        between_600_and_800_board_lot_people,
	        		        between_800_and_1000_board_lot_people,
	        		        over_1000_board_lot_people,
	        		        total_people
	        		    )
	        		    SELECT
	        		        id,
	        		        stock_code,
	        		        stock_name,
	        		        week_of_year,
	        		        count_date,
	        		        opening_price,
	        		        closing_price,
	        		        price_change,
	        		        price_change_percent,
	        		        tdcc_stock,
	        		        less_1_board_lot,
	        		        between_1_and_5_board_lot,
	        		        between_5_and_10_board_lot,
	        		        between_10_and_15_board_lot,
	        		        between_15_and_20_board_lot,
	        		        between_20_and_30_board_lot,
	        		        between_30_and_40_board_lot,
	        		        between_40_and_50_board_lot,
	        		        between_50_and_100_board_lot,
	        		        between_100_and_200_board_lot,
	        		        between_200_and_400_board_lot,
	        		        between_400_and_600_board_lot,
	        		        between_600_and_800_board_lot,
	        		        between_800_and_1000_board_lot,
	        		        over_1000_board_lot,
	        		        stock_total,
	        		        less_1_board_lot_people,
	        		        between_1_and_5_board_lot_people,
	        		        between_5_and_10_board_lot_people,
	        		        between_10_and_15_board_lot_people,
	        		        between_15_and_20_board_lot_people,
	        		        between_20_and_30_board_lot_people,
	        		        between_30_and_40_board_lot_people,
	        		        between_40_and_50_board_lot_people,
	        		        between_50_and_100_board_lot_people,
	        		        between_100_and_200_board_lot_people,
	        		        between_200_and_400_board_lot_people,
	        		        between_400_and_600_board_lot_people,
	        		        between_600_and_800_board_lot_people,
	        		        between_800_and_1000_board_lot_people,
	        		        over_1000_board_lot_people,
	        		        total_people
	        		    FROM (
	        		        SELECT *,
	        		               ROW_NUMBER() OVER (
	        		                   PARTITION BY id
	        		                   ORDER BY count_date DESC
	        		               ) as rn
	        		        FROM tmp_shareholder_structure
	        		    ) s
	        		    WHERE rn = 1
	        		    ON CONFLICT (id)
	        		    DO UPDATE SET
	        		        closing_price = EXCLUDED.closing_price,
	        		        price_change = EXCLUDED.price_change,
	        		        price_change_percent = EXCLUDED.price_change_percent,
	        		        total_people = EXCLUDED.total_people
	        		""");
	        }

	        connection.commit();
	    }
	}
	
	private String buildCsvLine(ShareholderStructure s) {
	    return String.join(",",
	            safe(s.getId()),
	            safe(s.getStockCode()),
	            safe(s.getStockName()),
	            safe(s.getWeekOfYear()),
	            safe(s.getCountDate()),
	            safe(s.getOpeningPrice()),
	            safe(s.getClosingPrice()),
	            safe(s.getPriceChange()),
	            safe(s.getPriceChangePercent()),
	            safe(s.getTdccStock()),
	            safe(s.getLessThanOneBoardLot()),
	            safe(s.getBetweenOneAndFiveBoardLot()),
	            safe(s.getBetweenFiveAndTenBoardLot()),
	            safe(s.getBetweenTenAndFifteenBoardLot()),
	            safe(s.getBetweenFifteenAndTwentyBoardLot()),
	            safe(s.getBetweenTwentyAndThirtyBoardLot()),
	            safe(s.getBetweenThirtyAndFortyBoardLot()),
	            safe(s.getBetweenFortyAndFiftyBoardLot()),
	            safe(s.getBetweenFiftyAndOneHundredBoardLot()),
	            safe(s.getBetweenOneHundredAndTwoHundredBoardLot()),
	            safe(s.getBetweenTwoHundredAndFourHundredBoardLot()),
	            safe(s.getBetweenFourHundredAndSixHundredBoardLot()),
	            safe(s.getBetweenSixHundredAndEightHundredBoardLot()),
	            safe(s.getBetweenEightHundredAndOneThousandBoardLot()),
	            safe(s.getOverOneThousandBoardLot()),
	            safe(s.getStockTotal()),
	            safe(s.getLessThanOneBoardLotPeople()),
	            safe(s.getBetweenOneAndFiveBoardLotPeople()),
	            safe(s.getBetweenFiveAndTenBoardLotPeople()),
	            safe(s.getBetweenTenAndFifteenBoardLotPeople()),
	            safe(s.getBetweenFifteenAndTwentyBoardLotPeople()),
	            safe(s.getBetweenTwentyAndThirtyBoardLotPeople()),
	            safe(s.getBetweenThirtyAndFortyBoardLotPeople()),
	            safe(s.getBetweenFortyAndFiftyBoardLotPeople()),
	            safe(s.getBetweenFiftyAndOneHundredBoardLotPeople()),
	            safe(s.getBetweenOneHundredAndTwoHundredBoardLotPeople()),
	            safe(s.getBetweenTwoHundredAndFourHundredBoardLotPeople()),
	            safe(s.getBetweenFourHundredAndSixHundredBoardLotPeople()),
	            safe(s.getBetweenSixHundredAndEightHundredBoardLotPeople()),
	            safe(s.getBetweenEightHundredAndOneThousandBoardLotPeople()),
	            safe(s.getOverOneThousandBoardLotPeople()),
	            safe(s.getTotalPeople())
	    );
	}

	private String safe(String v) {
	    return v == null ? "" : v.replace(",", " ");
	}
	
	private ShareholderStructureService getSelf() {
		return ctx.getBean(ShareholderStructureService.class);
	}
	
	public static double weekOfYearToScore(String weekText) {

	    // 2026W9 -> 2026 , 9
	    int year = Integer.parseInt(weekText.substring(0, 4));
	    int week = Integer.parseInt(weekText.substring(5));

	    return year * 100 + week;
	}
}
