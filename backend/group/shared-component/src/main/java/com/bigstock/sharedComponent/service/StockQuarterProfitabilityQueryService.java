package com.bigstock.sharedComponent.service;

import com.bigstock.sharedComponent.dto.QuarterlyProfitabilityRawResponse;
import com.bigstock.sharedComponent.repository.StockQuarterProfitabilityQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StockQuarterProfitabilityQueryService {

    private final StockQuarterProfitabilityQueryRepository repository;

    public List<QuarterlyProfitabilityRawResponse> queryByStockLatestN(String stockId, String market, int limit) {
        return repository.queryByStockLatestN(stockId, market, limit);
    }
}
