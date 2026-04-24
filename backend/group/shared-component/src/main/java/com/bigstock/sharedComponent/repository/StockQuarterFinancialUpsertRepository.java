package com.bigstock.sharedComponent.repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.bigstock.sharedComponent.dto.QuarterlyFinancialResponse;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class StockQuarterFinancialUpsertRepository {

    private final NamedParameterJdbcTemplate jdbc;

    private static final String UPSERT_SQL = """
        INSERT INTO bstock.stock_quarter_financial (
            stock_id, stock_name, market,
            year, quarter, period_start_month, period_end_month,
            unit,
            operating_revenue, operating_profit, non_operating_income_expense, net_profit_after_tax,
            capital_stock_end_period, earnings_per_share, net_asset_value_per_share,
            quick_ratio, current_ratio, depn,
            updated_at
        ) VALUES (
            :stock_id, :stock_name, :market,
            :year, :quarter, :period_start_month, :period_end_month,
            :unit,
            :operating_revenue, :operating_profit, :non_operating_income_expense, :net_profit_after_tax,
            :capital_stock_end_period, :earnings_per_share, :net_asset_value_per_share,
            :quick_ratio, :current_ratio, :depn,
            NOW()
        )
        ON CONFLICT (stock_id, market, year, quarter)
        DO UPDATE SET
            stock_name = EXCLUDED.stock_name,
            period_start_month = EXCLUDED.period_start_month,
            period_end_month   = EXCLUDED.period_end_month,
            unit = EXCLUDED.unit,

            operating_revenue = EXCLUDED.operating_revenue,
            operating_profit  = EXCLUDED.operating_profit,
            non_operating_income_expense = EXCLUDED.non_operating_income_expense,
            net_profit_after_tax = EXCLUDED.net_profit_after_tax,

            capital_stock_end_period  = EXCLUDED.capital_stock_end_period,
            earnings_per_share        = EXCLUDED.earnings_per_share,
            net_asset_value_per_share = EXCLUDED.net_asset_value_per_share,

            quick_ratio   = EXCLUDED.quick_ratio,
            current_ratio = EXCLUDED.current_ratio,
    		depn = EXCLUDED.depn,
            updated_at = NOW()
        """;

    public int batchUpsert(List<QuarterlyFinancialResponse> list) {
        if (list == null || list.isEmpty()) return 0;

        Map<String, QuarterlyFinancialResponse> dedup = new LinkedHashMap<>();

        for (QuarterlyFinancialResponse dto : list) {
            String key = dto.getStockId() + "|" + dto.getMarket() + "|" + dto.getYear() + "|" + dto.getQuarter();
            dedup.put(key, dto); // overwrite duplicates
        }

        List<QuarterlyFinancialResponse> cleaned = new ArrayList<>(dedup.values());
        List<MapSqlParameterSource> params = new ArrayList<>(cleaned.size());
        for (QuarterlyFinancialResponse dto : cleaned) {
            if (dto == null) continue;

            String stockId = safe(dto.getStockId());
            String market = safe(dto.getMarket()).toUpperCase();
            if (stockId.isBlank() || market.isBlank()) continue;

            MapSqlParameterSource p = new MapSqlParameterSource();
            p.addValue("stock_id", stockId);
            p.addValue("stock_name", safe(dto.getStockName()));
            p.addValue("market", market);

            p.addValue("year", dto.getYear());
            p.addValue("quarter", dto.getQuarter());
            p.addValue("period_start_month", dto.getPeriodStartMonth());
            p.addValue("period_end_month", dto.getPeriodEndMonth());

            p.addValue("unit", safe(dto.getUnit()).isBlank() ? "TWD" : safe(dto.getUnit()).toUpperCase());

            p.addValue("operating_revenue", dto.getOperatingRevenue());
            p.addValue("operating_profit", dto.getOperatingProfit());
            p.addValue("non_operating_income_expense", dto.getNonOperatingIncomeExpense());
            p.addValue("net_profit_after_tax", dto.getNetProfitAfterTax());

            p.addValue("capital_stock_end_period", dto.getCapitalStockEndPeriod());
            p.addValue("earnings_per_share", round2(dto.getEarningsPerShare()));
            p.addValue("net_asset_value_per_share", round2(dto.getNetAssetValuePerShare()));

            p.addValue("quick_ratio", round2(dto.getQuickRatio()));
            p.addValue("current_ratio", round2(dto.getCurrentRatio()));
            
            p.addValue("depn", round2(dto.getDepn()));
            

            params.add(p);
        }

        if (params.isEmpty()) return 0;

        int[] res = jdbc.batchUpdate(UPSERT_SQL, params.toArray(new MapSqlParameterSource[0]));
        return res.length;
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private static Double round2(Double v) {
        if (v == null) return null;
        return Math.round(v * 100.0) / 100.0;
    }
}
