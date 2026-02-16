package com.bigstock.sharedComponent.service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.sql.DataSource;

import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.bigstock.sharedComponent.entity.StockMonthPrice;
import com.bigstock.sharedComponent.entity.StockMonthPrice.StockMonthPriceId;
import com.bigstock.sharedComponent.entity.StockMonthPriceRank;
import com.bigstock.sharedComponent.repository.StockMonthPriceRankRepository;
import com.bigstock.sharedComponent.repository.StockMonthPriceRepository;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StockMonthPriceService {

	private final EntityManager entityManager;

	private final StockMonthPriceRepository repository;
	
	private final StockMonthPriceRankRepository stockMonthPriceRankRepository;
	
	private final JdbcTemplate jdbcTemplate;
	
	private final DataSource dataSource;

	public StockMonthPrice save(StockMonthPrice stockMonthPrice) {
		return repository.save(stockMonthPrice);
	}

	public List<StockMonthPrice> saveAll(List<StockMonthPrice> stockMonthPrices) {
		return repository.saveAll(stockMonthPrices);
	}

	public void delete(StockMonthPriceId id) {
		repository.deleteById(id);
	}
	
	@Transactional
	public void updateRankNo() {
		stockMonthPriceRankRepository.updateRankNo();
	}
	
	@Transactional
	public void deleteByRankNoLessThanZero() {
		stockMonthPriceRankRepository.deleteByRankNoLessThanZero();
	}
	

	public StockMonthPrice findById(StockMonthPriceId id) {
		return repository.findById(id).orElse(null);
	}
	
	public List<StockMonthPrice> findRankByStockCode(String stockCode){
		return repository.findByStockCode(stockCode);
	}

	public List<StockMonthPrice> findByStockCodeAndMmonthOfYearBeforEqualLimit(String monthOfYear) {
		return repository.findByMmonthOfYearAndDesc(monthOfYear);
	}

	public List<StockMonthPrice> findByMmonthOfYearAndDesc(String monthOfYear) {
		return repository.findByMmonthOfYearAndDesc(monthOfYear);
	}

	public List<StockMonthPrice> findStockCodeAndLimit(String stockCode, Integer limit) {
		return repository.findStockCodeAndLimit(stockCode, limit);
	}

	public List<StockMonthPrice> findAll() {
		return repository.findAll();
	}

	@Transactional
	public void batchInsertMonthPrices(List<StockMonthPrice> monthPrices) throws SQLException, IOException {
		 if (monthPrices == null || monthPrices.isEmpty()) {
		        return;
		    }

		    try (Connection connection = dataSource.getConnection()) {

		        connection.setAutoCommit(false);

		        CopyManager copyManager =
		                new CopyManager(connection.unwrap(BaseConnection.class));

		        // 1️⃣ TEMP TABLE
		        try (Statement stmt = connection.createStatement()) {
		            stmt.execute("""
		                CREATE TEMP TABLE tmp_stock_month_price
		                (LIKE bstock.stock_month_price INCLUDING ALL)
		                ON COMMIT DROP
		            """);
		        }

		        // 2️⃣ COPY
		        String copySql = """
		            COPY tmp_stock_month_price (
		                stock_code,
		                year,
		                month,
		                month_of_year,
		                first_trading_day,
		                high_price,
		                low_price,
		                change_rate,
		                trading_volume,
		                line_k_value,
		                line_d_value,
		                line_rsv_value,
		                five_ma,
		                twenty_ma,
		                ten_ma,
		                sixty_ma,
		                one_twenty_ma,
		                two_fourty_ma,
		                opening_price,
		                closing_price
		            )
		            FROM STDIN WITH (FORMAT csv)
		        """;

		        PipedOutputStream pos = new PipedOutputStream();
		        PipedInputStream pis = new PipedInputStream(pos, 65536);

		        ExecutorService executor = Executors.newSingleThreadExecutor();

		        executor.submit(() -> {
		            try (BufferedWriter writer =
		                         new BufferedWriter(new OutputStreamWriter(pos))) {

		                for (StockMonthPrice item : monthPrices) {
		                    writer.write(buildMonthCsvLine(item));
		                    writer.newLine();
		                }
		            }
		            return null;
		        });

		        copyManager.copyIn(copySql, pis);
		        executor.shutdown();

		        // 3️⃣ DELETE old rows
		        try (PreparedStatement ps = connection.prepareStatement("""
		            DELETE FROM bstock.stock_month_price t
		            USING tmp_stock_month_price tmp
		            WHERE t.stock_code = tmp.stock_code
		              AND t.year = tmp.year
		              AND t.month = tmp.month
		        """)) {
		            ps.executeUpdate();
		        }

		        // 4️⃣ INSERT new rows
		        try (Statement stmt = connection.createStatement()) {
		            stmt.execute("""
		                INSERT INTO bstock.stock_month_price
		                SELECT * FROM tmp_stock_month_price
		            """);
		        }

		        connection.commit();
		    }
	}

	private String buildMonthCsvLine(StockMonthPrice item) {
	    return String.join(",",
	            csv(item.getStockCode()),
	            csv(item.getYear()),
	            csv(item.getMonth()),
	            csv(item.getMonthOfYear()),
	            csvDate(item.getFirstTradingDay()),
	            csv(item.getHighPrice()),
	            csv(item.getLowPrice()),
	            csv(item.getChangeRate()),
	            csv(item.getTradingVolume()),
	            csv(item.getLineKValue()),
	            csv(item.getLineDValue()),
	            csv(item.getLineRsvValue()),
	            csv(item.getFiveMonthMa()),
	            csv(item.getTwentyMonthMa()),
	            csv(item.getTenMonthMa()),
	            csv(item.getSixtyMonthMa()),
	            csv(item.getOneTwentyMonthMa()),
	            csv(item.getTwoFourtyMonthMa()),
	            csv(item.getOpeningPrice()),
	            csv(item.getClosingPrice())
	    );
	}
	
	private String csv(Object v) {
	    if (v == null) return "";
	    String s = v.toString();
	    if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
	        s = s.replace("\"", "\"\"");
	        return "\"" + s + "\"";
	    }
	    return s;
	}

	private String csvDate(Date d) {
	    if (d == null) return "";
	    return d.toInstant()
	            .atZone(ZoneId.systemDefault())
	            .toLocalDate()
	            .toString();
	}
	
	@Transactional
	public void batchInsertMonthRankPrices(List<StockMonthPriceRank> monthPrices) throws IOException, SQLException {

		 if (monthPrices == null || monthPrices.isEmpty()) {
		        return;
		    }

		    try (Connection connection = dataSource.getConnection()) {

		        connection.setAutoCommit(false);

		        CopyManager copyManager =
		                new CopyManager(connection.unwrap(BaseConnection.class));

		        // 1️⃣ Create TEMP table
		        try (Statement stmt = connection.createStatement()) {
		            stmt.execute("""
		                CREATE TEMP TABLE tmp_stock_month_price_rank
		                (LIKE bstock.stock_month_price_rank INCLUDING ALL)
		                ON COMMIT DROP
		            """);
		        }

		        // 2️⃣ COPY into temp
		        String copySql = """
		            COPY tmp_stock_month_price_rank (
		                stock_code,
		                year,
		                month,
		                month_of_year,
		                first_trading_day,
		                high_price,
		                low_price,
		                change_rate,
		                trading_volume,
		                line_k_value,
		                line_d_value,
		                line_rsv_value,
		                five_ma,
		                twenty_ma,
		                ten_ma,
		                sixty_ma,
		                one_twenty_ma,
		                two_fourty_ma,
		                opening_price,
		                closing_price,
		                rank_no
		            )
		            FROM STDIN WITH (FORMAT csv)
		        """;

		        PipedOutputStream pos = new PipedOutputStream();
		        PipedInputStream pis = new PipedInputStream(pos, 65536);

		        ExecutorService executor = Executors.newSingleThreadExecutor();

		        executor.submit(() -> {
		            try (BufferedWriter writer =
		                         new BufferedWriter(new OutputStreamWriter(pos))) {

		                for (StockMonthPriceRank item : monthPrices) {
		                    writer.write(buildMonthRankCsvLine(item));
		                    writer.newLine();
		                }
		            }
		            return null;
		        });

		        copyManager.copyIn(copySql, pis);
		        executor.shutdown();

		        // 3️⃣ Delete existing rows by unique key
		        try (PreparedStatement ps = connection.prepareStatement("""
		            DELETE FROM bstock.stock_month_price_rank t
		            USING tmp_stock_month_price_rank tmp
		            WHERE t.stock_code = tmp.stock_code
		              AND t.year = tmp.year
		              AND t.month = tmp.month
		        """)) {
		            ps.executeUpdate();
		        }

		        // 4️⃣ Insert fresh rows
		        try (Statement stmt = connection.createStatement()) {
		            stmt.execute("""
		                INSERT INTO bstock.stock_month_price_rank
		                SELECT * FROM tmp_stock_month_price_rank
		            """);
		        }

		        connection.commit();
		    }
	}

	private String buildMonthRankCsvLine(StockMonthPriceRank item) {
	    return String.join(",",
	            csv(item.getStockCode()),
	            csv(item.getYear()),
	            csv(item.getMonth()),
	            csv(item.getMonthOfYear()),
	            csvDate(item.getFirstTradingDay()),
	            csv(item.getHighPrice()),
	            csv(item.getLowPrice()),
	            csv(item.getChangeRate()),
	            csv(item.getTradingVolume()),
	            csv(item.getLineKValue()),
	            csv(item.getLineDValue()),
	            csv(item.getLineRsvValue()),
	            csv(item.getFiveMonthMa()),
	            csv(item.getTwentyMonthMa()),
	            csv(item.getTenMonthMa()),
	            csv(item.getSixtyMonthMa()),
	            csv(item.getOneTwentyMonthMa()),
	            csv(item.getTwoFourtyMonthMa()),
	            csv(item.getOpeningPrice()),
	            csv(item.getClosingPrice()),
	            csv(item.getRankNo())
	    );
	}


	public StockMonthPriceRank buildRanks(String stockCode, List<StockMonthPrice> monthPrices) {

		// 依 年 + 月 由新到舊排序
		List<StockMonthPrice> sorted = monthPrices.stream()
				.sorted(Comparator.comparingInt((StockMonthPrice p) -> Integer.parseInt(p.getYear()))
						.thenComparingInt(StockMonthPrice::getMonth).reversed())
				.limit(720).toList();

		List<StockMonthPriceRank> ranks = new ArrayList<>(sorted.size());

		int rankNo = 1;
		for (StockMonthPrice p : sorted) {

		    StockMonthPriceRank r = new StockMonthPriceRank();

		    // =========================
		    // 🔹 PK / Business Key
		    // =========================
		    r.setStockCode(p.getStockCode());
		    r.setYear(p.getYear());
		    r.setMonth(p.getMonth());

		    // =========================
		    // 🔹 時間相關欄位
		    // =========================
		    r.setMonthOfYear(p.getMonthOfYear());
		    r.setFirstTradingDay(p.getFirstTradingDay()); 
		    // 若你擔心 Date 被修改，可改成：
		    // new Date(p.getFirstTradingDay().getTime())

		    // =========================
		    // 🔹 價格資訊（完整快照）
		    // =========================
		    r.setOpeningPrice(p.getOpeningPrice());
		    r.setClosingPrice(p.getClosingPrice());
		    r.setHighPrice(p.getHighPrice());
		    r.setLowPrice(p.getLowPrice());

		    // =========================
		    // 🔹 成交 / 變動
		    // =========================
		    r.setTradingVolume(p.getTradingVolume());
		    r.setChangeRate(p.getChangeRate());

		    // =========================
		    // 🔹 技術指標（KD / RSV）
		    // =========================
		    r.setLineKValue(p.getLineKValue());
		    r.setLineDValue(p.getLineDValue());
		    r.setLineRsvValue(p.getLineRsvValue());

		    // =========================
		    // 🔹 均線（MA）
		    // =========================
		    r.setFiveMonthMa(p.getFiveMonthMa());
		    r.setTenMonthMa(p.getTenMonthMa());
		    r.setTwentyMonthMa(p.getTwentyMonthMa());
		    r.setSixtyMonthMa(p.getSixtyMonthMa());
		    r.setOneTwentyMonthMa(p.getOneTwentyMonthMa());
		    r.setTwoFourtyMonthMa(p.getTwoFourtyMonthMa());

		    // =========================
		    // 🔹 Rank only（唯一新產生）
		    // =========================
		    r.setRankNo(rankNo++);
		    rankNo = rankNo + 1;
		    ranks.add(r);
		}

		return ranks.get(0);
	}
}
