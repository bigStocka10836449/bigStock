package com.bigstock.biz.service;

import com.bigstock.sharedComponent.dto.QuarterlyFinancialResponse;
import com.bigstock.sharedComponent.dto.QuarterlyFinancialVo;
import com.bigstock.sharedComponent.service.StockQuarterFinancialQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuarterlyFinancialDbService {

    private final StockQuarterFinancialQueryService queryService;

    public List<QuarterlyFinancialVo> queryLatestN(String stockId, int limit) {
        if (stockId == null || stockId.trim().isBlank() || limit <= 0) return Collections.emptyList();
        List<QuarterlyFinancialResponse> list = queryService.queryByStockLatestN(stockId.trim(), limit);
        return list.stream().map(QuarterlyFinancialDbService::toVo).collect(Collectors.toList());
    }

    public List<QuarterlyFinancialVo> queryRange(
            String stockId,
            int startYear,
            int startQuarter,
            int endYear,
            int endQuarter
    ) {
        if (stockId == null || stockId.trim().isBlank()) return Collections.emptyList();
        List<QuarterlyFinancialResponse> list = queryService.queryByStockRange(
                stockId.trim(),
                startYear, startQuarter,
                endYear, endQuarter
        );
        return list.stream().map(QuarterlyFinancialDbService::toVo).collect(Collectors.toList());
    }

    private static QuarterlyFinancialVo toVo(QuarterlyFinancialResponse d) {
        QuarterlyFinancialVo v = new QuarterlyFinancialVo();
        v.setStockId(d.getStockId());
        v.setStockName(d.getStockName());
        v.setMarket(d.getMarket());

        v.setYear(d.getYear());
        v.setQuarter(d.getQuarter());
        v.setPeriodStartMonth(d.getPeriodStartMonth());
        v.setPeriodEndMonth(d.getPeriodEndMonth());

        v.setUnit(d.getUnit());

        v.setOperatingRevenue(d.getOperatingRevenue());
        v.setOperatingProfit(d.getOperatingProfit());
        v.setNonOperatingIncomeExpense(d.getNonOperatingIncomeExpense());
        v.setNetProfitAfterTax(d.getNetProfitAfterTax());

        v.setCapitalStockEndPeriod(d.getCapitalStockEndPeriod());
        v.setEarningsPerShare(d.getEarningsPerShare());
        v.setNetAssetValuePerShare(d.getNetAssetValuePerShare());

        v.setQuickRatio(d.getQuickRatio());
        v.setCurrentRatio(d.getCurrentRatio());
        v.setDepn(d.getDepn());
        return v;
    }
}
