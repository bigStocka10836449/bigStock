package com.bigstock.sharedComponent.service;

import com.bigstock.sharedComponent.dto.QuarterlyFinancialResponse;
import com.bigstock.sharedComponent.repository.StockQuarterFinancialQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StockQuarterFinancialQueryService {

    private final StockQuarterFinancialQueryRepository queryRepository;

    public List<QuarterlyFinancialResponse> queryByStockLatestN(String stockId, int limit) {
        if (stockId == null || stockId.trim().isBlank() || limit <= 0) {
            return Collections.emptyList();
        }
        return queryRepository.findLatestNByStockId(stockId.trim(), limit);
    }

    public List<QuarterlyFinancialResponse> queryByStockRange(
            String stockId,
            int startYear,
            int startQuarter,
            int endYear,
            int endQuarter
    ) {
        if (stockId == null || stockId.trim().isBlank()) {
            return Collections.emptyList();
        }
        // 你也可以加上 quarter 1~4 的簡單檢查，但我先不改動你既有行為
        return queryRepository.findRangeByStockId(
                stockId.trim(),
                startYear, startQuarter,
                endYear, endQuarter
        );
    }
}
