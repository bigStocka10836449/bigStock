package com.bigstock.sharedComponent.service;

import java.io.Reader;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bigstock.sharedComponent.dto.ThreeInstitutionalTradingResponse;
import com.bigstock.sharedComponent.entity.StockThreeInstitutionalTrading;
import com.bigstock.sharedComponent.entity.StockThreeInstitutionalTradingId;
import com.bigstock.sharedComponent.redis.ThreeInstitutionalTradingRedisRepository;
import com.bigstock.sharedComponent.repository.StockThreeInstitutionalTradingRepository;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockThreeInstitutionalTradingService {

    private final StockThreeInstitutionalTradingRepository repository;
    private final ThreeInstitutionalTradingRedisRepository redisRepository;
    private final DataSource dataSource;
    
    @Data
    @AllArgsConstructor
    public static class SyncResult {
        private String yyyyMMdd;
        private int twseCount;
        private int tpexCount;
        private int savedCount;
    }

    public List<StockThreeInstitutionalTrading> getDateRangMarketStockData( LocalDate startDate,
            LocalDate endDate){
    	return repository.getDateRangMarketStockData(startDate, endDate);
    }
    
    @Transactional
    public List<StockThreeInstitutionalTrading> getLastThreeSingleStockData(String stockCode){
    	return repository.getLastThreeSingleStockData(stockCode);
    }
    
    @Transactional
    public void deleteByRankNoLessThanZero( LocalDate expiredDate){
    	repository.deleteByExpiredDate(expiredDate);
    }
    
    
    /**
     * 從 Redis 的 norm key 讀出（twse/tpex），然後寫入 DB（JPA saveAll）。
     * - Redis 內容不動（保留）
     * - DB 用複合 PK (trade_date, market, stock_code) 自動覆蓋更新
     */
    @Transactional
    public SyncResult syncFromRedisToDb(
            String yyyyMMdd,
            List<ThreeInstitutionalTradingResponse> twse,
            List<ThreeInstitutionalTradingResponse> tpex
    ) throws Exception {

        // 1️⃣ Merge + map
        List<StockThreeInstitutionalTrading> raw =
                new ArrayList<>(twse.size() + tpex.size());
        raw.addAll(mapToEntities(twse));
        raw.addAll(mapToEntities(tpex));

        if (raw.isEmpty()) {
            return new SyncResult(yyyyMMdd, twse.size(), tpex.size(), 0);
        }

        // 2️⃣ Deduplicate (CRITICAL)
        Map<String, StockThreeInstitutionalTrading> unique = new HashMap<>();
        for (StockThreeInstitutionalTrading e : raw) {
            String key = e.getId().getTradeDate() + "_" + e.getId().getMarket() + "_" + e.getId().getStockCode();
            unique.put(key, e); // last wins
        }
        List<StockThreeInstitutionalTrading> entities =
                new ArrayList<>(unique.values());

        // 3️⃣ Get Spring-managed connection
        Connection conn = DataSourceUtils.getConnection(dataSource);

        try {

            // --- 3.1 Create TEMP table ---
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("""
                    CREATE TEMP TABLE tmp_stock_three_insti
                    (LIKE bstock.stock_three_institutional_trading INCLUDING ALL)
                    ON COMMIT DROP
                """);
            }

            // --- 3.2 COPY ---
            BaseConnection pgConn = conn.unwrap(BaseConnection.class);
            CopyManager copyManager = new CopyManager(pgConn);

            StringBuilder sb = new StringBuilder(entities.size() * 150);

            for (StockThreeInstitutionalTrading e : entities) {
                sb.append(e.getId().getTradeDate()).append("\t")
                  .append(e.getId().getMarket()).append("\t")
                  .append(e.getId().getStockCode()).append("\t")
                  .append(nullSafeText(e.getStockName())).append("\t")
                  .append(nullSafe(e.getForeignBuy())).append("\t")
                  .append(nullSafe(e.getForeignSell())).append("\t")
                  .append(nullSafe(e.getInvestmentTrustBuy())).append("\t")
                  .append(nullSafe(e.getInvestmentTrustSell())).append("\t")
                  .append(nullSafe(e.getDealerBuy())).append("\t")
                  .append(nullSafe(e.getDealerSell()))
                  .append("\n");
            }

            String copySql = """
                COPY tmp_stock_three_insti (
                    trade_date,
                    market,
                    stock_code,
                    stock_name,
                    foreign_buy,
                    foreign_sell,
                    investment_trust_buy,
                    investment_trust_sell,
                    dealer_buy,
                    dealer_sell
                )
                FROM STDIN WITH (FORMAT csv, DELIMITER E'\\t')
            """;

            try (Reader reader = new StringReader(sb.toString())) {
                copyManager.copyIn(copySql, reader);
            }

            // --- 3.3 UPSERT ---
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("""
                    INSERT INTO bstock.stock_three_institutional_trading (
                        trade_date,
                        market,
                        stock_code,
                        stock_name,
                        foreign_buy,
                        foreign_sell,
                        investment_trust_buy,
                        investment_trust_sell,
                        dealer_buy,
                        dealer_sell,
                        created_at,
                        updated_at
                    )
                    SELECT
                        trade_date,
                        market,
                        stock_code,
                        stock_name,
                        foreign_buy,
                        foreign_sell,
                        investment_trust_buy,
                        investment_trust_sell,
                        dealer_buy,
                        dealer_sell,
                        now(),
                        now()
                    FROM tmp_stock_three_insti
                    ON CONFLICT (trade_date, market, stock_code)
                    DO UPDATE SET
                        stock_name = EXCLUDED.stock_name,
                        foreign_buy = EXCLUDED.foreign_buy,
                        foreign_sell = EXCLUDED.foreign_sell,
                        investment_trust_buy = EXCLUDED.investment_trust_buy,
                        investment_trust_sell = EXCLUDED.investment_trust_sell,
                        dealer_buy = EXCLUDED.dealer_buy,
                        dealer_sell = EXCLUDED.dealer_sell,
                        updated_at = now()
                """);
            }

        } finally {
            DataSourceUtils.releaseConnection(conn, dataSource);
        }

        return new SyncResult(
                yyyyMMdd,
                twse.size(),
                tpex.size(),
                entities.size()
        );
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
    
    @Transactional
    public SyncResult upsertFromNormList(
            String yyyyMMdd,
            List<ThreeInstitutionalTradingResponse> twse,
            List<ThreeInstitutionalTradingResponse> tpex
    ) {
        Map<StockThreeInstitutionalTradingId, StockThreeInstitutionalTrading> uniq = new LinkedHashMap<>();

        putAll(uniq, twse);
        putAll(uniq, tpex);

        if (!uniq.isEmpty()) {
            repository.saveAll(uniq.values()); // 同 PK → update；無 PK → 你一定要補 DB unique/PK
        }

        return new SyncResult(yyyyMMdd, twse.size(), tpex.size(), uniq.size());
    }
    
    private void putAll(
            Map<StockThreeInstitutionalTradingId, StockThreeInstitutionalTrading> uniq,
            List<ThreeInstitutionalTradingResponse> list
    ) {
        for (ThreeInstitutionalTradingResponse dto : list) {
            if (dto == null) continue;

            String stockCode = safe(dto.getStockCode());
            String market = safe(dto.getMarket()).toUpperCase();

            if (stockCode.isBlank() || market.isBlank()) continue;

            // dto.tradeDate = yyyy-MM-dd
            LocalDate tradeDate = LocalDate.parse(dto.getTradeDate());

            StockThreeInstitutionalTradingId id =
                    new StockThreeInstitutionalTradingId(tradeDate, market, stockCode);

            StockThreeInstitutionalTrading e = new StockThreeInstitutionalTrading(id);
            e.setStockName(safe(dto.getStockName()));
            e.setForeignBuy(dto.getForeignBuy());
            e.setForeignSell(dto.getForeignSell());
            e.setInvestmentTrustBuy(dto.getInvestmentTrustBuy());
            e.setInvestmentTrustSell(dto.getInvestmentTrustSell());
            e.setDealerBuy(dto.getDealerBuy());
            e.setDealerSell(dto.getDealerSell());

            // ✅ 同 key 覆蓋：只留最新
            uniq.put(id, e);
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }
    
    private String nullSafeText(String v) {
        if (v == null) return "";
        return v.replace("\t", " ").replace("\n", " ");
    }
    
    private String nullSafe(Long v) {
        return v == null ? "0" : String.valueOf(v);
    }
}