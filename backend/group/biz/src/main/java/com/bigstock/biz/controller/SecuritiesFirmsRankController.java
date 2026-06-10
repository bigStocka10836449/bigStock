package com.bigstock.biz.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bigstock.biz.service.SecuritiesFirmsRankQueryService;
import com.bigstock.sharedComponent.dto.SecuritiesFirmsRankResult;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/securities-firms/rank")
@RequiredArgsConstructor
public class SecuritiesFirmsRankController {

    private final SecuritiesFirmsRankQueryService securitiesFirmsRankQueryService;

    @Operation(summary = "個股買賣日報表券商買賣超排名", description = "依 stockId + 固定天數取得前15買超與前15賣超券商")
    @GetMapping("/stock/{stockId}")
    public ResponseEntity<SecuritiesFirmsRankResult> getSecuritiesFirmsRank(
            @PathVariable("stockId") String stockId,
            @RequestParam("rangeDays") Integer rangeDays,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        SecuritiesFirmsRankResult result = securitiesFirmsRankQueryService.getFixedRank(stockId, rangeDays);
        return ResponseEntity.ok(result);
    }
}