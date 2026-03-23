package com.bigstock.biz.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bigstock.sharedComponent.client.TpexQuarterlyFinancialClient;
import com.bigstock.sharedComponent.client.TwseQuarterlyFinancialClient;
import com.bigstock.sharedComponent.dto.QuarterlyFinancialVo;
import com.bigstock.sharedComponent.service.QuarterlyFinancialRedisService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/financial/quarterly")
public class QuarterlyFinancialController {

    private final TpexQuarterlyFinancialClient tpexClient;
    private final TwseQuarterlyFinancialClient twseClient;
    private final QuarterlyFinancialRedisService redisService;

    @PostMapping
    public void fetchToRedis(
            @RequestParam int year,
            @RequestParam int quarter,
            @RequestParam String market
    ) {
        List<QuarterlyFinancialVo> list =
                market.equalsIgnoreCase("TWSE")
                        ? twseClient.fetch(year, quarter)
                        : tpexClient.fetch(year, quarter);

        redisService.write(year, quarter, market.toUpperCase(), list);
    }

    @GetMapping
    public List<QuarterlyFinancialVo> readFromRedis(
            @RequestParam int year,
            @RequestParam int quarter,
            @RequestParam String market
    ) {
        return redisService.read(year, quarter, market.toUpperCase());
    }
}
