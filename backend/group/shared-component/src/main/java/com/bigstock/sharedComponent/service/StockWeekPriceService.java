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
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.sql.DataSource;

import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.springframework.data.repository.query.Param;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.bigstock.sharedComponent.entity.StockWeekPrice;
import com.bigstock.sharedComponent.entity.StockWeekPrice.StockWeekPriceId;
import com.bigstock.sharedComponent.entity.StockWeekPriceRank;
import com.bigstock.sharedComponent.repository.StockWeekPriceRankRepository;
import com.bigstock.sharedComponent.repository.StockWeekPriceRepository;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StockWeekPriceService {

	private final StockWeekPriceRepository repository;
	
	private final StockWeekPriceRankRepository stockWeekPriceRankRepository;
	
	private final EntityManager entityManager;
	
	private final JdbcTemplate jdbcTemplate;
	
	private final DataSource dataSource;

	public List<StockWeekPrice> findAll() {
		return repository.findAll();
	}

	public Optional<StockWeekPrice> findById(StockWeekPriceId id) {
		return repository.findById(id);
	}

	public StockWeekPrice save(StockWeekPrice stockWeekPrice) {
		return repository.save(stockWeekPrice);
	}

	public List<StockWeekPrice> saveAll(List<StockWeekPrice> stockWeekPrices) {
		return repository.saveAll(stockWeekPrices);
	}

	public void deleteById(StockWeekPriceId id) {
		repository.deleteById(id);
	}

	public List<StockWeekPrice> findByYearBeforEqualLimitTwoFourty(String startyearMinus6, String endYear) {
		return repository.findByYearBeforEqualLimitTwoFourty(startyearMinus6, endYear);
	}

	public List<StockWeekPrice> findBySockCode(@Param("stockCode") String stockCode) {
		return repository.findBySockCode(stockCode);
	}

	public List<StockWeekPrice> findByWeekOfYear(String weekOfYear) {
		return repository.findByWeekOfYear(weekOfYear);
	}

	public List<StockWeekPrice> findStockCodeAndLimit(String stockCode, Integer limit) {
		return repository.findStockCodeAndLimit(stockCode, limit);
	}
	
	public List<StockWeekPrice> findRankByStockCode(String stockCode){
		return repository.findByStockCode(stockCode);
	}
	
	@Transactional
	public void batchInsertWeekPrices(List<StockWeekPrice> weekPrices) {

	    String sql = """
	        INSERT INTO bstock.stock_week_price (
	            stock_code,
	            week_of_year,
	            year,
	            month,
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
	        VALUES (
	            ?,?,?,?,?,?,?,?,?,?,
	            ?,?,?,?,?,?,?,?,?,?
	        )
	        ON CONFLICT (stock_code, week_of_year, year)
	        DO UPDATE SET
	            month = EXCLUDED.month,
	            first_trading_day = EXCLUDED.first_trading_day,
	            high_price = EXCLUDED.high_price,
	            low_price = EXCLUDED.low_price,
	            change_rate = EXCLUDED.change_rate,
	            trading_volume = EXCLUDED.trading_volume,
	            line_k_value = EXCLUDED.line_k_value,
	            line_d_value = EXCLUDED.line_d_value,
	            line_rsv_value = EXCLUDED.line_rsv_value,
	            five_ma = EXCLUDED.five_ma,
	            twenty_ma = EXCLUDED.twenty_ma,
	            ten_ma = EXCLUDED.ten_ma,
	            sixty_ma = EXCLUDED.sixty_ma,
	            one_twenty_ma = EXCLUDED.one_twenty_ma,
	            two_fourty_ma = EXCLUDED.two_fourty_ma,
	            opening_price = EXCLUDED.opening_price,
	            closing_price = EXCLUDED.closing_price
	        """;

	    jdbcTemplate.batchUpdate(sql, weekPrices, weekPrices.size(),
	        (ps, item) -> {

	            ps.setString(1, item.getStockCode());
	            ps.setString(2, item.getWeekOfYear());
	            ps.setString(3, item.getYear());
	            ps.setObject(4, item.getMonth());
	            ps.setDate(5, toSqlDate(item.getFirstTradingDay()));

	            ps.setBigDecimal(6, item.getHighPrice());
	            ps.setBigDecimal(7, item.getLowPrice());
	            ps.setBigDecimal(8, item.getChangeRate());
	            ps.setObject(9, item.getTradingVolume());

	            ps.setBigDecimal(10, item.getLineKValue());
	            ps.setBigDecimal(11, item.getLineDValue());
	            ps.setBigDecimal(12, item.getLineRsvValue());

	            ps.setBigDecimal(13, item.getFiveWeekMa());
	            ps.setBigDecimal(14, item.getTwentyWeekMa());
	            ps.setBigDecimal(15, item.getTenWeekMa());
	            ps.setBigDecimal(16, item.getSixtyWeekMa());
	            ps.setBigDecimal(17, item.getOneTwentyWeekMa());
	            ps.setBigDecimal(18, item.getTwoFourtyWeekMa());

	            ps.setBigDecimal(19, item.getOpeningPrice());
	            ps.setBigDecimal(20, item.getClosingPrice());
	        }
	    );
	}

	private java.sql.Date toSqlDate(Date date) {
	    return date == null ? null : new java.sql.Date(date.getTime());
	}
	
	@Transactional
	public void batchMergeMonthPrices(List<StockWeekPrice> weekPrices) {

		int batchSize = 100;

		for (int i = 0; i < weekPrices.size(); i++) {
			entityManager.persist(weekPrices.get(i));

			if (i > 0 && i % batchSize == 0) {
				entityManager.flush();
				entityManager.clear();
			}
		}

		entityManager.flush();
		entityManager.clear();
	}
	
	public StockWeekPriceRank buildRanks(String stockCode, List<StockWeekPrice> WeekPrices) {

		// 依 年 + 交易日期排序 由新到舊排序
		List<StockWeekPrice> sorted = WeekPrices.stream()
				.sorted(Comparator.comparing(StockWeekPrice::getFirstTradingDay).reversed())
				.limit(720).toList();

		List<StockWeekPriceRank> ranks = new ArrayList<>(sorted.size());

		int rankNo = 1;
		for (StockWeekPrice p : sorted) {

			StockWeekPriceRank r = new StockWeekPriceRank();


		    r.setStockCode(p.getStockCode());
		    r.setYear(p.getYear());
		    r.setMonth(p.getMonth());


		    r.setWeekOfYear(p.getWeekOfYear());
		    r.setFirstTradingDay(p.getFirstTradingDay()); 

		    r.setOpeningPrice(p.getOpeningPrice());
		    r.setClosingPrice(p.getClosingPrice());
		    r.setHighPrice(p.getHighPrice());
		    r.setLowPrice(p.getLowPrice());

		    r.setTradingVolume(p.getTradingVolume());
		    r.setChangeRate(p.getChangeRate());

	
		    r.setLineKValue(p.getLineKValue());
		    r.setLineDValue(p.getLineDValue());
		    r.setLineRsvValue(p.getLineRsvValue());


		    r.setFiveWeekMa(p.getFiveWeekMa());
		    r.setTenWeekMa(p.getTenWeekMa());
		    r.setTwentyWeekMa(p.getTwentyWeekMa());
		    r.setSixtyWeekMa(p.getSixtyWeekMa());
		    r.setOneTwentyWeekMa(p.getOneTwentyWeekMa());
		    r.setTwoFourtyWeekMa(p.getTwoFourtyWeekMa());


		    r.setRankNo(rankNo);
		    rankNo = rankNo +1;
		    ranks.add(r);
		}

		return ranks.get(0);
	}
	@Transactional
	public void updateRankNo() {
		stockWeekPriceRankRepository.updateRankNo();
	}
	
	@Transactional
	public void deleteByRankNoLessThanZero() {
		stockWeekPriceRankRepository.deleteByRankNoLessThanZero();
	}
	
	@Transactional
	public void batchInsertWeekRankPrices(List<StockWeekPriceRank> weekRankPrices) throws SQLException, IOException {


	    if (weekRankPrices == null || weekRankPrices.isEmpty()) {
	        return;
	    }

	    try (Connection connection = dataSource.getConnection()) {

	        connection.setAutoCommit(false);

	        CopyManager copyManager =
	                new CopyManager(connection.unwrap(BaseConnection.class));

	        // 1️⃣ TEMP TABLE (same schema)
	        try (Statement stmt = connection.createStatement()) {
	            stmt.execute("""
	                CREATE TEMP TABLE tmp_stock_week_price_rank
	                (LIKE bstock.stock_week_price_rank INCLUDING ALL)
	                ON COMMIT DROP
	            """);
	        }

	        // 2️⃣ COPY INTO TEMP TABLE
	        String copySql = """
	            COPY tmp_stock_week_price_rank (
	                stock_code,
	                week_of_year,
	                year,
	                month,
	                first_trading_day,
	                high_price,
	                low_price,
	                change_rate,
	                trading_volume,
	                line_k_value,
	                line_d_value,
	                line_rsv_value,
	                five_ma,
	                ten_ma,
	                twenty_ma,
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
	            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(pos))) {
	                for (StockWeekPriceRank item : weekRankPrices) {
	                    writer.write(buildWeekRankCsvLine(item));
	                    writer.newLine();
	                }
	            }
	            return null;
	        });

	        copyManager.copyIn(copySql, pis);
	        executor.shutdown();

	        // 3️⃣ DELETE old rows by key (same as your ON CONFLICT key)
	        try (PreparedStatement ps = connection.prepareStatement("""
	            DELETE FROM bstock.stock_week_price_rank t
	            USING tmp_stock_week_price_rank tmp
	            WHERE t.stock_code = tmp.stock_code
	              AND t.week_of_year = tmp.week_of_year
	              AND t.year = tmp.year
	        """)) {
	            ps.executeUpdate();
	        }

	        // 4️⃣ INSERT new rows
	        try (Statement stmt = connection.createStatement()) {
	            stmt.execute("""
	                INSERT INTO bstock.stock_week_price_rank
	                SELECT * FROM tmp_stock_week_price_rank
	            """);
	        }

	        connection.commit();
	    }
	}
	
	private String buildWeekRankCsvLine(StockWeekPriceRank item) {
	    return String.join(",",
	            csv(item.getStockCode()),
	            csv(item.getWeekOfYear()),
	            csv(item.getYear()),
	            csv(item.getMonth()),
	            csvDate(item.getFirstTradingDay()),
	            csv(item.getHighPrice()),
	            csv(item.getLowPrice()),
	            csv(item.getChangeRate()),
	            csv(item.getTradingVolume()),
	            csv(item.getLineKValue()),
	            csv(item.getLineDValue()),
	            csv(item.getLineRsvValue()),
	            csv(item.getFiveWeekMa()),
	            csv(item.getTenWeekMa()),
	            csv(item.getTwentyWeekMa()),
	            csv(item.getSixtyWeekMa()),
	            csv(item.getOneTwentyWeekMa()),
	            csv(item.getTwoFourtyWeekMa()),
	            csv(item.getOpeningPrice()),
	            csv(item.getClosingPrice()),
	            csv(item.getRankNo())
	    );
	}
	private String csv(Object v) {
	    if (v == null) return "";
	    String s = v.toString();
	    // if contains special chars, wrap with quotes and escape quotes
	    if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
	        s = s.replace("\"", "\"\"");
	        return "\"" + s + "\"";
	    }
	    return s;
	}

	private String csvDate(Date d) {
	    if (d == null) return "";
	    return d.toInstant().atZone(ZoneId.of("Asia/Taipei")).toLocalDate().toString();
	}

}
