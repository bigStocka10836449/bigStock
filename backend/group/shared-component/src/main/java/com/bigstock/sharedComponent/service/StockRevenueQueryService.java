package com.bigstock.sharedComponent.service;

import com.bigstock.sharedComponent.dto.StockRevenueResponse;
import com.bigstock.sharedComponent.entity.StockRevenue;
import com.bigstock.sharedComponent.repository.StockRevenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StockRevenueQueryService {

    private final StockRevenueRepository stockRevenueRepository;

    private static final DateTimeFormatter YM = DateTimeFormatter.ofPattern("yyyy-MM");

    /**
     * 查 DB：單一個股的月營收區間（start/end 都是「月份第一天」：yyyy-MM-01）
     * market 可為 null（依你 repository 實作，null 表示全 market）
     */
    public List<StockRevenueResponse> queryStockMonthlyRevenue(
            String stockId,
            String market,
            LocalDate startMonthInclusive,
            LocalDate endMonthInclusive
    ) {
        if (stockId == null || stockId.trim().isEmpty()) {
            return new ArrayList<>();
        }
        if (startMonthInclusive == null || endMonthInclusive == null) {
            return new ArrayList<>();
        }

        List<StockRevenue> rows = stockRevenueRepository.findByStockIdAndMonthRange(
                stockId.trim(),
                market,
                startMonthInclusive,
                endMonthInclusive
        );

        List<StockRevenueResponse> out = new ArrayList<>();
        for (StockRevenue e : rows) {
            StockRevenueResponse r = new StockRevenueResponse();
            r.setStockId(e.getId().getStockId());
            r.setStockName(e.getStockName());
            r.setRevenue(e.getRevenue());
            r.setMarket(e.getId().getMarket());
            // DB: revenueMonth = yyyy-MM-01 -> API 回 yyyy-MM
            r.setDate(e.getId().getRevenueMonth().format(YM));
            out.add(r);
        }
        return out;
    }
}
