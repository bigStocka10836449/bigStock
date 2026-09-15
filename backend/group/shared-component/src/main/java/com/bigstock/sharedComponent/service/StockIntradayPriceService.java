package com.bigstock.sharedComponent.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bigstock.sharedComponent.entity.StockIntradayPrice;
import com.bigstock.sharedComponent.repository.StockIntradayPriceRepository;

@Service
public class StockIntradayPriceService {

    private final StockIntradayPriceRepository repository;

    public StockIntradayPriceService(
            StockIntradayPriceRepository repository) {

        this.repository = repository;
    }
    
    @Transactional(readOnly = true)
    public List<StockIntradayPrice> getTop60ByPeriod(String period){
    	 List<StockIntradayPrice> prices = repository.findTop60ByPeriod(period);
    	 return prices;
    }
    
    @Transactional(readOnly = true)
    public List<StockIntradayPrice> getLatest60Prices(
            String stockCode,
            String period) {

        List<StockIntradayPrice> prices =
                repository
                        .findTop60ByStockCodeAndPeriodOrderByTradingTimeDesc(
                                stockCode,
                                period
                        );

        Collections.reverse(prices);

        return prices;
    }

    @Transactional(readOnly = true)
    public List<StockIntradayPrice> getPrices(
            String stockCode,
            String period) {

        return repository
                .findByStockCodeAndPeriodOrderByTradingTimeAsc(
                        stockCode,
                        period
                );
    }

    @Transactional(readOnly = true)
    public StockIntradayPrice getLatestPrice(
            String stockCode,
            String period) {

        return repository
                .findFirstByStockCodeAndPeriodOrderByTradingTimeDesc(
                        stockCode,
                        period
                )
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public StockIntradayPrice getOldestPrice(
            String stockCode,
            String period) {

        return repository
                .findFirstByStockCodeAndPeriodOrderByTradingTimeAsc(
                        stockCode,
                        period
                )
                .orElse(null);
    }

    @Transactional
    public List<StockIntradayPrice> saveAll(
            List<StockIntradayPrice> prices) {

        if (prices == null || prices.isEmpty()) {
            return Collections.emptyList();
        }

        return repository.saveAll(prices);
    }

    @Transactional
    public int deleteBefore(
            String period,
            LocalDateTime cutoffTime) {

        return repository.deleteBefore(
                period,
                cutoffTime
        );
    }

    @Transactional
    public int cleanupExpiredData() {

        ZoneId zoneId =
                ZoneId.of("Asia/Taipei");

        LocalDateTime now =
                LocalDateTime.now(zoneId);

        int deleted = 0;

        deleted += repository.deleteBefore(
                "5m",
                now.minusDays(60)
        );

        deleted += repository.deleteBefore(
                "60m",
                now.minusMonths(4)
        );

        return deleted;
    }
}