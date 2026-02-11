package com.bigstock.sharedComponent.repository;

import com.bigstock.sharedComponent.dto.QuarterlyProfitabilityRawResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class StockQuarterProfitabilityQueryRepository {

    private final NamedParameterJdbcTemplate jdbc;

    private static final String SQL_LATEST_N = """
        SELECT
            stock_id, stock_name, market,
            year, quarter,
            operating_profit,
            non_operating_income_expense,
            earnings_per_share
        FROM bstock.stock_quarter_financial
        WHERE stock_id = :stock_id
          AND (:market IS NULL OR market = :market)
        ORDER BY year DESC, quarter DESC
        LIMIT :limit
        """;

    public List<QuarterlyProfitabilityRawResponse> queryByStockLatestN(
            String stockId,
            String market,
            int limit
    ) {
        String sid = safe(stockId);
        if (sid.isBlank() || limit <= 0) return Collections.emptyList();

        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("stock_id", sid)
                .addValue(
                        "market",
                        (market == null || market.trim().isBlank())
                                ? null
                                : market.trim().toUpperCase()
                )
                .addValue("limit", limit);

        return jdbc.query(SQL_LATEST_N, p, (rs, i) -> {
            QuarterlyProfitabilityRawResponse d = new QuarterlyProfitabilityRawResponse();
            d.setStockId(rs.getString("stock_id"));
            d.setStockName(rs.getString("stock_name"));
            d.setMarket(rs.getString("market"));
            d.setYear(rs.getInt("year"));
            d.setQuarter(rs.getInt("quarter"));

            d.setOperatingProfit((Long) rs.getObject("operating_profit"));
            d.setNonOperatingIncomeExpense((Long) rs.getObject("non_operating_income_expense"));

            // ✅ numeric / decimal → BigDecimal → Double
            BigDecimal epsBd = rs.getBigDecimal("earnings_per_share");
            d.setEarningsPerShare(epsBd == null ? null : epsBd.doubleValue());

            return d;
        });
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
