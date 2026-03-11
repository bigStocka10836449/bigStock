package com.bigstock.biz.controller;

import com.bigstock.biz.service.QuarterlyFinancialRedisService;
import com.bigstock.sharedComponent.client.TpexQuarterlyFinancialClient;
import com.bigstock.sharedComponent.client.TwseQuarterlyFinancialClient;
import com.bigstock.sharedComponent.dto.QuarterlyFinancialVo;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
