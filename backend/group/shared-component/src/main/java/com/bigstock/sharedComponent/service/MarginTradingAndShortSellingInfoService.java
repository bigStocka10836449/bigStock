package com.bigstock.sharedComponent.service;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bigstock.sharedComponent.entity.MarginTradingAndShortSellingInfo;
import com.bigstock.sharedComponent.repository.MarginTradingAndShortSellingInfoRepository;

@Service
public class MarginTradingAndShortSellingInfoService {

    private final MarginTradingAndShortSellingInfoRepository repository;

    @Autowired
    public MarginTradingAndShortSellingInfoService(MarginTradingAndShortSellingInfoRepository repository) {
        this.repository = repository;
    }

    public List<MarginTradingAndShortSellingInfo> getAllRecords() {
        return repository.findAll();
    }

    public Optional<MarginTradingAndShortSellingInfo> getRecordById(MarginTradingAndShortSellingInfo.MarginTradingAndShortSellingInfoId id) {
        return repository.findById(id);
    }

    public MarginTradingAndShortSellingInfo saveRecord(MarginTradingAndShortSellingInfo record) {
        return repository.save(record);
    }

    public void deleteRecordById(MarginTradingAndShortSellingInfo.MarginTradingAndShortSellingInfoId id) {
        repository.deleteById(id);
    }
    
    public List<MarginTradingAndShortSellingInfo> saveAll(List<MarginTradingAndShortSellingInfo> marginTradingAndShortSellingInfos){
    	return repository.saveAll(marginTradingAndShortSellingInfos);
    }
    
    public List<MarginTradingAndShortSellingInfo> findMarginTradingAndShortSellingInfoByDateRange(String stockCode, Date firstDate, Date secondDate){
    	return repository.findMarginTradingAndShortSellingInfoByDateRange(stockCode, firstDate, secondDate);
    }
    
    public void bulkUpsertMarginTradingAndShortSellingInfo(
            List<MarginTradingAndShortSellingInfo> data
    ) throws Exception {

        if (data == null || data.isEmpty()) {
            return;
        }

        try (Connection connection = dataSource.getConnection()) {

            connection.setAutoCommit(false);

            CopyManager copyManager =
                    new CopyManager(connection.unwrap(BaseConnection.class));

            // 1️⃣ create temp table
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("""
                    CREATE TEMP TABLE tmp_margin_trading_and_short_selling_info
                    (LIKE bstock.margin_trading_and_short_selling_info INCLUDING ALL)
                    ON COMMIT DROP
                """);
            }

            // 2️⃣ COPY
            String copySql = """
                COPY tmp_margin_trading_and_short_selling_info (
                    trading_day,
                    stock_code,
                    margin_purchase_balance_previous_day,
                    margin_purchase,
                    margin_sales,
                    cash_redemption,
                    margin_purchase_balance,
                    margin_purchase_quota,
                    short_sale_balance_previous_day,
                    short_sale,
                    short_convering,
                    stock_redemption,
                    short_sale_balance,
                    short_sale_quota,
                    offsetting
                )
                FROM STDIN WITH (FORMAT csv)
            """;

            PipedOutputStream pos = new PipedOutputStream();
            PipedInputStream pis = new PipedInputStream(pos, 65536);

            ExecutorService executor = Executors.newSingleThreadExecutor();

            executor.submit(() -> {
                try (BufferedWriter writer =
                             new BufferedWriter(new OutputStreamWriter(pos))) {

                    for (MarginTradingAndShortSellingInfo item : data) {
                        writer.write(buildCsvLine(item));
                        writer.newLine();
                    }
                }
                return null;
            });

            copyManager.copyIn(copySql, pis);
            executor.shutdown();

            // 3️⃣ delete conflict rows
            try (PreparedStatement ps = connection.prepareStatement("""
                DELETE FROM bstock.margin_trading_and_short_selling_info t
                USING tmp_margin_trading_and_short_selling_info tmp
                WHERE t.trading_day = tmp.trading_day
                AND t.stock_code = tmp.stock_code
            """)) {
                ps.executeUpdate();
            }

            // 4️⃣ insert
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("""
                    INSERT INTO bstock.margin_trading_and_short_selling_info
                    SELECT * FROM tmp_margin_trading_and_short_selling_info
                """);
            }

            connection.commit();
        }
    }
}
