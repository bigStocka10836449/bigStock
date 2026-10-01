package com.bigstock.sharedComponent.service;

import java.io.Reader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.sql.DataSource;

import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bigstock.sharedComponent.entity.StockIntradayPrice;
import com.bigstock.sharedComponent.repository.StockIntradayPriceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StockIntradayPriceService {

    private final StockIntradayPriceRepository repository;
    
    private final DataSource dataSource;
    
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
    public void bulkUpsertIntradayPrices(
            List<StockIntradayPrice> data) throws Exception {

        if (data == null || data.isEmpty()) {
            return;
        }

        try (Connection connection = dataSource.getConnection()) {

            connection.setAutoCommit(false);

            try {

                CopyManager copyManager =
                        new CopyManager(
                                connection.unwrap(
                                        BaseConnection.class
                                )
                        );

                // =====================================================
                // TEMP TABLE
                // =====================================================

                try (Statement stmt =
                             connection.createStatement()) {

                    stmt.execute("""
                        CREATE TEMP TABLE tmp_stock_intraday_price
                        (
                            LIKE bstock.stock_intraday_price
                            INCLUDING ALL
                        )
                        ON COMMIT DROP
                    """);
                }


                // =====================================================
                // STREAM DATA -> COPY
                // =====================================================

                Reader reader = new Reader() {

                    private final Iterator<StockIntradayPrice> it =
                            data.iterator();

                    private String currentLine = null;

                    private int index = 0;


                    @Override
                    public int read(
                            char[] cbuf,
                            int off,
                            int len) {

                        try {

                            int count = 0;

                            while (count < len) {

                                if (currentLine == null
                                        || index >= currentLine.length()) {

                                    if (!it.hasNext()) {
                                        break;
                                    }

                                    currentLine =
                                            buildIntradayCsvLine(
                                                    it.next()
                                            )
                                            + "\n";

                                    index = 0;
                                }

                                cbuf[off + count] =
                                        currentLine.charAt(index++);

                                count++;
                            }

                            return count == 0
                                    ? -1
                                    : count;

                        } catch (Exception e) {

                            throw new RuntimeException(e);
                        }
                    }


                    @Override
                    public void close() {
                    }
                };


                // =====================================================
                // COPY -> TEMP TABLE
                // =====================================================

                copyManager.copyIn("""
                    COPY tmp_stock_intraday_price (
                        stock_code,
                        trading_time,
                        period,

                        opening_price,
                        closing_price,
                        high_price,
                        low_price,
                        trading_volume,

                        five_ma,
                        ten_ma,
                        twenty_ma,
                        sixty_ma,

                        line_rsv_value,
                        line_k_value,
                        line_d_value
                    )
                    FROM STDIN
                    WITH (
                        FORMAT csv,
                        NULL ''
                    )
                """, reader);


                // =====================================================
                // DELETE EXISTING MATCHING ROWS
                //
                // unique:
                // stock_code + trading_time + period
                // =====================================================

                try (PreparedStatement ps =
                             connection.prepareStatement("""
                        DELETE FROM bstock.stock_intraday_price t
                        USING tmp_stock_intraday_price tmp
                        WHERE t.stock_code = tmp.stock_code
                          AND t.trading_time = tmp.trading_time
                          AND t.period = tmp.period
                    """)) {

                    ps.executeUpdate();
                }


                // =====================================================
                // INSERT TEMP -> REAL TABLE
                // =====================================================

                try (Statement stmt =
                             connection.createStatement()) {

                    stmt.execute("""
                        INSERT INTO bstock.stock_intraday_price (
                            stock_code,
                            trading_time,
                            period,

                            opening_price,
                            closing_price,
                            high_price,
                            low_price,
                            trading_volume,

                            five_ma,
                            ten_ma,
                            twenty_ma,
                            sixty_ma,

                            line_rsv_value,
                            line_k_value,
                            line_d_value
                        )
                        SELECT
                            stock_code,
                            trading_time,
                            period,

                            opening_price,
                            closing_price,
                            high_price,
                            low_price,
                            trading_volume,

                            five_ma,
                            ten_ma,
                            twenty_ma,
                            sixty_ma,

                            line_rsv_value,
                            line_k_value,
                            line_d_value
                        FROM tmp_stock_intraday_price
                    """);
                }


                connection.commit();

            } catch (Exception e) {

                connection.rollback();

                throw e;
            }
        }
    }
    
    private String buildIntradayCsvLine(
            StockIntradayPrice data) {

        StringBuilder sb =
                new StringBuilder(256);

        appendCsv(
                sb,
                data.getStockCode()
        );

        sb.append(',');

        appendCsv(
                sb,
                data.getTradingTime() == null
                        ? null
                        : data.getTradingTime().toString()
        );

        sb.append(',');

        appendCsv(
                sb,
                data.getPeriod()
        );

        sb.append(',');

        appendCsv(
                sb,
                data.getOpeningPrice()
        );

        sb.append(',');

        appendCsv(
                sb,
                data.getClosingPrice()
        );

        sb.append(',');

        appendCsv(
                sb,
                data.getHighPrice()
        );

        sb.append(',');

        appendCsv(
                sb,
                data.getLowPrice()
        );

        sb.append(',');

        appendCsv(
                sb,
                data.getTradingVolume()
        );

        sb.append(',');

        appendCsv(
                sb,
                data.getFiveMa()
        );

        sb.append(',');

        appendCsv(
                sb,
                data.getTenMa()
        );

        sb.append(',');

        appendCsv(
                sb,
                data.getTwentyMa()
        );

        sb.append(',');

        appendCsv(
                sb,
                data.getSixtyMa()
        );

        sb.append(',');

        appendCsv(
                sb,
                data.getLineRsvValue()
        );

        sb.append(',');

        appendCsv(
                sb,
                data.getLineKValue()
        );

        sb.append(',');

        appendCsv(
                sb,
                data.getLineDValue()
        );

        return sb.toString();
    }
    private void appendCsv(
            StringBuilder sb,
            Object value) {

        if (value == null) {

            // PostgreSQL COPY NULL ''
            sb.append("");

            return;
        }

        String text =
                String.valueOf(value);

        boolean needQuote =
                text.contains(",")
                || text.contains("\"")
                || text.contains("\n")
                || text.contains("\r");

        if (!needQuote) {

            sb.append(text);

            return;
        }

        sb.append('"');

        sb.append(
                text.replace(
                        "\"",
                        "\"\""
                )
        );

        sb.append('"');
    }
}