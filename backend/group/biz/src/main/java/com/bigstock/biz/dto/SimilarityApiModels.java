package com.bigstock.biz.dto;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

public final class SimilarityApiModels {

    private SimilarityApiModels() {
    }

    @Schema(name = "SimilaritySearchRequest", description = "相似股票查詢參數")
    public record SearchRequest(
            @JsonProperty("stock_code")
            @Schema(description = "目標股票代碼", example = "3605", minLength = 1, requiredMode = RequiredMode.REQUIRED)
            String stockCode,
            @JsonProperty("window_size")
            @Schema(description = "比對交易日數", example = "65", minimum = "5", maximum = "500", requiredMode = RequiredMode.REQUIRED)
            Integer windowSize,
            @JsonProperty("top_n")
            @Schema(description = "回傳筆數", example = "10", minimum = "1", maximum = "100", requiredMode = RequiredMode.REQUIRED)
            Integer topN,
            @JsonProperty("anchor_date")
            @Schema(description = "目標走勢結束日；省略時由 Python 選取最新日期", nullable = true, example = "2026-08-04")
            LocalDate anchorDate,
            @JsonProperty("higher_timeframe_mode")
            @Schema(description = "週月線處理方式；省略時沿用 Python 設定", nullable = true, allowableValues = {"off", "boost", "filter"})
            String higherTimeframeMode) {
    }

    @Schema(name = "SimilaritySearchResponse")
    public record SearchResponse(
            @JsonProperty("stock_code") String stockCode,
            @JsonProperty("window_size") Integer windowSize,
            @JsonProperty("top_n") Integer topN,
            @JsonProperty("anchor_date") @Schema(nullable = true) LocalDate anchorDate,
            @JsonProperty("higher_timeframe_mode")
            @Schema(allowableValues = {"off", "boost", "filter"}) String higherTimeframeMode,
            List<Result> results,
            @JsonProperty("skipped_count") Integer skippedCount,
            @JsonProperty("candidate_pool_count") Integer candidatePoolCount,
            @JsonProperty("higher_timeframe_filtered_count") Integer higherTimeframeFilteredCount,
            @JsonProperty("higher_timeframe_unavailable_count") Integer higherTimeframeUnavailableCount,
            String message) {
    }

    @Schema(name = "SimilarityResult")
    public record Result(
            @JsonProperty("stock_code") String stockCode,
            @JsonProperty("start_date") LocalDate startDate,
            @JsonProperty("end_date") LocalDate endDate,
            @Schema(description = "最終排名分數") Double score,
            @JsonProperty("shape_score") Double shapeScore,
            @JsonProperty("soft_dtw_score") Double softDtwScore,
            @JsonProperty("cosine_score") Double cosineScore,
            @JsonProperty("shifted_cosine_score") Double shiftedCosineScore,
            @JsonProperty("best_shift") Integer bestShift,
            @JsonProperty("multi_scale_score") Double multiScaleScore,
            @JsonProperty("turning_point_score") Double turningPointScore,
            @JsonProperty("soft_dtw_distance") Double softDtwDistance,
            @JsonProperty("ranking_distance") Double rankingDistance,
            @JsonProperty("candidate_sources") List<String> candidateSources,
            @JsonProperty("soft_dtw_rank") Integer softDtwRank,
            @JsonProperty("shifted_cosine_rank") Integer shiftedCosineRank,
            @JsonProperty("multi_scale_rank") Integer multiScaleRank,
            @JsonProperty("turning_point_rank") Integer turningPointRank,
            @JsonProperty("higher_timeframe") @Schema(nullable = true) HigherTimeframeDetail higherTimeframe) {
    }

    @Schema(name = "SimilarityHigherTimeframeDetail")
    public record HigherTimeframeDetail(
            Boolean available,
            Boolean passed,
            @JsonProperty("matched_rules") List<Integer> matchedRules,
            @JsonProperty("primary_rule") @Schema(nullable = true) Integer primaryRule,
            @JsonProperty("ranking_boost") Double rankingBoost,
            @JsonProperty("quiet_week_count") @Schema(nullable = true) Integer quietWeekCount,
            @JsonProperty("max_monthly_return") @Schema(nullable = true) Double maxMonthlyReturn,
            @JsonProperty("weekly_bar_count") Integer weeklyBarCount,
            @JsonProperty("monthly_return_count") Integer monthlyReturnCount,
            @JsonProperty("left_low") @Schema(nullable = true) Double leftLow,
            @JsonProperty("right_low") @Schema(nullable = true) Double rightLow,
            @JsonProperty("neckline_high") @Schema(nullable = true) Double necklineHigh,
            @JsonProperty("right_rebound_high") @Schema(nullable = true) Double rightReboundHigh,
            @JsonProperty("pattern_confidence") @Schema(nullable = true) Double patternConfidence,
            String reason) {
    }

    @Schema(name = "SimilarityCacheReloadResponse")
    public record CacheReloadResponse(
            Boolean accepted,
            String version,
            Integer stockCount,
            @Schema(type = "string", format = "date-time") String createdAt) {
    }

    @Schema(name = "SimilarityCacheStatusResponse")
    public record CacheStatusResponse(
            Boolean ready,
            @Schema(nullable = true) String version,
            Integer stockCount,
            @Schema(type = "string", format = "date-time", nullable = true) String createdAt,
            @Schema(nullable = true) Integer featureCount) {
    }

    @Schema(name = "SimilarityHealthResponse")
    public record HealthResponse(
            String status,
            Boolean featureCacheReady,
            @Schema(nullable = true) String featureCacheVersion,
            Integer stockCount) {
    }

    @Schema(name = "SimilarityApiError")
    public record ApiError(
            @Schema(description = "錯誤訊息；Python 參數驗證錯誤可能回傳陣列") Object detail) {
    }
}