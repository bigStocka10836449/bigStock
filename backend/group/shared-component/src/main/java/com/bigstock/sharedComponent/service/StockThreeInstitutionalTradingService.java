package com.bigstock.sharedComponent.service;

import com.bigstock.sharedComponent.dto.ThreeInstitutionalTradingResponse;
import com.bigstock.sharedComponent.entity.StockThreeInstitutionalTrading;
import com.bigstock.sharedComponent.entity.StockThreeInstitutionalTradingId;
import com.bigstock.sharedComponent.redis.ThreeInstitutionalTradingRedisRepository;
import com.bigstock.sharedComponent.repository.StockThreeInstitutionalTradingRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockThreeInstitutionalTradingService {

    private final StockThreeInstitutionalTradingRepository repository;
    private final ThreeInstitutionalTradingRedisRepository redisRepository;

    @Data
    @AllArgsConstructor
    public static class SyncResult {
        private String yyyyMMdd;
        private int twseCount;
        private int tpexCount;
        private int savedCount;
    }

    /**
     * 從 Redis 的 norm key 讀出（twse/tpex），然後寫入 DB（JPA saveAll）。
     * - Redis 內容不動（保留）
     * - DB 用複合 PK (trade_date, market, stock_code) 自動覆蓋更新
     */
    @Transactional
    public SyncResult syncFromRedisToDb(String yyyyMMdd) {
        List<ThreeInstitutionalTradingResponse> twse = redisRepository.getNormList("twse", yyyyMMdd);
        List<ThreeInstitutionalTradingResponse> tpex = redisRepository.getNormList("tpex", yyyyMMdd);

        List<StockThreeInstitutionalTrading> entities = new ArrayList<>(twse.size() + tpex.size());
        entities.addAll(mapToEntities(twse));
        entities.addAll(mapToEntities(tpex));

        if (!entities.isEmpty()) {
            repository.saveAll(entities);
        }

        return new SyncResult(yyyyMMdd, twse.size(), tpex.size(), entities.size());
    }

    private List<StockThreeInstitutionalTrading> mapToEntities(List<ThreeInstitutionalTradingResponse> list) {
        List<StockThreeInstitutionalTrading> out = new ArrayList<>(list.size());
        for (ThreeInstitutionalTradingResponse dto : list) {
            if (dto == null) continue;

            String stockCode = safe(dto.getStockCode());
            String market = safe(dto.getMarket()).toUpperCase();

            if (stockCode.isBlank() || market.isBlank()) continue;

            // dto.tradeDate = yyyy-MM-dd
            LocalDate tradeDate = LocalDate.parse(dto.getTradeDate());

            StockThreeInstitutionalTradingId id = new StockThreeInstitutionalTradingId(tradeDate, market, stockCode);
            StockThreeInstitutionalTrading e = new StockThreeInstitutionalTrading(id);

            e.setStockName(safe(dto.getStockName()));
            e.setForeignBuy(dto.getForeignBuy());
            e.setForeignSell(dto.getForeignSell());
            e.setInvestmentTrustBuy(dto.getInvestmentTrustBuy());
            e.setInvestmentTrustSell(dto.getInvestmentTrustSell());
            e.setDealerBuy(dto.getDealerBuy());
            e.setDealerSell(dto.getDealerSell());

            out.add(e);
        }
        return out;
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }
}