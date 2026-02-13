package com.bigstock.sharedComponent.repository;

import com.bigstock.sharedComponent.dto.QuarterlyFinancialResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class StockQuarterFinancialQueryRepository {

    private final NamedParameterJdbcTemplate jdbc;

    private static final String BASE_SELECT = """
        SELECT
            stock_id, stock_name, market,
            year, quarter, period_start_month, period_end_month,
            unit,
            operating_revenue, operating_profit, non_operating_income_expense, net_profit_after_tax,
            capital_stock_end_period, earnings_per_share, net_asset_value_per_share,
            quick_ratio, current_ratio, depn
        FROM bstock.stock_quarter_financial
        """;

    /**
     * 最新 N 筆（不帶 market）
     */
    public List<QuarterlyFinancialResponse> findLatestNByStockId(String stockId, int limit) {
        String sid = safe(stockId);
        if (sid.isBlank() || limit <= 0) return Collections.emptyList();

        String sql = BASE_SELECT + """
            WHERE stock_id = :stock_id
            ORDER BY year DESC, quarter DESC
            LIMIT :limit
            """;

        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("stock_id", sid)
                .addValue("limit", limit);

        return jdbc.query(sql, p, (rs, i) -> map(rs));
    }

    /**
     * 區間查：以 (year*4+quarter) 做 BETWEEN（不帶 market）
     */
    public List<QuarterlyFinancialResponse> findRangeByStockId(
            String stockId,
            int startYear,
            int startQuarter,
            int endYear,
            int endQuarter
    ) {
        String sid = safe(stockId);
        if (sid.isBlank()) return Collections.emptyList();

        int sIdx = startYear * 4 + startQuarter;
        int eIdx = endYear * 4 + endQuarter;

        String sql = BASE_SELECT + """
            WHERE stock_id = :stock_id
              AND (year * 4 + quarter) BETWEEN :sIdx AND :eIdx
            ORDER BY year ASC, quarter ASC
            """;

        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("stock_id", sid)
                .addValue("sIdx", sIdx)
                .addValue("eIdx", eIdx);

        return jdbc.query(sql, p, (rs, i) -> map(rs));
    }

    private static QuarterlyFinancialResponse map(ResultSet rs) throws SQLException {
        QuarterlyFinancialResponse d = new QuarterlyFinancialResponse();
        d.setStockId(rs.getString("stock_id"));
        d.setStockName(rs.getString("stock_name"));
        d.setMarket(rs.getString("market"));

        d.setYear(rs.getInt("year"));
        d.setQuarter(rs.getInt("quarter"));
        d.setPeriodStartMonth(rs.getInt("period_start_month"));
        d.setPeriodEndMonth(rs.getInt("period_end_month"));

        d.setUnit(rs.getString("unit"));

        // bigint / int8：用 getObject 轉 Long OK（也可改 rs.getLong + wasNull）
        d.setOperatingRevenue((Long) rs.getObject("operating_revenue"));
        d.setOperatingProfit((Long) rs.getObject("operating_profit"));
        d.setNonOperatingIncomeExpense((Long) rs.getObject("non_operating_income_expense"));
        d.setNetProfitAfterTax((Long) rs.getObject("net_profit_after_tax"));
        d.setCapitalStockEndPeriod((Long) rs.getObject("capital_stock_end_period"));

        // ✅ numeric(18,2)：Postgres driver 會回 BigDecimal，必須這樣轉
        d.setEarningsPerShare(toDouble(rs.getBigDecimal("earnings_per_share")));
        d.setNetAssetValuePerShare(toDouble(rs.getBigDecimal("net_asset_value_per_share")));
        d.setQuickRatio(toDouble(rs.getBigDecimal("quick_ratio")));
        d.setCurrentRatio(toDouble(rs.getBigDecimal("current_ratio")));
        d.setDepn(toDouble(rs.getBigDecimal("depn")));

        return d;
    }

    private static Double toDouble(BigDecimal bd) {
        return bd == null ? null : bd.doubleValue();
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
