package com.bigstock.sharedComponent.service;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.sql.DataSource;

import org.apache.commons.lang3.math.NumberUtils;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.springframework.aop.framework.AopContext;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.repository.query.Param;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.bigstock.sharedComponent.annotation.BigStockCacheableWithLock;
import com.bigstock.sharedComponent.entity.StockDayPrice;
import com.bigstock.sharedComponent.entity.StockDayPriceRank;
import com.bigstock.sharedComponent.repository.StockDayPriceRankRepository;
import com.bigstock.sharedComponent.repository.StockDayPriceRepository;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StockDayPriceService {
	private final StockDayPriceRepository stockDayPriceRepository;
	
	private final StockDayPriceRankRepository stockDayPriceRankRepository;
	
	private final EntityManager entityManager;
	
	private final DataSource dataSource;
	
	private final JdbcTemplate jdbcTemplate;

	@CacheEvict(value = { "shortLivedCache", "longLivedCache", "defaultCache" }, allEntries = true)
	public StockDayPrice save(StockDayPrice stockDayPrice) {
		return stockDayPriceRepository.save(stockDayPrice);
	}

	@CacheEvict(value = { "shortLivedCache", "longLivedCache", "defaultCache" }, allEntries = true)
	public List<StockDayPrice> saveAll(List<StockDayPrice> stockDayPrices) {
		return stockDayPriceRepository.saveAll(stockDayPrices);
	}

	@CacheEvict(value = { "shortLivedCache", "longLivedCache", "defaultCache" }, allEntries = true)
	public void deleteByIds(List<StockDayPrice.StockDayPriceId> ids) {
		stockDayPriceRepository.deleteAllByIdInBatch(ids);
	};

	public Optional<StockDayPrice> findById(StockDayPrice.StockDayPriceId id) {
		return stockDayPriceRepository.findById(id);
	}

	public List<StockDayPrice> findByStockCode(String stockCode) {
		return stockDayPriceRepository.findByStockCode(stockCode);
	}

	public List<StockDayPrice> findThisWeekStockDayPrices(String stockCode, String weekOfYear) {
		return stockDayPriceRepository.findThisWeekStockDayPrices(stockCode, weekOfYear);
	}

	public Optional<StockDayPrice> findByStockCodeAndTradingDate(String stockCode, Date tradingDate) {
		return stockDayPriceRepository.findByStockCodeAndTradingDay(stockCode, tradingDate);
	}

	@Cacheable(value = "middleLivedCache", key = "#p0 + '-' + #p1 + '-' + #p2")
	public List<StockDayPrice> findByStockCodeAndStartDateAndEndDateCache(String stockCode, String startDate,
			String endDate) throws ParseException {
		return getSelf().findByStockCodeAndStartDateAndEndDate(stockCode, startDate, endDate);
	}

	@BigStockCacheableWithLock(value = "middleLivedCache", key = "#p0 + '-' + #p1 + '-' + #p2")
	public List<StockDayPrice> findByStockCodeAndStartDateAndEndDate(String stockCode, String startDate, String endDate)
			throws ParseException {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		return stockDayPriceRepository.findByStockCodeAndStartDateAndEndDate(stockCode, sdf.parse(startDate),
				sdf.parse(endDate));
	}

	public boolean checkIsTradingDateIsExsits(Date tradingDay) {
		return NumberUtils.INTEGER_ONE.equals(stockDayPriceRepository.checkIsTradingDateIsExsits(tradingDay));
	}

	public List<StockDayPrice> findPreviousFiftyTowDaysBeforeLastestDayInfo(String stockCode) {
		return stockDayPriceRepository.findPreviousFiftyTowDaysBeforeLastestDayInfo(stockCode);
	}

	public List<StockDayPrice> findByStartDateAndEndDate(@Param("startDate") Date startDate,
			@Param("endDate") Date endDate) {
		return stockDayPriceRepository.findByStartDateAndEndDate(startDate, endDate);
	}

	public Optional<StockDayPrice> findByStockCodeAndTradingDayBeforLimitOne(String stockCode, Date endDate) {
		return stockDayPriceRepository.findByStockCodeAndTradingDayBeforLimitOne(stockCode, endDate);
	}

	public List<StockDayPrice> findByStockCodeAndTradingDayBeforEqualLimitTwoFourty(Date startDateMinus360,
			 Date endDate){
		
		String sql = """
		        select stock_code, trading_day, opening_price, closing_price, high_price, low_price, line_k_value, line_d_value
		        from bstock.stock_day_price
		        where trading_day between ? and ?
		          and closing_price not in ('--','----','')
		        order by stock_code, trading_day desc
		    """;

		    return jdbcTemplate.query(sql,
		            ps -> {
		                ps.setFetchSize(1000);
		                ps.setDate(1, new java.sql.Date(startDateMinus360.getTime()));
		                ps.setDate(2, new java.sql.Date(endDate.getTime()));
		            },
		            (rs, rowNum) -> {
		                StockDayPrice s = new StockDayPrice();
		                s.setStockCode(rs.getString("stock_code"));
		                s.setTradingDay(rs.getDate("trading_day"));
		                s.setOpeningPrice(rs.getString("opening_price"));
		                s.setClosingPrice(rs.getString("closing_price"));
		                s.setHighPrice(rs.getString("high_price"));
		                s.setLowPrice(rs.getString("low_price"));
		                s.setLineKvalue(rs.getString("line_k_value"));
		                s.setLineDvalue(rs.getString("line_d_value"));
		                return s;
		            }
		    );
	}
	
	public Date getCurrentTradeDate() {
		return stockDayPriceRankRepository.findByIdStockCode("2330").get(0).getTradingDay();
	}

	public Optional<StockDayPrice> findByStockCodeAndTradingDayBeforeAndClosePriceIsValidLimitOne(String stockCode,
			Date endDate) {
		return stockDayPriceRepository.findByStockCodeAndTradingDayBeforeAndClosePriceIsValidLimitOne(stockCode,
				endDate);
	}

	public Integer checkIsTradingDateRangeContaineNotCalculate(@Param("stockCode") String stockCode,
			@Param("startDate") Date startDate, @Param("endDate") Date endDate) {
		return stockDayPriceRepository.checkIsTradingDateRangeContaineNotCalculate(stockCode, startDate, endDate);
	}

	public Optional<StockDayPrice> findLastestStockDayPrice() {
		return stockDayPriceRepository.findLastestStockDayPrice();
	}

	List<String> findClosingPriceReachLimitUpStockCodeByTradingDay(Date tradingDay) {
		return stockDayPriceRepository.findClosingPriceReachLimitUpStockCodeByTradingDay(tradingDay);
	}

	public List<StockDayPrice> findTodateReachLimitUp(Date endDate) {
		return stockDayPriceRepository.findTodateReachLimitUp(endDate);
	}

	public List<StockDayPrice> findByWeekOfYear(String weekOfYear) {
		return stockDayPriceRepository.findByWeekOfYear(weekOfYear);
	}

	public List<StockDayPrice> findByMonthOfYear(String monthOfYear) {
		return stockDayPriceRepository.findByMonthOfYear(monthOfYear);
	}

	public List<StockDayPrice> findStockCodeAndLimit(String stockCode, Integer limit) {
		return stockDayPriceRepository.findStockCodeAndLimit(stockCode, limit);
	}

	private StockDayPriceService getSelf() {
		return (StockDayPriceService) AopContext.currentProxy();
	}
	
	@Transactional
	public void deleteRankNoByStockCode() {
		stockDayPriceRankRepository.deleteByRankNoLessThanZero();
	}
	
	@Transactional
	public void updateRankNo() {
		stockDayPriceRankRepository.updateRankNo();
	}
	
	@Transactional
	public void bulkUpsertDayRankPrices(
	        List<StockDayPriceRank> data,
	        Date tradingDate) throws Exception {

	    if (data == null || data.isEmpty()) {
	        return;
	    }

	    try (Connection connection = dataSource.getConnection()) {

	        connection.setAutoCommit(false);

	        CopyManager copyManager =
	                new CopyManager(connection.unwrap(BaseConnection.class));

	        // 1️⃣ 建立 TEMP TABLE
	        try (Statement stmt = connection.createStatement()) {
	            stmt.execute("""
	                CREATE TEMP TABLE tmp_stock_day_price_rank
	                (LIKE bstock.stock_day_price_rank INCLUDING ALL)
	                ON COMMIT DROP
	            """);
	        }

	        // 2️⃣ COPY INTO TEMP TABLE
	        String copySql = """
	            COPY tmp_stock_day_price_rank (
	                stock_code,
	                trading_day,
	                month_of_year,
	                opening_price,
	                closing_price,
	                high_price,
	                low_price,
	                start_of_week_date,
	                end_of_week_date,
	                "change",
	                change_rate,
	                week_of_year,
	                trading_volume,
	                lmit_up,
	                limit_down,
	                line_k_value,
	                line_d_value,
	                line_rsv_value,
	                five_ma,
	                twenty_ma,
	                ten_ma,
	                sixty_ma,
	                one_twenty_ma,
	                two_fourty_ma,
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

	                for (StockDayPriceRank item : data) {
	                    writer.write(buildCsvLine(item));
	                    writer.newLine();
	                }
	            }
	            return null;
	        });

	        copyManager.copyIn(copySql, pis);

	        executor.shutdown();

	        // 3️⃣ 刪除該交易日舊資料（只刪單日）
	        try (PreparedStatement ps = connection.prepareStatement("""
	           DELETE FROM bstock.stock_day_price_rank t
				USING tmp_stock_day_price_rank tmp
				WHERE t.stock_code = tmp.stock_code
				AND t.trading_day = tmp.trading_day
	        """)) {
	            ps.executeUpdate();
	        }

	        // 4️⃣ Merge
	        try (Statement stmt = connection.createStatement()) {
	            stmt.execute("""
	                INSERT INTO bstock.stock_day_price_rank
	                SELECT * FROM tmp_stock_day_price_rank
	            """);
	        }

	        connection.commit();
	    }
	}
	
	private String buildCsvLine(StockDayPriceRank item) {

	    return String.join(",",
	            safe(item.getStockCode()),
	            toDate(item.getTradingDay()),
	            safe(item.getMonthOfYear()),
	            safe(item.getOpeningPrice()),
	            safe(item.getClosingPrice()),
	            safe(item.getHighPrice()),
	            safe(item.getLowPrice()),
	            toDate(item.getStartOfWeekDate()),
	            toDate(item.getEndOfWeekDate()),
	            safe(item.getChange()),
	            safe(item.getChangeRate()),
	            safe(item.getWeekOfYear()),
	            safe(item.getTradingVolume()),
	            safe(item.getLimitUp()),
	            safe(item.getLimitDown()),
	            safe(item.getLineKvalue()),
	            safe(item.getLineDvalue()),
	            safe(item.getLineRSVvalue()),
	            safe(item.getFiveDaysMa()),
	            safe(item.getTwentyDaysMa()),
	            safe(item.getTenDaysMa()),
	            safe(item.getSixtyDaysMa()),
	            safe(item.getOneTwentyDaysMa()),
	            safe(item.getTwoFourtyDaysMa()),
	            safe(item.getRankNo())
	    );
	}
	
	private String safe(Object value) {
		if (value == null)
			return "";
		return value.toString().replace(",", "");
	}

	private String toDate(Date date) {
	    if (date == null) return "";
	    return new java.sql.Date(date.getTime()).toString();
	}
	
	 @Transactional
	    public void upsertBatch(List<StockDayPrice> list) {

	        if (list == null || list.isEmpty()) {
	            return;
	        }

	        String sql = """
	            INSERT INTO bstock.stock_day_price (
	                stock_code,
	                trading_day,
	                opening_price,
	                closing_price,
	                high_price,
	                low_price,
	                start_of_week_date,
	                end_of_week_date,
	                "change",
	                week_of_year,
	                trading_volume,
	                lmit_up,
	                limit_down,
	                change_rate,
	                line_k_value,
	                line_d_value,
	                line_rsv_value,
	                five_ma,
	                twenty_ma,
	                ten_ma,
	                sixty_ma,
	                one_twenty_ma,
	                two_fourty_ma,
	                month_of_year
	            )
	        	 VALUES (
				    ?,?,?,?,?,?,?,?,?,?,
				    ?,?,?,?,?,?,?,?,?,?,
				    ?,?,?,?
				)
	            ON CONFLICT (stock_code, trading_day)
	            DO UPDATE SET
	                opening_price = EXCLUDED.opening_price,
	                closing_price = EXCLUDED.closing_price,
	                high_price = EXCLUDED.high_price,
	                low_price = EXCLUDED.low_price,
	                start_of_week_date = EXCLUDED.start_of_week_date,
	                end_of_week_date = EXCLUDED.end_of_week_date,
	                "change" = EXCLUDED."change",
	                week_of_year = EXCLUDED.week_of_year,
	                trading_volume = EXCLUDED.trading_volume,
	                lmit_up = EXCLUDED.lmit_up,
	                limit_down = EXCLUDED.limit_down,
	                change_rate = EXCLUDED.change_rate,
	                line_k_value = EXCLUDED.line_k_value,
	                line_d_value = EXCLUDED.line_d_value,
	                line_rsv_value = EXCLUDED.line_rsv_value,
	                five_ma = EXCLUDED.five_ma,
	                twenty_ma = EXCLUDED.twenty_ma,
	                ten_ma = EXCLUDED.ten_ma,
	                sixty_ma = EXCLUDED.sixty_ma,
	                one_twenty_ma = EXCLUDED.one_twenty_ma,
	                two_fourty_ma = EXCLUDED.two_fourty_ma,
	                month_of_year = EXCLUDED.month_of_year
	        """;

	        jdbcTemplate.batchUpdate(sql, list, 100, (ps, price) -> {

	            ps.setString(1, price.getStockCode());
	            ps.setDate(2, new java.sql.Date(price.getTradingDay().getTime()));
	            ps.setString(3, price.getOpeningPrice());
	            ps.setString(4, price.getClosingPrice());
	            ps.setString(5, price.getHighPrice());
	            ps.setString(6, price.getLowPrice());
	            ps.setDate(7, toSqlDate(price.getStartOfWeekDate()));
	            ps.setDate(8, toSqlDate(price.getEndOfWeekDate()));
	            ps.setString(9, price.getChange());
	            ps.setString(10, price.getWeekOfYear());
	            ps.setString(11, price.getTradingVolume());
	            ps.setString(12, price.getLimitUp());
	            ps.setString(13, price.getLimitDown());
	            ps.setString(14, price.getChangeRate() == null ? null : price.getChangeRate().toString());
	            ps.setString(15, price.getLineKvalue());
	            ps.setString(16, price.getLineDvalue());
	            ps.setString(17, price.getLineRSVvalue());
	            ps.setString(18, price.getFiveDaysMa());
	            ps.setString(19, price.getTwentyDaysMa());
	            ps.setString(20, price.getTenDaysMa());
	            ps.setString(21, price.getSixtyDaysMa());
	            ps.setString(22, price.getOneTwentyDaysMa());
	            ps.setString(23, price.getTwoFourtyDaysMa());
	            ps.setString(24, price.getMonthOfYear());
	        });
	    }

	    private java.sql.Date toSqlDate(Date date) {
	        return date == null ? null : new java.sql.Date(date.getTime());
	    }
	

	public StockDayPriceRank buildRanks(String stockCode, StockDayPrice stockDayPrice) {

		// 依 年 + 月 由新到舊排序


			StockDayPriceRank r = new StockDayPriceRank();

		    // =========================
		    // 🔹 PK / Business Key
		    // =========================
		    r.setStockCode(stockDayPrice.getStockCode());
		    r.setWeekOfYear(stockDayPrice.getWeekOfYear());
		    r.setMonthOfYear(stockDayPrice.getMonthOfYear());

		    // =========================
		    // 🔹 時間相關欄位
		    // =========================
		    r.setTradingDay(stockDayPrice.getTradingDay()); 
		    r.setStartOfWeekDate(stockDayPrice.getStartOfWeekDate());
		    r.setEndOfWeekDate(stockDayPrice.getEndOfWeekDate());
		    // 若你擔心 Date 被修改，可改成：
		    // new Date(p.getFirstTradingDay().getTime())

		    // =========================
		    // 🔹 價格資訊（完整快照）
		    // =========================
		    r.setOpeningPrice(stockDayPrice.getOpeningPrice());
		    r.setClosingPrice(stockDayPrice.getClosingPrice());
		    r.setHighPrice(stockDayPrice.getHighPrice());
		    r.setLowPrice(stockDayPrice.getLowPrice());
		    r.setChange(stockDayPrice.getChange());
		    // =========================
		    // 🔹 成交 / 變動
		    // =========================
		    r.setTradingVolume(stockDayPrice.getTradingVolume());
		    r.setChangeRate(stockDayPrice.getChangeRate());
		    r.setLimitUp(stockDayPrice.getLimitUp());
		    r.setLimitDown(stockDayPrice.getLimitDown());
		    // =========================
		    // 🔹 技術指標（KD / RSV）
		    // =========================
		    r.setLineKvalue(stockDayPrice.getLineKvalue());
		    r.setLineDvalue(stockDayPrice.getLineDvalue());
		    r.setLineRSVvalue(stockDayPrice.getLineRSVvalue());

		    // =========================
		    // 🔹 均線（MA）
		    // =========================
		    r.setFiveDaysMa(stockDayPrice.getFiveDaysMa());
		    r.setTenDaysMa(stockDayPrice.getTenDaysMa());
		    r.setTwentyDaysMa(stockDayPrice.getTwentyDaysMa());
		    r.setSixtyDaysMa(stockDayPrice.getSixtyDaysMa());
		    r.setOneTwentyDaysMa(stockDayPrice.getOneTwentyDaysMa());
		    r.setTwoFourtyDaysMa(stockDayPrice.getTwoFourtyDaysMa());


		return r;
	}
}
