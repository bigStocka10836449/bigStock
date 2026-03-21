package com.bigstock.biz.service;

import com.bigstock.sharedComponent.dto.QuarterlyFinancialResponse;
import com.bigstock.sharedComponent.dto.QuarterlyFinancialVo;
import com.bigstock.sharedComponent.service.StockQuarterFinancialService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuarterlyFinancialRedisService {

    private final StringRedisTemplate redis;

    // ✅ 這個用 Spring 注入（不要 new ObjectMapper，避免跟全域設定不一致）
    private final ObjectMapper objectMapper;

    // ✅ 新增：shared-component 的 DB upsert service
    private final StockQuarterFinancialService stockQuarterFinancialService;

    private static String key(int year, int quarter, String market) {
        return "FIN:QTR:" + year + "Q" + quarter + ":" + market;
    }

    public void write(int year, int quarter, String market, List<QuarterlyFinancialVo> list) {
        try {
            // 1) 寫 Redis（保留你原本行為）
            String json = objectMapper.writeValueAsString(list);
            String k = key(year, quarter, market);
            redis.opsForValue().set(k, json);
            redis.expire(k, Duration.ofDays(90));

            // 2) ✅ 同步寫 DB（不從 Redis 回寫，直接用當下 list）
            //    - market 以參數為準（避免 vo 內有人塞錯）
            //    - Double 兩位小數：shared-component upsert 內會 round2（你也可以在這裡先 round）
            if (list != null && !list.isEmpty()) {
                List<QuarterlyFinancialResponse> dtoList = list.stream()
                        .map(v -> toResponse(year, quarter, market, v))
                        .collect(Collectors.toList());

                stockQuarterFinancialService.upsertFromNormList(dtoList);
            }

        } catch (Exception e) {
            throw new RuntimeException("write redis failed", e);
        }
    }

    public List<QuarterlyFinancialVo> read(int year, int quarter, String market) {
        try {
            String json = redis.opsForValue().get(key(year, quarter, market));
            if (json == null || json.isBlank()) return Collections.emptyList();
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            throw new RuntimeException("read redis failed", e);
        }
    }

    private static QuarterlyFinancialResponse toResponse(int year, int quarter, String market, QuarterlyFinancialVo v) {
        QuarterlyFinancialResponse d = new QuarterlyFinancialResponse();
        d.setStockId(v.getStockId());
        d.setStockName(v.getStockName());

        // ✅ 以 write() 的 market 參數為準
        d.setMarket(market == null ? null : market.trim().toUpperCase());

        // ✅ 以 write() 的 year/quarter 為準（你目前 redis key 已經用這兩個）
        d.setYear(year);
        d.setQuarter(quarter);

        d.setPeriodStartMonth(v.getPeriodStartMonth());
        d.setPeriodEndMonth(v.getPeriodEndMonth());
        d.setUnit(v.getUnit() == null ? "TWD" : v.getUnit());

        d.setOperatingRevenue(v.getOperatingRevenue());
        d.setOperatingProfit(v.getOperatingProfit());
        d.setNonOperatingIncomeExpense(v.getNonOperatingIncomeExpense());
        d.setNetProfitAfterTax(v.getNetProfitAfterTax());

        d.setCapitalStockEndPeriod(v.getCapitalStockEndPeriod());
        d.setEarningsPerShare(v.getEarningsPerShare());
        d.setNetAssetValuePerShare(v.getNetAssetValuePerShare());

        d.setQuickRatio(v.getQuickRatio());
        d.setCurrentRatio(v.getCurrentRatio());
        d.setDepn(v.getDepn());
        return d;
    }
}
