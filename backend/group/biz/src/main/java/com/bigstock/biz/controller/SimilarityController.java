package com.bigstock.biz.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bigstock.biz.dto.SimilarityApiModels;
import com.bigstock.biz.service.SimilarityProxyService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/biz/similarity", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Similarity Controller", description = "餘弦相似度搜尋與快取管理")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({
        @ApiResponse(responseCode = "502", description = "無法連線到相似度服務", content = @Content(schema = @Schema(implementation = SimilarityApiModels.ApiError.class))),
        @ApiResponse(responseCode = "504", description = "相似度服務回應逾時", content = @Content(schema = @Schema(implementation = SimilarityApiModels.ApiError.class))),
        @ApiResponse(responseCode = "default", description = "保留 Python 服務的狀態碼與回應內容", content = @Content(schema = @Schema(implementation = SimilarityApiModels.ApiError.class)))
})
public class SimilarityController {

    private final SimilarityProxyService service;

    @Operation(summary = "搜尋相似股票", description = "由 biz 轉送至相似度服務，參數及排名結果沿用 Python API。",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    content = @Content(schema = @Schema(implementation = SimilarityApiModels.SearchRequest.class))))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "搜尋結果", content = @Content(schema = @Schema(implementation = SimilarityApiModels.SearchResponse.class))),
            @ApiResponse(responseCode = "422", description = "參數驗證失敗", content = @Content(schema = @Schema(implementation = SimilarityApiModels.ApiError.class)))
    })
    @PostMapping(value = "/search", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> search(@RequestBody(required = false) byte[] request) {
        return service.search(request);
    }

    @Operation(summary = "重載相似度快取", description = "通知 Python 重新載入 Redis；完成後回傳 202。")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "快取重載完成", content = @Content(schema = @Schema(implementation = SimilarityApiModels.CacheReloadResponse.class))),
            @ApiResponse(responseCode = "409", description = "已有快取重載作業執行中", content = @Content(schema = @Schema(implementation = SimilarityApiModels.ApiError.class)))
    })
    @PostMapping("/cache/reload")
    public ResponseEntity<byte[]> reloadCache() {
        return service.reloadCache();
    }

    @Operation(summary = "查詢相似度快取狀態")
    @ApiResponse(responseCode = "200", description = "快取狀態", content = @Content(schema = @Schema(implementation = SimilarityApiModels.CacheStatusResponse.class)))
    @GetMapping("/cache/status")
    public ResponseEntity<byte[]> cacheStatus() {
        return service.cacheStatus();
    }

    @Operation(summary = "查詢相似度服務健康狀態")
    @ApiResponse(responseCode = "200", description = "服務狀態", content = @Content(schema = @Schema(implementation = SimilarityApiModels.HealthResponse.class)))
    @GetMapping("/health")
    public ResponseEntity<byte[]> health() {
        return service.health();
    }
}