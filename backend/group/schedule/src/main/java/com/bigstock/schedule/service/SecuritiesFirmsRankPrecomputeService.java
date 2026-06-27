package com.bigstock.schedule.service;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.bigstock.sharedComponent.dto.SecuritiesFirmsRankResult;
import com.bigstock.sharedComponent.service.SecuritiesFirmsDayOperateRankService;
import com.bigstock.sharedComponent.service.SecuritiesFirmsDayOperateService;
import com.bigstock.sharedComponent.service.SecuritiesFirmsRankRedisService;
import com.bigstock.sharedComponent.service.StockInfoService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SecuritiesFirmsRankPrecomputeService {

    private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT = ThreadLocal.withInitial(
            () -> new SimpleDateFormat("yyyy-MM-dd")
    );

    private static final DateTimeFormatter RUN_ID_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private static final List<Integer> RANGE_DAYS_LIST = Arrays.asList(1, 3, 5, 10, 20, 60, 120);

    /**
     * Java range 計算版會把 120 天每日券商彙總資料放到 Java 暫存計算。
     * 先用 50 降低單批記憶體與 GC 壓力。
     */
    private static final int STOCK_BATCH_SIZE = 50;

    /**
     * 併發 worker 數量。
     * 先固定 2 條，避免一次把 DB 壓力放太大。
     */
    private static final int PRECOMPUTE_WORKER_COUNT = 2;

    /**
     * 每批查詢後稍微暫停，避免連續打 DB。
     * 若要測純速度，可以先改成 0。
     * 若正式環境想保守，可以維持 20。
     */
    private static final long BATCH_SLEEP_MILLIS = 20L;

    private final SecuritiesFirmsDayOperateService securitiesFirmsDayOperateService;

    private final SecuritiesFirmsDayOperateRankService rankService;

    private final SecuritiesFirmsRankRedisService rankRedisService;

    private final StockInfoService stockInfoService;

    private final RedissonClient redissonClient;

    @Async("securitiesFirmsRankTaskExecutor")
    public void precomputeAsync(String triggerSource) {
        precompute(triggerSource);
    }

    public void precompute(String triggerSource) {
        java.util.Date latestAvailableTradingDate = null;
        String endDateText = null;
        String runId = null;
        RLock lock = null;
        boolean locked = false;
        long precomputeStartMillis = System.currentTimeMillis();

        try {
            latestAvailableTradingDate = securitiesFirmsDayOperateService
                    .findLatestAvailableTradingDate("2330");

            if (latestAvailableTradingDate == null) {
                log.warn("No securities firms day operate data available. triggerSource={}", triggerSource);
                return;
            }

            endDateText = formatDate(latestAvailableTradingDate);
            runId = buildRunId();

            String lockKey = "sfdo:rank:precompute:lock:" + endDateText;
            lock = redissonClient.getLock(lockKey);

            locked = lock.tryLock(0, 120, TimeUnit.MINUTES);

            if (!locked) {
                log.info("Securities firms rank precompute skipped, lock exists. triggerSource={}, endDate={}, runId={}",
                        triggerSource, endDateText, runId);
                return;
            }

            log.info("Securities firms rank precompute use latest available trading date. triggerSource={}, endDate={}, runId={}",
                    triggerSource, endDateText, runId);

            List<String> stockCodes = stockInfoService.getAllFourDigitStockCodes();

            if (stockCodes == null || stockCodes.isEmpty()) {
                log.warn("Securities firms rank precompute skipped, no stock codes found. triggerSource={}, endDate={}, runId={}",
                        triggerSource, endDateText, runId);
                return;
            }

            List<java.util.Date> maxTradingDates = rankService.findLatestTradingDates(
                    latestAvailableTradingDate,
                    120
            );

            if (maxTradingDates == null || maxTradingDates.isEmpty()) {
                log.warn("Securities firms rank precompute skipped, no trading dates found. triggerSource={}, endDate={}, runId={}",
                        triggerSource, endDateText, runId);
                return;
            }

            List<PrecomputeBatch> batches = buildPrecomputeBatches(stockCodes, STOCK_BATCH_SIZE);

            rankRedisService.markRunRunning(
                    runId,
                    triggerSource,
                    endDateText,
                    stockCodes.size(),
                    batches.size(),
                    PRECOMPUTE_WORKER_COUNT,
                    STOCK_BATCH_SIZE
            );

            log.info("Securities firms rank precompute start. triggerSource={}, endDate={}, runId={}, stockCount={}, rangeDays={}, maxTradingDays={}, batchSize={}, workerCount={}, totalBatchCount={}, batchSleepMillis={}",
                    triggerSource,
                    endDateText,
                    runId,
                    stockCodes.size(),
                    RANGE_DAYS_LIST,
                    maxTradingDates.size(),
                    STOCK_BATCH_SIZE,
                    PRECOMPUTE_WORKER_COUNT,
                    batches.size(),
                    BATCH_SLEEP_MILLIS);

            PrecomputeRunResult runResult = executeBatchesConcurrently(
                    triggerSource,
                    endDateText,
                    runId,
                    latestAvailableTradingDate,
                    batches
            );

            long precomputeCostMillis = System.currentTimeMillis() - precomputeStartMillis;
            long usedMemoryMb = getUsedMemoryMb();

            validateRunBeforeBatchPublish(runResult);

            rankRedisService.publishSuccessfulStocks(
                    endDateText,
                    runId,
                    runResult.getPublishedStockCodes(),
                    runResult.getTotalWriteCount(),
                    batches.size(),
                    runResult.getFinishedBatchCount(),
                    runResult.getFailedBatchCount(),
                    precomputeCostMillis
            );

            log.info("Securities firms rank precompute finished and published successful batches. triggerSource={}, endDate={}, runId={}, stockCount={}, totalBatchCount={}, finishedBatchCount={}, failedBatchCount={}, workerCount={}, publishedStockCount={}, totalWriteCount={}, costMillis={}, usedMemoryMb={}",
                    triggerSource,
                    endDateText,
                    runId,
                    stockCodes.size(),
                    batches.size(),
                    runResult.getFinishedBatchCount(),
                    runResult.getFailedBatchCount(),
                    PRECOMPUTE_WORKER_COUNT,
                    runResult.getPublishedStockCodes().size(),
                    runResult.getTotalWriteCount(),
                    precomputeCostMillis,
                    usedMemoryMb);

        } catch (Exception e) {
            if (runId != null) {
                rankRedisService.markRunFailed(runId, endDateText, e.getMessage());
            }

            log.error("Securities firms rank precompute failed. triggerSource={}, endDate={}, runId={}",
                    triggerSource, endDateText, runId, e);
        } finally {
            if (locked && lock != null && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private PrecomputeRunResult executeBatchesConcurrently(
            String triggerSource,
            String endDateText,
            String runId,
            java.util.Date latestAvailableTradingDate,
            List<PrecomputeBatch> batches
    ) {
        if (batches == null || batches.isEmpty()) {
            return new PrecomputeRunResult(0, 0, 0, List.of());
        }

        ExecutorService executorService = Executors.newFixedThreadPool(
                PRECOMPUTE_WORKER_COUNT,
                buildPrecomputeWorkerThreadFactory()
        );

        List<Future<PrecomputeBatchResult>> futures = new ArrayList<>(batches.size());

        long parallelStartMillis = System.currentTimeMillis();

        try {
            for (PrecomputeBatch batch : batches) {
                futures.add(executorService.submit(() -> processBatch(
                        triggerSource,
                        endDateText,
                        runId,
                        latestAvailableTradingDate,
                        batch,
                        batches.size()
                )));
            }

            int totalWriteCount = 0;
            int finishedBatchCount = 0;
            int failedBatchCount = 0;
            List<String> publishedStockCodes = new ArrayList<>();

            for (Future<PrecomputeBatchResult> future : futures) {
                try {
                    PrecomputeBatchResult batchResult = future.get();
                    totalWriteCount += batchResult.getWriteCount();
                    publishedStockCodes.addAll(batchResult.getPublishedStockCodes());
                    finishedBatchCount++;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    cancelFutures(futures);
                    throw new RuntimeException("Securities firms rank precompute interrupted", e);
                } catch (ExecutionException e) {
                    failedBatchCount++;

                    log.error("Securities firms rank precompute batch task failed, this batch will keep old active data. triggerSource={}, endDate={}, runId={}",
                            triggerSource, endDateText, runId, e.getCause() == null ? e : e.getCause());
                }
            }

            long parallelCostMillis = System.currentTimeMillis() - parallelStartMillis;
            long usedMemoryMb = getUsedMemoryMb();

            log.info("Securities firms rank precompute parallel batches finished. triggerSource={}, endDate={}, runId={}, workerCount={}, totalBatchCount={}, finishedBatchCount={}, failedBatchCount={}, publishedStockCount={}, totalWriteCount={}, costMillis={}, usedMemoryMb={}",
                    triggerSource,
                    endDateText,
                    runId,
                    PRECOMPUTE_WORKER_COUNT,
                    batches.size(),
                    finishedBatchCount,
                    failedBatchCount,
                    publishedStockCodes.size(),
                    totalWriteCount,
                    parallelCostMillis,
                    usedMemoryMb);

            return new PrecomputeRunResult(totalWriteCount, finishedBatchCount, failedBatchCount, publishedStockCodes);

        } finally {
            shutdownExecutorQuietly(executorService);
        }
    }

    private PrecomputeBatchResult processBatch(
            String triggerSource,
            String endDateText,
            String runId,
            java.util.Date latestAvailableTradingDate,
            PrecomputeBatch batch,
            int totalBatchCount
    ) {
        String workerName = Thread.currentThread().getName();
        long batchStartMillis = System.currentTimeMillis();

        log.info("Securities firms rank precompute batch start. triggerSource={}, endDate={}, runId={}, workerName={}, batchIndex={}, totalBatchCount={}, batchStart={}, batchEnd={}, batchSize={}, firstStockCode={}, lastStockCode={}, usedMemoryMb={}",
                triggerSource,
                endDateText,
                runId,
                workerName,
                batch.getBatchIndex(),
                totalBatchCount,
                batch.getStartIndex(),
                batch.getEndIndex(),
                batch.getStockCodes().size(),
                batch.getFirstStockCode(),
                batch.getLastStockCode(),
                getUsedMemoryMb());

        List<SecuritiesFirmsRankResult> results = rankService.calculateFixedRankBatchAllRangesByLatestTradingDate(
                batch.getStockCodes(),
                RANGE_DAYS_LIST,
                latestAvailableTradingDate
        );

        long redisStartMillis = System.currentTimeMillis();

        List<SecuritiesFirmsRankResult> doneResults = filterDoneResults(results);
        int batchWriteCount = writeFixedRankResultsToRedis(endDateText, runId, doneResults);
        List<String> publishedStockCodes = findCompletePublishedStockCodes(doneResults);

        long redisCostMillis = System.currentTimeMillis() - redisStartMillis;
        long batchCostMillis = System.currentTimeMillis() - batchStartMillis;
        int resultCount = results == null ? 0 : results.size();
        int doneResultCount = countDoneResults(results);
        long usedMemoryMb = getUsedMemoryMb();

        log.info("Securities firms rank precompute batch finished. triggerSource={}, endDate={}, runId={}, workerName={}, batchIndex={}, totalBatchCount={}, batchStart={}, batchEnd={}, batchSize={}, resultCount={}, doneResultCount={}, writeCount={}, publishedStockCount={}, redisCostMillis={}, costMillis={}, usedMemoryMb={}",
                triggerSource,
                endDateText,
                runId,
                workerName,
                batch.getBatchIndex(),
                totalBatchCount,
                batch.getStartIndex(),
                batch.getEndIndex(),
                batch.getStockCodes().size(),
                resultCount,
                doneResultCount,
                batchWriteCount,
                publishedStockCodes.size(),
                redisCostMillis,
                batchCostMillis,
                usedMemoryMb);

        sleepQuietly(BATCH_SLEEP_MILLIS);

        return new PrecomputeBatchResult(batch.getBatchIndex(), batchWriteCount, publishedStockCodes, batchCostMillis);
    }

    private List<PrecomputeBatch> buildPrecomputeBatches(List<String> stockCodes, int batchSize) {
        if (stockCodes == null || stockCodes.isEmpty() || batchSize <= 0) {
            return List.of();
        }

        List<PrecomputeBatch> batches = new ArrayList<>();
        int batchIndex = 0;

        for (int start = 0; start < stockCodes.size(); start += batchSize) {
            int end = Math.min(start + batchSize, stockCodes.size());
            batches.add(new PrecomputeBatch(
                    batchIndex,
                    start,
                    end,
                    new ArrayList<>(stockCodes.subList(start, end))
            ));
            batchIndex++;
        }

        return batches;
    }

    private ThreadFactory buildPrecomputeWorkerThreadFactory() {
        AtomicInteger threadNumber = new AtomicInteger(1);

        return runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName("sfdo-rank-worker-" + threadNumber.getAndIncrement());
            thread.setDaemon(false);
            return thread;
        };
    }

    private void cancelFutures(List<Future<PrecomputeBatchResult>> futures) {
        if (futures == null || futures.isEmpty()) {
            return;
        }

        for (Future<PrecomputeBatchResult> future : futures) {
            if (future != null && !future.isDone()) {
                future.cancel(true);
            }
        }
    }

    private void shutdownExecutorQuietly(ExecutorService executorService) {
        if (executorService == null) {
            return;
        }

        executorService.shutdown();

        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private int writeFixedRankResultsToRedis(
            String endDateText,
            String runId,
            List<SecuritiesFirmsRankResult> doneResults
    ) {
        if (doneResults == null || doneResults.isEmpty()) {
            return 0;
        }

        return rankRedisService.putFixedRanksBatch(endDateText, runId, doneResults);
    }

    private List<SecuritiesFirmsRankResult> filterDoneResults(List<SecuritiesFirmsRankResult> results) {
        if (results == null || results.isEmpty()) {
            return List.of();
        }

        return results.stream()
                .filter(result -> result != null && "DONE".equals(result.getStatus()))
                .toList();
    }

    private int countDoneResults(List<SecuritiesFirmsRankResult> results) {
        return filterDoneResults(results).size();
    }

    private List<String> findCompletePublishedStockCodes(List<SecuritiesFirmsRankResult> doneResults) {
        if (doneResults == null || doneResults.isEmpty()) {
            return List.of();
        }

        Map<String, Set<Integer>> stockRangeMap = new LinkedHashMap<>();

        for (SecuritiesFirmsRankResult result : doneResults) {
            if (result == null || result.getStockCode() == null || result.getStockCode().isBlank()
                    || result.getRangeDays() == null) {
                continue;
            }

            stockRangeMap.computeIfAbsent(result.getStockCode(), ignored -> new LinkedHashSet<>())
                    .add(result.getRangeDays());
        }

        List<String> completeStockCodes = new ArrayList<>();

        for (Map.Entry<String, Set<Integer>> entry : stockRangeMap.entrySet()) {
            if (entry.getValue().containsAll(RANGE_DAYS_LIST)) {
                completeStockCodes.add(entry.getKey());
            }
        }

        return completeStockCodes;
    }

    private void validateRunBeforeBatchPublish(PrecomputeRunResult runResult) {
        if (runResult == null) {
            throw new IllegalStateException("precompute run result is null");
        }

        if (runResult.getTotalWriteCount() <= 0 || runResult.getPublishedStockCodes().isEmpty()) {
            throw new IllegalStateException("precompute has no successful batch to publish. totalWriteCount="
                    + runResult.getTotalWriteCount()
                    + ", publishedStockCount=" + runResult.getPublishedStockCodes().size());
        }

        if (runResult.getTotalWriteCount() % RANGE_DAYS_LIST.size() != 0) {
            throw new IllegalStateException("precompute totalWriteCount is not divisible by range count. totalWriteCount="
                    + runResult.getTotalWriteCount() + ", rangeCount=" + RANGE_DAYS_LIST.size());
        }
    }

    private long getUsedMemoryMb() {
        Runtime runtime = Runtime.getRuntime();
        return (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
    }

    private String buildRunId() {
        return LocalDateTime.now().format(RUN_ID_FORMATTER);
    }

    private String formatDate(java.util.Date date) {
        if (date == null) {
            return null;
        }

        return DATE_FORMAT.get().format(date);
    }

    private void sleepQuietly(long millis) {
        if (millis <= 0) {
            return;
        }

        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Securities firms rank precompute interrupted", e);
        }
    }

    private static class PrecomputeBatch {
        private final int batchIndex;
        private final int startIndex;
        private final int endIndex;
        private final List<String> stockCodes;

        private PrecomputeBatch(
                int batchIndex,
                int startIndex,
                int endIndex,
                List<String> stockCodes
        ) {
            this.batchIndex = batchIndex;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.stockCodes = stockCodes;
        }

        private int getBatchIndex() {
            return batchIndex;
        }

        private int getStartIndex() {
            return startIndex;
        }

        private int getEndIndex() {
            return endIndex;
        }

        private List<String> getStockCodes() {
            return stockCodes;
        }

        private String getFirstStockCode() {
            return stockCodes == null || stockCodes.isEmpty() ? null : stockCodes.get(0);
        }

        private String getLastStockCode() {
            return stockCodes == null || stockCodes.isEmpty() ? null : stockCodes.get(stockCodes.size() - 1);
        }
    }

    private static class PrecomputeBatchResult {
        private final int batchIndex;
        private final int writeCount;
        private final List<String> publishedStockCodes;
        private final long costMillis;

        private PrecomputeBatchResult(int batchIndex, int writeCount, List<String> publishedStockCodes, long costMillis) {
            this.batchIndex = batchIndex;
            this.writeCount = writeCount;
            this.publishedStockCodes = publishedStockCodes == null ? List.of() : publishedStockCodes;
            this.costMillis = costMillis;
        }

        @SuppressWarnings("unused")
        private int getBatchIndex() {
            return batchIndex;
        }

        private int getWriteCount() {
            return writeCount;
        }

        private List<String> getPublishedStockCodes() {
            return publishedStockCodes;
        }

        @SuppressWarnings("unused")
        private long getCostMillis() {
            return costMillis;
        }
    }

    private static class PrecomputeRunResult {
        private final int totalWriteCount;
        private final int finishedBatchCount;
        private final int failedBatchCount;
        private final List<String> publishedStockCodes;

        private PrecomputeRunResult(
                int totalWriteCount,
                int finishedBatchCount,
                int failedBatchCount,
                List<String> publishedStockCodes
        ) {
            this.totalWriteCount = totalWriteCount;
            this.finishedBatchCount = finishedBatchCount;
            this.failedBatchCount = failedBatchCount;
            this.publishedStockCodes = publishedStockCodes == null ? List.of() : publishedStockCodes;
        }

        private int getTotalWriteCount() {
            return totalWriteCount;
        }

        private int getFinishedBatchCount() {
            return finishedBatchCount;
        }

        private int getFailedBatchCount() {
            return failedBatchCount;
        }

        private List<String> getPublishedStockCodes() {
            return publishedStockCodes;
        }
    }
}
