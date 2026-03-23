package com.bigstock.sharedComponent.service;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.sql.DataSource;

import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.bigstock.sharedComponent.entity.MarginTradingAndShortSellingInfo;
import com.bigstock.sharedComponent.entity.StockDayPrice;
import com.bigstock.sharedComponent.repository.MarginTradingAndShortSellingInfoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MarginTradingAndShortSellingInfoService {

	private final MarginTradingAndShortSellingInfoRepository repository;

	private final JdbcTemplate jdbcTemplate;

	private final DataSource dataSource;

	private static final DateTimeFormatter PG_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	public List<MarginTradingAndShortSellingInfo> getAllRecords() {
		return repository.findAll();
	}

	public Optional<MarginTradingAndShortSellingInfo> getRecordById(
			MarginTradingAndShortSellingInfo.MarginTradingAndShortSellingInfoId id) {
		return repository.findById(id);
	}

	public MarginTradingAndShortSellingInfo saveRecord(MarginTradingAndShortSellingInfo record) {
		return repository.save(record);
	}

	public void deleteRecordById(MarginTradingAndShortSellingInfo.MarginTradingAndShortSellingInfoId id) {
		repository.deleteById(id);
	}

	public List<MarginTradingAndShortSellingInfo> saveAll(
			List<MarginTradingAndShortSellingInfo> marginTradingAndShortSellingInfos) {
		return repository.saveAll(marginTradingAndShortSellingInfos);
	}

	public List<MarginTradingAndShortSellingInfo> findMarginTradingAndShortSellingInfoByDateRange(String stockCode,
			Date firstDate, Date secondDate) {
		return repository.findMarginTradingAndShortSellingInfoByDateRange(stockCode, firstDate, secondDate);
	}

	public List<MarginTradingAndShortSellingInfo> findByTradingDayBeforEqualLimitTwoFourty(Date startDateMinus360,
			Date endDate) {

		String sql = """
				    select *
				    from bstock.margin_trading_and_short_selling_info
				    where trading_day between ? and ?
				    order by stock_code, trading_day desc
				""";

		return jdbcTemplate.query(sql, ps -> {
			ps.setFetchSize(1000);
			ps.setDate(1, new java.sql.Date(startDateMinus360.getTime()));
			ps.setDate(2, new java.sql.Date(endDate.getTime()));
		}, (rs, rowNum) -> {

			MarginTradingAndShortSellingInfo s = new MarginTradingAndShortSellingInfo();

			s.setStockCode(rs.getString("stock_code"));
			s.setTradingDay(rs.getDate("trading_day"));
			s.setMarginPurchaseBalancePreviousDay(rs.getString("margin_purchase_balance_previous_day"));
			s.setMarginPurchase(rs.getString("margin_purchase"));
			s.setMarginSales(rs.getString("margin_sales"));
			s.setCashRedemption(rs.getString("cash_redemption"));
			s.setMarginPurchaseBalance(rs.getString("margin_purchase_balance"));
			s.setMarginPurchaseQuota(rs.getString("margin_purchase_quota"));
			s.setShortSaleBalancePreviousDay(rs.getString("short_sale_balance_previous_day"));
			s.setShortSale(rs.getString("short_sale"));
			s.setShortConvering(rs.getString("short_convering"));
			s.setStockRedemption(rs.getString("stock_redemption"));
			s.setShortSaleBalance(rs.getString("short_sale_balance"));
			s.setShortSaleQuota(rs.getString("short_sale_quota"));
			s.setOffsetting(rs.getString("offsetting"));

			return s;
		});
	}

	public void bulkUpsertMarginTradingAndShortSellingInfo(List<MarginTradingAndShortSellingInfo> data)
			throws Exception {

		if (data == null || data.isEmpty()) {
			return;
		}

		try (Connection connection = dataSource.getConnection()) {

			connection.setAutoCommit(false);

			CopyManager copyManager = new CopyManager(connection.unwrap(BaseConnection.class));

			// create temp table
			try (Statement stmt = connection.createStatement()) {
				stmt.execute("""
						    CREATE TEMP TABLE tmp_margin_trading_and_short_selling_info
						    (LIKE bstock.margin_trading_and_short_selling_info INCLUDING ALL)
						    ON COMMIT DROP
						""");
			}

			// 2 COPY
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
				try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(pos))) {

					for (MarginTradingAndShortSellingInfo item : data) {
						writer.write(buildCsvLine(item));
						writer.newLine();
					}
				}
				return null;
			});

			copyManager.copyIn(copySql, pis);
			executor.shutdown();

			// ⭐ 3️⃣ UPSERT (NO DELETE)
			try (Statement stmt = connection.createStatement()) {
				stmt.execute("""
						    INSERT INTO bstock.margin_trading_and_short_selling_info AS t (
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
						    SELECT DISTINCT ON (trading_day, stock_code)
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
						    FROM tmp_margin_trading_and_short_selling_info
						    ORDER BY trading_day, stock_code
						    ON CONFLICT (trading_day, stock_code)
						    DO UPDATE SET
						        margin_purchase_balance_previous_day = EXCLUDED.margin_purchase_balance_previous_day,
						        margin_purchase = EXCLUDED.margin_purchase,
						        margin_sales = EXCLUDED.margin_sales,
						        cash_redemption = EXCLUDED.cash_redemption,
						        margin_purchase_balance = EXCLUDED.margin_purchase_balance,
						        margin_purchase_quota = EXCLUDED.margin_purchase_quota,
						        short_sale_balance_previous_day = EXCLUDED.short_sale_balance_previous_day,
						        short_sale = EXCLUDED.short_sale,
						        short_convering = EXCLUDED.short_convering,
						        stock_redemption = EXCLUDED.stock_redemption,
						        short_sale_balance = EXCLUDED.short_sale_balance,
						        short_sale_quota = EXCLUDED.short_sale_quota,
						        offsetting = EXCLUDED.offsetting;
						""");
			}

			connection.commit();
		}
	}

	private String buildCsvLine(MarginTradingAndShortSellingInfo item) {

		StringBuilder sb = new StringBuilder(256);

		// ⭐ first column WITHOUT comma
		appendFirstCsv(sb, formatDate(item.getTradingDay()));

		appendCsv(sb, item.getStockCode());
		appendCsv(sb, item.getMarginPurchaseBalancePreviousDay());
		appendCsv(sb, item.getMarginPurchase());
		appendCsv(sb, item.getMarginSales());
		appendCsv(sb, item.getCashRedemption());
		appendCsv(sb, item.getMarginPurchaseBalance());
		appendCsv(sb, item.getMarginPurchaseQuota());
		appendCsv(sb, item.getShortSaleBalancePreviousDay());
		appendCsv(sb, item.getShortSale());
		appendCsv(sb, item.getShortConvering());
		appendCsv(sb, item.getStockRedemption());
		appendCsv(sb, item.getShortSaleBalance());
		appendCsv(sb, item.getShortSaleQuota());
		appendCsv(sb, item.getOffsetting());

		return sb.toString();
	}

	private void appendFirstCsv(StringBuilder sb, String value) {

		if (value == null) {
			sb.append("");
			return;
		}

		sb.append(value);
	}

	private void appendCsv(StringBuilder sb, String value) {

		sb.append(',');

		if (value == null || value.isBlank()) {
			sb.append("");
			return;
		}

		boolean needQuote = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r");

		if (needQuote) {
			sb.append('"');
			sb.append(value.replace("\"", "\"\""));
			sb.append('"');
		} else {
			sb.append(value);
		}
	}

	private String formatDate(Date d) {

		if (d == null)
			return null;

		return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().toString();
	}
}
