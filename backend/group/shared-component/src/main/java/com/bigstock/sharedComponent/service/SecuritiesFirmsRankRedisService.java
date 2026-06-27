package com.bigstock.sharedComponent.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.bigstock.sharedComponent.dto.SecuritiesFirmsRankItem;
import com.bigstock.sharedComponent.dto.SecuritiesFirmsRankResult;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SecuritiesFirmsRankRedisService {

    private static final String RANK_KEY_PREFIX = "sfdo:rank";
    private static final String VERSION_RANK_KEY_PREFIX = "sfdo:rank:v";
    private static final String READY_KEY_PREFIX = "sfdo:rank:ready";
    private static final String ACTIVE_RUN_KEY_PREFIX = "sfdo:rank:active-run";
    private static final String ACTIVE_STOCK_END_DATE_KEY_PREFIX = "sfdo:rank:active-stock-end-date";
    private static final String ACTIVE_STOCK_RUN_KEY_PREFIX = "sfdo:rank:active-stock-run";
    private static final String RUN_META_KEY_PREFIX = "sfdo:rank:run";
    private static final String LATEST_END_DATE_KEY = "sfdo:rank:latest-end-date";

    /**
     * 固定區間排行快取保留 4 天。
     *
     * 注意：這裡控制的是 Redis 內 rank value 的保存時間，不是交易日計算範圍。
     * App 仍然只帶 rangeDays，例如 1 / 3 / 5 / 10 / 20 / 60 / 120，
     * 後端會用 latest-ready-date 對應的最近可用交易日去讀取快取。
     */
    private static final Duration RANK_TTL = Duration.ofDays(4);

    private static final Duration READY_TTL = Duration.ofDays(4);

    private static final Duration RUN_META_TTL = Duration.ofDays(4);

    private static final byte GZIP_MAGIC_1 = (byte) 0x1F;

    private static final byte GZIP_MAGIC_2 = (byte) 0x8B;

    private final StringRedisTemplate stringRedisTemplate;

    private final ObjectMapper objectMapper;

    /**
     * 舊版 key：sfdo:rank:{stockCode}:{rangeDays}:{endDate}
     *
     * 保留這個 method，讓舊資料與舊呼叫方式可以 fallback。
     */
    public String buildFixedRankKey(String stockCode, Integer rangeDays, String endDate) {
        return RANK_KEY_PREFIX + ":" + stockCode + ":" + rangeDays + ":" + endDate;
    }

    /**
     * 新版 key：sfdo:rank:v:{endDate}:{runId}:{stockCode}:{rangeDays}
     */
    public String buildVersionRankKey(String endDate, String runId, String stockCode, Integer rangeDays) {
        return VERSION_RANK_KEY_PREFIX + ":" + endDate + ":" + runId + ":" + stockCode + ":" + rangeDays;
    }

    public String buildReadyKey(String endDate) {
        return READY_KEY_PREFIX + ":" + endDate;
    }

    public String buildActiveRunKey(String endDate) {
        return ACTIVE_RUN_KEY_PREFIX + ":" + endDate;
    }

    public String buildActiveStockEndDateKey(String stockCode) {
        return ACTIVE_STOCK_END_DATE_KEY_PREFIX + ":" + stockCode;
    }

    public String buildActiveStockRunKey(String endDate, String stockCode) {
        return ACTIVE_STOCK_RUN_KEY_PREFIX + ":" + endDate + ":" + stockCode;
    }

    public String buildRunMetaKey(String runId) {
        return RUN_META_KEY_PREFIX + ":" + runId + ":meta";
    }

    public Optional<SecuritiesFirmsRankResult> getFixedRank(String stockCode, Integer rangeDays, String endDate) {
        try {
            Optional<SecuritiesFirmsRankResult> requestedDateResult = getFixedRankByDate(stockCode, rangeDays, endDate);

            if (requestedDateResult.isPresent()) {
                return requestedDateResult;
            }

            Optional<String> stockActiveEndDate = getActiveStockEndDate(stockCode);

            if (stockActiveEndDate.isPresent() && !stockActiveEndDate.get().equals(endDate)) {
                Optional<SecuritiesFirmsRankResult> fallbackResult = getFixedRankByDate(
                        stockCode,
                        rangeDays,
                        stockActiveEndDate.get()
                );

                if (fallbackResult.isPresent()) {
                    log.info("getFixedRank fallback to stock active date. stockCode={}, rangeDays={}, requestedEndDate={}, stockActiveEndDate={}",
                            stockCode, rangeDays, endDate, stockActiveEndDate.get());
                    return fallbackResult;
                }
            }

            return Optional.empty();

        } catch (Exception e) {
            log.error("getFixedRank failed. stockCode={}, rangeDays={}, endDate={}",
                    stockCode, rangeDays, endDate, e);
            return Optional.empty();
        }
    }

    private Optional<SecuritiesFirmsRankResult> getFixedRankByDate(String stockCode, Integer rangeDays, String endDate) {
        if (stockCode == null || stockCode.isBlank() || rangeDays == null || endDate == null || endDate.isBlank()) {
            return Optional.empty();
        }

        Optional<String> activeStockRunId = getActiveStockRunId(endDate, stockCode);

        if (activeStockRunId.isPresent()) {
            String versionKey = buildVersionRankKey(endDate, activeStockRunId.get(), stockCode, rangeDays);
            Optional<SecuritiesFirmsRankResult> versionResult = getRankByKey(versionKey);

            if (versionResult.isPresent()) {
                return versionResult;
            }

            log.warn("getFixedRank stock active version key not found. stockCode={}, rangeDays={}, endDate={}, runId={}, versionKey={}",
                    stockCode, rangeDays, endDate, activeStockRunId.get(), versionKey);
        }

        Optional<String> activeRunId = getActiveRunId(endDate);

        if (activeRunId.isPresent()) {
            String versionKey = buildVersionRankKey(endDate, activeRunId.get(), stockCode, rangeDays);
            Optional<SecuritiesFirmsRankResult> versionResult = getRankByKey(versionKey);

            if (versionResult.isPresent()) {
                return versionResult;
            }

            log.warn("getFixedRank global active version key not found, fallback legacy key. stockCode={}, rangeDays={}, endDate={}, runId={}, versionKey={}",
                    stockCode, rangeDays, endDate, activeRunId.get(), versionKey);
        }

        String legacyKey = buildFixedRankKey(stockCode, rangeDays, endDate);
        return getRankByKey(legacyKey);
    }

    private Optional<SecuritiesFirmsRankResult> getRankByKey(String key) {
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }

        byte[] valueBytes = getBytes(key);

        if (valueBytes == null || valueBytes.length == 0) {
            return Optional.empty();
        }

        try {
            byte[] jsonBytes = isGzip(valueBytes) ? gunzip(valueBytes) : valueBytes;
            SecuritiesFirmsRankResult result = objectMapper.readValue(jsonBytes, SecuritiesFirmsRankResult.class);
            result.setSource("REDIS");
            return Optional.of(result);
        } catch (Exception e) {
            log.error("getRankByKey decode failed. key={}", key, e);
            return Optional.empty();
        }
    }

    /**
     * 舊版單筆寫入：保留給其他舊程式碼使用。
     *
     * 新排程不使用這個 method，新排程會寫入 version key。
     */
    public void putFixedRank(String stockCode, Integer rangeDays, String endDate, SecuritiesFirmsRankResult result) {
        try {
            validateDoneResult(result, endDate);

            String key = buildFixedRankKey(stockCode, rangeDays, endDate);
            byte[] valueBytes = encodeCompressedJson(result);
            setBytes(key, valueBytes, RANK_TTL);

        } catch (Exception e) {
            log.error("putFixedRank failed. stockCode={}, rangeDays={}, endDate={}",
                    stockCode, rangeDays, endDate, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 舊版批次寫入：保留給過渡期使用。
     *
     * 新排程請使用 putFixedRanksBatch(endDate, runId, results)。
     */
    public int putFixedRanksBatch(String endDate, List<SecuritiesFirmsRankResult> results) {
        return putFixedRanksBatchInternal(endDate, null, results, false);
    }

    /**
     * 新版批次寫入：寫入 version key，不會覆蓋目前 active 版本。
     *
     * 只有 publishRun 成功後，App API 才會讀到這批新資料。
     */
    public int putFixedRanksBatch(String endDate, String runId, List<SecuritiesFirmsRankResult> results) {
        return putFixedRanksBatchInternal(endDate, runId, results, true);
    }

    private int putFixedRanksBatchInternal(
            String endDate,
            String runId,
            List<SecuritiesFirmsRankResult> results,
            boolean versioned
    ) {
        if (endDate == null || endDate.isBlank() || results == null || results.isEmpty()) {
            return 0;
        }

        if (versioned && (runId == null || runId.isBlank())) {
            throw new IllegalArgumentException("runId is required when writing versioned rank cache.");
        }

        try {
            List<SecuritiesFirmsRankResult> doneResults = results.stream()
                    .filter(result -> result != null && "DONE".equals(result.getStatus()))
                    .toList();

            if (doneResults.isEmpty()) {
                return 0;
            }

            List<FixedRankRedisValue> values = new ArrayList<>(doneResults.size());

            for (SecuritiesFirmsRankResult result : doneResults) {
                validateDoneResult(result, endDate);

                String key = versioned
                        ? buildVersionRankKey(endDate, runId, result.getStockCode(), result.getRangeDays())
                        : buildFixedRankKey(result.getStockCode(), result.getRangeDays(), endDate);

                values.add(new FixedRankRedisValue(key, encodeCompressedJson(result)));
            }

            stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                for (FixedRankRedisValue value : values) {
                    connection.stringCommands().setEx(
                            value.getKey().getBytes(StandardCharsets.UTF_8),
                            RANK_TTL.getSeconds(),
                            value.getValueBytes()
                    );
                }
                return null;
            });

            return values.size();

        } catch (Exception e) {
            log.error("putFixedRanksBatch failed. endDate={}, runId={}, versioned={}, resultCount={}",
                    endDate, runId, versioned, results.size(), e);
            throw new RuntimeException(e);
        }
    }

    public void markRunRunning(
            String runId,
            String triggerSource,
            String endDate,
            int stockCount,
            int totalBatchCount,
            int workerCount,
            int batchSize
    ) {
        if (runId == null || runId.isBlank()) {
            return;
        }

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("status", "RUNNING");
        meta.put("runId", runId);
        meta.put("triggerSource", triggerSource);
        meta.put("endDate", endDate);
        meta.put("stockCount", stockCount);
        meta.put("totalBatchCount", totalBatchCount);
        meta.put("workerCount", workerCount);
        meta.put("batchSize", batchSize);
        meta.put("startedAt", LocalDateTime.now().toString());

        putRunMeta(runId, meta);
    }

    public void markRunFailed(String runId, String endDate, String reason) {
        if (runId == null || runId.isBlank()) {
            return;
        }

        Map<String, Object> meta = readRunMeta(runId);
        meta.put("status", "FAILED");
        meta.put("runId", runId);
        meta.put("endDate", endDate);
        meta.put("failedAt", LocalDateTime.now().toString());
        meta.put("reason", reason);

        putRunMeta(runId, meta);
    }

    /**
     * 全部 batch 成功且驗證通過後才呼叫。
     *
     * 使用 Redis transaction 一次切 active-run、latest-end-date、ready 與 run meta。
     */
    public void publishRun(
            String endDate,
            String runId,
            int totalWriteCount,
            int totalBatchCount,
            long costMillis
    ) {
        if (endDate == null || endDate.isBlank() || runId == null || runId.isBlank()) {
            throw new IllegalArgumentException("endDate and runId are required for publishRun.");
        }

        try {
            Map<String, Object> meta = readRunMeta(runId);
            meta.put("status", "SUCCESS");
            meta.put("runId", runId);
            meta.put("endDate", endDate);
            meta.put("totalWriteCount", totalWriteCount);
            meta.put("totalBatchCount", totalBatchCount);
            meta.put("costMillis", costMillis);
            meta.put("publishedAt", LocalDateTime.now().toString());

            byte[] activeRunKey = buildActiveRunKey(endDate).getBytes(StandardCharsets.UTF_8);
            byte[] latestEndDateKey = LATEST_END_DATE_KEY.getBytes(StandardCharsets.UTF_8);
            byte[] readyKey = buildReadyKey(endDate).getBytes(StandardCharsets.UTF_8);
            byte[] metaKey = buildRunMetaKey(runId).getBytes(StandardCharsets.UTF_8);

            byte[] runIdBytes = runId.getBytes(StandardCharsets.UTF_8);
            byte[] endDateBytes = endDate.getBytes(StandardCharsets.UTF_8);
            byte[] doneBytes = "DONE".getBytes(StandardCharsets.UTF_8);
            byte[] metaBytes = objectMapper.writeValueAsBytes(meta);

            stringRedisTemplate.execute((RedisCallback<Object>) connection -> {
                connection.multi();
                connection.stringCommands().setEx(activeRunKey, READY_TTL.getSeconds(), runIdBytes);
                connection.stringCommands().setEx(latestEndDateKey, READY_TTL.getSeconds(), endDateBytes);
                connection.stringCommands().setEx(readyKey, READY_TTL.getSeconds(), doneBytes);
                connection.stringCommands().setEx(metaKey, RUN_META_TTL.getSeconds(), metaBytes);
                connection.exec();
                return null;
            });

        } catch (Exception e) {
            log.error("publishRun failed. endDate={}, runId={}, totalWriteCount={}", endDate, runId, totalWriteCount, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 批次容錯 publish：只有成功完成的股票會切到本次 runId。
     * 失敗 batch 的股票不會更新 active-stock pointer，因此 API 會繼續讀前一版成功資料。
     */
    public void publishSuccessfulStocks(
            String endDate,
            String runId,
            List<String> publishedStockCodes,
            int totalWriteCount,
            int totalBatchCount,
            int finishedBatchCount,
            int failedBatchCount,
            long costMillis
    ) {
        if (endDate == null || endDate.isBlank() || runId == null || runId.isBlank()) {
            throw new IllegalArgumentException("endDate and runId are required for publishSuccessfulStocks.");
        }

        if (publishedStockCodes == null || publishedStockCodes.isEmpty()) {
            throw new IllegalArgumentException("publishedStockCodes is empty. endDate=" + endDate + ", runId=" + runId);
        }

        try {
            List<String> distinctStockCodes = publishedStockCodes.stream()
                    .filter(stockCode -> stockCode != null && !stockCode.isBlank())
                    .distinct()
                    .toList();

            if (distinctStockCodes.isEmpty()) {
                throw new IllegalArgumentException("distinct publishedStockCodes is empty. endDate=" + endDate + ", runId=" + runId);
            }

            String status = failedBatchCount > 0 ? "PARTIAL_SUCCESS" : "SUCCESS";

            Map<String, Object> meta = readRunMeta(runId);
            meta.put("status", status);
            meta.put("runId", runId);
            meta.put("endDate", endDate);
            meta.put("totalWriteCount", totalWriteCount);
            meta.put("totalBatchCount", totalBatchCount);
            meta.put("finishedBatchCount", finishedBatchCount);
            meta.put("failedBatchCount", failedBatchCount);
            meta.put("publishedStockCount", distinctStockCodes.size());
            meta.put("costMillis", costMillis);
            meta.put("publishedAt", LocalDateTime.now().toString());

            byte[] latestEndDateKey = LATEST_END_DATE_KEY.getBytes(StandardCharsets.UTF_8);
            byte[] readyKey = buildReadyKey(endDate).getBytes(StandardCharsets.UTF_8);
            byte[] metaKey = buildRunMetaKey(runId).getBytes(StandardCharsets.UTF_8);

            byte[] runIdBytes = runId.getBytes(StandardCharsets.UTF_8);
            byte[] endDateBytes = endDate.getBytes(StandardCharsets.UTF_8);
            byte[] doneBytes = "DONE".getBytes(StandardCharsets.UTF_8);
            byte[] metaBytes = objectMapper.writeValueAsBytes(meta);

            stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                for (String stockCode : distinctStockCodes) {
                    connection.stringCommands().setEx(
                            buildActiveStockEndDateKey(stockCode).getBytes(StandardCharsets.UTF_8),
                            READY_TTL.getSeconds(),
                            endDateBytes
                    );
                    connection.stringCommands().setEx(
                            buildActiveStockRunKey(endDate, stockCode).getBytes(StandardCharsets.UTF_8),
                            READY_TTL.getSeconds(),
                            runIdBytes
                    );
                }

                connection.stringCommands().setEx(latestEndDateKey, READY_TTL.getSeconds(), endDateBytes);
                connection.stringCommands().setEx(readyKey, READY_TTL.getSeconds(), doneBytes);
                connection.stringCommands().setEx(metaKey, RUN_META_TTL.getSeconds(), metaBytes);
                return null;
            });

            log.info("publishSuccessfulStocks finished. endDate={}, runId={}, status={}, totalBatchCount={}, finishedBatchCount={}, failedBatchCount={}, publishedStockCount={}, totalWriteCount={}, costMillis={}",
                    endDate, runId, status, totalBatchCount, finishedBatchCount, failedBatchCount,
                    distinctStockCodes.size(), totalWriteCount, costMillis);

        } catch (Exception e) {
            log.error("publishSuccessfulStocks failed. endDate={}, runId={}, totalWriteCount={}", endDate, runId, totalWriteCount, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 舊版 ready method：保留相容。
     * 新排程請使用 publishRun。
     */
    public void markDailyReady(String endDate) {
        stringRedisTemplate.opsForValue().set(buildReadyKey(endDate), "DONE", READY_TTL);
        stringRedisTemplate.opsForValue().set(LATEST_END_DATE_KEY, endDate, READY_TTL);
    }

    public boolean isDailyReady(String endDate) {
        if (endDate == null || endDate.isBlank()) {
            return false;
        }

        Optional<String> activeRunId = getActiveRunId(endDate);

        if (activeRunId.isPresent()) {
            return true;
        }

        String value = stringRedisTemplate.opsForValue().get(buildReadyKey(endDate));
        return "DONE".equals(value);
    }

    public Optional<String> getLatestEndDate() {
        return Optional.ofNullable(stringRedisTemplate.opsForValue().get(LATEST_END_DATE_KEY));
    }

    public Optional<String> getActiveRunId(String endDate) {
        if (endDate == null || endDate.isBlank()) {
            return Optional.empty();
        }

        return Optional.ofNullable(stringRedisTemplate.opsForValue().get(buildActiveRunKey(endDate)))
                .filter(value -> !value.isBlank());
    }

    public Optional<String> getActiveStockEndDate(String stockCode) {
        if (stockCode == null || stockCode.isBlank()) {
            return Optional.empty();
        }

        return Optional.ofNullable(stringRedisTemplate.opsForValue().get(buildActiveStockEndDateKey(stockCode)))
                .filter(value -> !value.isBlank());
    }

    public Optional<String> getActiveStockRunId(String endDate, String stockCode) {
        if (endDate == null || endDate.isBlank() || stockCode == null || stockCode.isBlank()) {
            return Optional.empty();
        }

        return Optional.ofNullable(stringRedisTemplate.opsForValue().get(buildActiveStockRunKey(endDate, stockCode)))
                .filter(value -> !value.isBlank());
    }

    private void putRunMeta(String runId, Map<String, Object> meta) {
        try {
            String key = buildRunMetaKey(runId);
            byte[] metaBytes = objectMapper.writeValueAsBytes(meta == null ? Map.of() : meta);
            setBytes(key, metaBytes, RUN_META_TTL);
        } catch (Exception e) {
            log.warn("putRunMeta failed. runId={}", runId, e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readRunMeta(String runId) {
        try {
            byte[] metaBytes = getBytes(buildRunMetaKey(runId));

            if (metaBytes == null || metaBytes.length == 0) {
                return new LinkedHashMap<>();
            }

            return objectMapper.readValue(metaBytes, LinkedHashMap.class);
        } catch (Exception e) {
            log.warn("readRunMeta failed. runId={}", runId, e);
            return new LinkedHashMap<>();
        }
    }

    private void validateDoneResult(SecuritiesFirmsRankResult result, String expectedEndDate) {
        if (result == null) {
            throw new IllegalArgumentException("rank result is null");
        }

        if (!"DONE".equals(result.getStatus())) {
            throw new IllegalArgumentException("rank result status is not DONE. stockCode=" + result.getStockCode());
        }

        if (result.getStockCode() == null || result.getStockCode().isBlank()) {
            throw new IllegalArgumentException("rank result stockCode is blank");
        }

        if (result.getRangeDays() == null) {
            throw new IllegalArgumentException("rank result rangeDays is null. stockCode=" + result.getStockCode());
        }

        validateResultEndDate(result, expectedEndDate);

        if (result.getBuyTop15() == null || result.getSellTop15() == null) {
            throw new IllegalArgumentException("rank result top15 list is null. stockCode=" + result.getStockCode()
                    + ", rangeDays=" + result.getRangeDays());
        }

        validateRankItems(result.getStockCode(), result.getRangeDays(), "buyTop15", result.getBuyTop15());
        validateRankItems(result.getStockCode(), result.getRangeDays(), "sellTop15", result.getSellTop15());
    }

    private void validateResultEndDate(SecuritiesFirmsRankResult result, String expectedEndDate) {
        if (result.getEndDate() == null || result.getEndDate().isBlank()) {
            throw new IllegalArgumentException("rank result endDate is blank. stockCode=" + result.getStockCode()
                    + ", rangeDays=" + result.getRangeDays());
        }

        if (expectedEndDate == null || expectedEndDate.isBlank()) {
            return;
        }

        try {
            LocalDate expected = LocalDate.parse(expectedEndDate);
            LocalDate actual = LocalDate.parse(result.getEndDate());

            if (actual.isAfter(expected)) {
                throw new IllegalArgumentException("rank result endDate is after expectedEndDate. stockCode=" + result.getStockCode()
                        + ", rangeDays=" + result.getRangeDays()
                        + ", expectedEndDate=" + expectedEndDate
                        + ", resultEndDate=" + result.getEndDate());
            }
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("rank result endDate format invalid. stockCode=" + result.getStockCode()
                    + ", rangeDays=" + result.getRangeDays()
                    + ", expectedEndDate=" + expectedEndDate
                    + ", resultEndDate=" + result.getEndDate(), e);
        }
    }

    private void validateRankItems(
            String stockCode,
            Integer rangeDays,
            String listName,
            List<SecuritiesFirmsRankItem> items
    ) {
        for (SecuritiesFirmsRankItem item : items) {
            if (item == null) {
                throw new IllegalArgumentException(listName + " item is null. stockCode=" + stockCode + ", rangeDays=" + rangeDays);
            }

            if (item.getRank() == null) {
                throw new IllegalArgumentException(listName + " rank is null. stockCode=" + stockCode + ", rangeDays=" + rangeDays);
            }

            if (item.getSecuritiesFirms() == null || item.getSecuritiesFirms().isBlank()) {
                throw new IllegalArgumentException(listName + " securitiesFirms is blank. stockCode=" + stockCode + ", rangeDays=" + rangeDays);
            }

            if (item.getBuyAmount() == null
                    || item.getSellAmount() == null
                    || item.getNetAmount() == null
                    || item.getBuyLots() == null
                    || item.getSellLots() == null
                    || item.getNetLots() == null) {
                throw new IllegalArgumentException(listName + " amount/lots has null. stockCode=" + stockCode
                        + ", rangeDays=" + rangeDays + ", securitiesFirms=" + item.getSecuritiesFirms());
            }
        }
    }

    private byte[] encodeCompressedJson(SecuritiesFirmsRankResult result) throws Exception {
        byte[] jsonBytes = objectMapper.writeValueAsBytes(result);
        return gzip(jsonBytes);
    }

    private byte[] gzip(byte[] source) throws Exception {
        if (source == null || source.length == 0) {
            return source;
        }

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream)) {
            gzipOutputStream.write(source);
        }

        return byteArrayOutputStream.toByteArray();
    }

    private byte[] gunzip(byte[] source) throws Exception {
        if (source == null || source.length == 0) {
            return source;
        }

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        try (GZIPInputStream gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(source))) {
            byte[] buffer = new byte[4096];
            int len;

            while ((len = gzipInputStream.read(buffer)) > 0) {
                byteArrayOutputStream.write(buffer, 0, len);
            }
        }

        return byteArrayOutputStream.toByteArray();
    }

    private boolean isGzip(byte[] source) {
        return source != null
                && source.length >= 2
                && source[0] == GZIP_MAGIC_1
                && source[1] == GZIP_MAGIC_2;
    }

    private byte[] getBytes(String key) {
        return stringRedisTemplate.execute((RedisCallback<byte[]>) connection ->
                connection.stringCommands().get(key.getBytes(StandardCharsets.UTF_8))
        );
    }

    private void setBytes(String key, byte[] valueBytes, Duration ttl) {
        stringRedisTemplate.execute((RedisCallback<Object>) connection -> {
            connection.stringCommands().setEx(
                    key.getBytes(StandardCharsets.UTF_8),
                    ttl.getSeconds(),
                    valueBytes
            );
            return null;
        });
    }

    private static class FixedRankRedisValue {
        private final String key;
        private final byte[] valueBytes;

        private FixedRankRedisValue(String key, byte[] valueBytes) {
            this.key = key;
            this.valueBytes = valueBytes;
        }

        private String getKey() {
            return key;
        }

        private byte[] getValueBytes() {
            return valueBytes;
        }
    }
}
