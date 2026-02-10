package com.bigstock.biz.service;

import com.bigstock.biz.dto.QuarterlyProfitabilityResponse;
import com.bigstock.sharedComponent.dto.QuarterlyProfitabilityRawResponse;
import com.bigstock.sharedComponent.service.StockQuarterProfitabilityQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QuarterlyProfitabilityService {

    private final StockQuarterProfitabilityQueryService queryService;

    /** 固定最多 4 個會計年 = 16 季 */
    private static final int MAX_QUARTERS = 16;

    public List<QuarterlyProfitabilityResponse> queryLatest4Years(String stockId, String market) {
        List<QuarterlyProfitabilityRawResponse> raws =
                queryService.queryByStockLatestN(stockId, market, MAX_QUARTERS);

        if (raws == null || raws.isEmpty()) return List.of();

        // 前端通常希望舊 → 新
        raws.sort(Comparator
                .comparingInt(QuarterlyProfitabilityRawResponse::getYear)
                .thenComparingInt(QuarterlyProfitabilityRawResponse::getQuarter));

        List<QuarterlyProfitabilityResponse> out = new ArrayList<>(raws.size());
        for (QuarterlyProfitabilityRawResponse r : raws) {
            Long op = r.getOperatingProfit();
            Long noi = r.getNonOperatingIncomeExpense();

            Long pretaxProfit = safeLong(op) + safeLong(noi);

            QuarterlyProfitabilityResponse dto = new QuarterlyProfitabilityResponse();
            dto.setStockId(r.getStockId());
            dto.setStockName(r.getStockName());
            dto.setYear(r.getYear());
            dto.setQuarter(r.getQuarter());

            dto.setPretaxProfit(pretaxProfit);
            dto.setNonOperatingIncomeExpense(noi);

            // 業外佔淨利比（以稅前純益為分母）
            dto.setNonOperatingRatio(calcRatioPercent(noi, pretaxProfit));

            dto.setEarningsPerShare(r.getEarningsPerShare());

            out.add(dto);
        }
        return out;
    }

    private static long safeLong(Long v) {
        return v == null ? 0L : v;
    }

    private static Double calcRatioPercent(Long numerator, Long denominator) {
        if (numerator == null || denominator == null) return null;
        if (denominator == 0L) return null;
        double v = (numerator.doubleValue() / denominator.doubleValue()) * 100.0;
        return round2(v);
    }

    private static Double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
