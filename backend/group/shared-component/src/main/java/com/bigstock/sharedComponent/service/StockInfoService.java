package com.bigstock.sharedComponent.service;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import com.bigstock.sharedComponent.entity.MarginTradingAndShortSellingInfo;
import com.bigstock.sharedComponent.entity.StockInfo;
import com.bigstock.sharedComponent.redis.CacheOperatorService;
import com.bigstock.sharedComponent.repository.StockInfoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StockInfoService {
	private final StockInfoRepository stockInfoRepository;
	
	private final DataSource dataSource;
	
	private final CacheOperatorService cacheOperatorService;
	
	private final RedissonClient redissonClient;
	
	public List<StockInfo> getAllStockInfo() {
		List<StockInfo> stockInfos = cacheOperatorService.getSnapshotDataList("ultraLongLivedCache",
				"stockInfo:compressed", StockInfo.class);
		if (stockInfos.isEmpty()) {
			return stockInfos;
		} else {
			String lockKey = "lock:getAllStockInfo:cacheName:ultraLongLivedCache:stockInfo:compressed";

			RLock lock = redissonClient.getLock(lockKey);
			boolean lockAcquired = false;
			try {

				lockAcquired = lock.tryLock(10, TimeUnit.MINUTES);

				if (lockAcquired) {
					stockInfos = cacheOperatorService.getSnapshotDataList("ultraLongLivedCache", "stockInfo:compressed",
							StockInfo.class);

					if (!stockInfos.isEmpty()) {
						return stockInfos;
					}
					List<StockInfo> nonCacheSstockInfos = stockInfoRepository.getAllStockInfo();
					cacheOperatorService.putSnapshotDataListAtomic("ultraLongLivedCache", "stockInfo:compressed",
							nonCacheSstockInfos);
					return nonCacheSstockInfos;
				} else {
					throw new RuntimeException("Could not acquire lock for getAllStockInfo");
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
	
	public List<StockInfo> insertAll(List<StockInfo> stockInfos){
		return stockInfoRepository.saveAll(stockInfos);
	}
	
	public List<StockInfo> findByStockType(String stockType){
		return stockInfoRepository.findByStockType(stockType);
	}
	
	public Optional<StockInfo> findById(String stockCode){
		return stockInfoRepository.findById(stockCode);
	}
	
	public List<StockInfo> findByIds(List<String> ids){
		return stockInfoRepository.findAllById(ids);
	}
	
	
	public List<String> getStockCodeByStockType(String stockType) {
		return stockInfoRepository.getStockCodeByStockType(stockType);
	}
	
	public void refreshStockInfoAtomic(List<StockInfo> data) throws Exception {

	    if (data == null || data.isEmpty()) return;

	    try (Connection conn = dataSource.getConnection()) {

	        conn.setAutoCommit(false);

	        // advisory lock (prevent concurrent refresh)
	        try (Statement stmt = conn.createStatement()) {
	            stmt.execute("SELECT pg_advisory_xact_lock(889977)");
	        }

	        // create temp table
	        try (Statement stmt = conn.createStatement()) {
	            stmt.execute("""
	                    CREATE TEMP TABLE tmp_stock_info
	                    (LIKE bstock.stock_info INCLUDING ALL)
	                    ON COMMIT DROP
	                    """);
	        }

	        CopyManager copyManager =
	                new CopyManager(conn.unwrap(BaseConnection.class));

	        String copySql = """
	                COPY tmp_stock_info
	                (stock_code, stock_name, stock_type)
	                FROM STDIN WITH (FORMAT csv)
	                """;

	        // ⭐ PIPE STREAM
	        PipedOutputStream pos = new PipedOutputStream();
	        PipedInputStream pis = new PipedInputStream(pos, 1024 * 1024);

	        Thread writerThread = new Thread(() -> {
	            try (BufferedWriter writer =
	                         new BufferedWriter(new OutputStreamWriter(pos, StandardCharsets.UTF_8))) {

	                for (StockInfo s : data) {

	                    writer.write(escapeCsv(s.getStockCode()));
	                    writer.write(",");
	                    writer.write(escapeCsv(s.getStockName()));
	                    writer.write(",");
	                    writer.write(escapeCsv(s.getStockType()));
	                    writer.write("\n");
	                }

	            } catch (Exception e) {
	                throw new RuntimeException(e);
	            }
	        });

	        writerThread.start();

	        // ⭐ COPY START (reading from pipe)
	        copyManager.copyIn(copySql, pis);

	        writerThread.join();

	        // atomic swap
	        try (Statement stmt = conn.createStatement()) {

	            stmt.execute("LOCK TABLE bstock.stock_info IN ACCESS EXCLUSIVE MODE");

	            stmt.execute("TRUNCATE bstock.stock_info");

	            stmt.execute("""
	                    INSERT INTO bstock.stock_info
	                    SELECT * FROM tmp_stock_info
	                    """);
	        }

	        conn.commit();
	    }
	}
	
	private String escapeCsv(String value) {

	    if (value == null) return "";

	    String v = value.replace("\"", "\"\"");

	    if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
	        return "\"" + v + "\"";
	    }

	    return v;
	}
}
