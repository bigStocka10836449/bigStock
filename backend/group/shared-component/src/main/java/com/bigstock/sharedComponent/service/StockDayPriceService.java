package com.bigstock.sharedComponent.service;

import java.io.Reader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationContext;
import org.springframework.data.repository.query.Param;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bigstock.sharedComponent.annotation.BigStockCacheableWithLock;
import com.bigstock.sharedComponent.dto.StockTrendCache;
import com.bigstock.sharedComponent.entity.StockDayPrice;
import com.bigstock.sharedComponent.entity.StockDayPriceRank;
import com.bigstock.sharedComponent.enums.TrendRegime;
import com.bigstock.sharedComponent.redis.CacheOperatorService;
import com.bigstock.sharedComponent.repository.StockDayPriceRankRepository;
import com.bigstock.sharedComponent.repository.StockDayPriceRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockDayPriceService {
	
	@Autowired
	private ApplicationContext ctx;
	
	private final StockDayPriceRepository stockDayPriceRepository;
	
	private final StockDayPriceRankRepository stockDayPriceRankRepository;
	
	private final CacheOperatorService cacheOperatorService;
	
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

	public List<StockDayPrice> findByStockCodeAndStartDateAndEndDateCache(String stockCode, String startDate,
			String endDate) throws ParseException {
		List<StockDayPrice> stockDayPrices = cacheOperatorService.getCompressedZSetAllScore("ultraLongLivedCache", "stock:compressed:" + stockCode, StockDayPrice.class);
		if(!stockDayPrices.isEmpty()) {
			return stockDayPrices;
		}
		return getSelf().findByStockCodeAndStartDateAndEndDate(stockCode, startDate, endDate);
	}
	
	public List<StockDayPrice> findLastest600StockDayPriceByStockCodeCache(String stockCode) throws ParseException {
		List<StockDayPrice> stockDayPrices = cacheOperatorService.getCompressedZSetAllScore("ultraLongLivedCache", "stock:compressed:" + stockCode, StockDayPrice.class);
		if(!stockDayPrices.isEmpty()) {
			return stockDayPrices;
		}
		return getSelf().findLastest600StockDayPriceByStockCode(stockCode);
	}
	
	@BigStockCacheableWithLock(value = "ultraLongLivedCache", key = "#p0")
	@Transactional(readOnly = true, timeout = 30)
	public List<StockDayPrice> findLastest600StockDayPriceByStockCode(String stockCode){
		log.info("findLastest600StockDayPriceByStockCode , {}", stockCode);
		List<StockDayPrice> stockDayPrices =
		        stockDayPriceRankRepository.findByIdStockCode(stockCode)
		                .stream()
		                .map(this::buildStockDayPrice)
		                .toList();
		cacheOperatorService.batchUpsertCompressedZSetSeries("ultraLongLivedCache",
				"stock:compressed:" + stockCode,
				stockDayPrices, stockDayPrice -> stockDayPrice.getTradingDay().getTime(),
				CacheOperatorService.DEFAULT_SERIES_MAX_SIZE);
		return stockDayPrices;
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
		        select stock_code, trading_day,change, change_rate, opening_price,
		         closing_price, high_price, low_price,
		         start_of_week_date,end_of_week_date,week_of_year,
		         trading_volume,limit_up,limit_down, line_k_value, line_d_value,five_ma,twenty_ma,ten_ma
		         ,sixty_ma,one_twenty_ma,two_fourty_ma
		        from bstock.stock_day_price_rank
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
		                s.setChange(rs.getString("change"));
		                if(ObjectUtils.isNotEmpty(rs.getString("change_rate"))) {
		                	s.setChangeRate(Double.valueOf(rs.getString("change_rate")));
		                }
		                s.setTradingVolume(rs.getString("trading_volume"));
		                s.setEndOfWeekDate(rs.getDate("end_of_week_date"));
		                s.setWeekOfYear(rs.getString("week_of_year"));
		                s.setStartOfWeekDate(rs.getDate("start_of_week_date"));
		                s.setLimitDown(rs.getString("limit_down"));
		                s.setLimitUp(rs.getString("limit_up"));
		                s.setLineKvalue(rs.getString("line_k_value"));
		                s.setLineDvalue(rs.getString("line_d_value"));
		                s.setFiveDaysMa(rs.getString("five_ma"));
		                s.setTenDaysMa(rs.getString("ten_ma"));
		                s.setTwentyDaysMa(rs.getString("twenty_ma"));
		                s.setSixtyDaysMa(rs.getString("sixty_ma"));
		                s.setOneTwentyDaysMa(rs.getString("one_twenty_ma"));
		                s.setTwoFourtyDaysMa(rs.getString("two_fourty_ma"));
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
	
	public List<StockDayPriceRank> findByIdTradingDay(LocalDate tradingDay){
		return stockDayPriceRankRepository.findByIdTradingDay(tradingDay);
	}

	private StockDayPriceService getSelf() {
		return ctx.getBean(StockDayPriceService.class);
	}
	
	@Transactional
	public void deleteRankNoByStockCode() {
		stockDayPriceRankRepository.deleteByRankNoLessThanZero();
	}
	
	@Transactional
	public void updateRankNo() {
		stockDayPriceRankRepository.updateRankNo();
	}
	
	public void bulkUpsertDayRankPrices(List<StockDayPriceRank> data, Date tradingDate) throws Exception {

		if (data == null || data.isEmpty()) {
			return;
		}

		try (Connection connection = dataSource.getConnection()) {

			connection.setAutoCommit(false);

			CopyManager copyManager = new CopyManager(connection.unwrap(BaseConnection.class));

			// TEMP TABLE
			try (Statement stmt = connection.createStatement()) {
				stmt.execute("""
						    CREATE TEMP TABLE tmp_stock_day_price_rank
						    (LIKE bstock.stock_day_price_rank INCLUDING ALL)
						    ON COMMIT DROP
						""");
			}

			Reader reader = new Reader() {

				private final Iterator<StockDayPriceRank> it = data.iterator();
				private String currentLine = null;
				private int index = 0;

				@Override
				public int read(char[] cbuf, int off, int len) {

					try {
						int count = 0;

						while (count < len) {

							if (currentLine == null || index >= currentLine.length()) {
								if (!it.hasNext())
									break;

								currentLine = buildCsvLine(it.next()) + "\n";
								index = 0;
							}

							cbuf[off + count] = currentLine.charAt(index++);
							count++;
						}

						return count == 0 ? -1 : count;

					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}

				@Override
				public void close() {
				}
			};

			copyManager.copyIn("""
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
					        limit_up,
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
					""", reader);

			// DELETE
			try (PreparedStatement ps = connection.prepareStatement("""
					    DELETE FROM bstock.stock_day_price_rank t
					    USING tmp_stock_day_price_rank tmp
					    WHERE t.stock_code = tmp.stock_code
					    AND t.trading_day = tmp.trading_day
					""")) {
				ps.executeUpdate();
			}

			// INSERT
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
	
	    
	    public StockTrendCache calculationVectors(
	            List<StockDayPrice> list) {

	        if (CollectionUtils.isEmpty(list)) {
	            return null;
	        }

	        list.sort(
	                Comparator.comparing(
	                        StockDayPrice::getTradingDay
	                )
	        );
	        log.info("calculateOneStock stockCode={}", list.get(0).getStockCode());
	        List<LocalDate> dates = new ArrayList<>();

	        List<Double> returns = new ArrayList<>();
	        List<Double> dailyReturns = new ArrayList<>();
	        List<Double> volumeRatio = new ArrayList<>();

	        List<Double> k = new ArrayList<>();
	        List<Double> d = new ArrayList<>();
	        List<Double> rsv = new ArrayList<>();

	        List<Double> bias5 = new ArrayList<>();
	        List<Double> bias10 = new ArrayList<>();
	        List<Double> bias20 = new ArrayList<>();
	        List<Double> bias60 = new ArrayList<>();
	        List<Double> bias120 = new ArrayList<>();
	        List<Double> bias240 = new ArrayList<>();

	        List<Double> gap5_10 = new ArrayList<>();
	        List<Double> gap10_20 = new ArrayList<>();
	        List<Double> gap20_60 = new ArrayList<>();
	        List<Double> gap60_120 = new ArrayList<>();
	        List<Double> gap120_240 = new ArrayList<>();

	        double firstClose =
	                safeDouble(
	                        list.get(0).getClosingPrice()
	                );

	        double previousClose = firstClose;

	        final int VOLUME_WINDOW = 20;

	        for (int i = 0; i < list.size(); i++) {

	            StockDayPrice item = list.get(i);

	            double close =
	                    safeDouble(
	                            item.getClosingPrice()
	                    );

	            LocalDate tradingDate =
	                    item.getTradingDay()
	                            .toInstant()
	                            .atZone(ZoneId.systemDefault())
	                            .toLocalDate();

	            dates.add(tradingDate);

	            /*
	             * cumulative return
	             */
	            returns.add(
	                    firstClose == 0
	                            ? 0D
	                            : (close / firstClose) - 1
	            );

	            /*
	             * daily return
	             */
	            if (i == 0) {

	                dailyReturns.add(0D);

	            } else {

	                dailyReturns.add(
	                        previousClose == 0
	                                ? 0D
	                                : (close / previousClose) - 1
	                );
	            }

	            previousClose = close;

	            /*
	             * KD RSV
	             */
	            k.add(
	                    safeDouble(
	                            item.getLineKvalue()
	                    )
	            );

	            d.add(
	                    safeDouble(
	                            item.getLineDvalue()
	                    )
	            );

	            rsv.add(
	                    safeDouble(
	                            item.getLineRSVvalue()
	                    )
	            );

	            /*
	             * volume ratio
	             */
	            if (i < VOLUME_WINDOW) {

	                volumeRatio.add(1D);

	            } else {

	                double avgVolume = 0;

	                for (int j = i - VOLUME_WINDOW;
	                     j < i;
	                     j++) {

	                    avgVolume +=
	                            safeDouble(
	                                    list.get(j)
	                                            .getTradingVolume()
	                            );
	                }

	                avgVolume /= VOLUME_WINDOW;

	                double currentVolume =
	                        safeDouble(
	                                item.getTradingVolume()
	                        );

	                volumeRatio.add(
	                        avgVolume == 0
	                                ? 1D
	                                : currentVolume / avgVolume
	                );
	            }

	            /*
	             * MA
	             */
	            
	            double ma5 =
	                    safeDouble(item.getFiveDaysMa());
	            
	            double ma10 =
	                    safeDouble(item.getTenDaysMa());

	            double ma20 =
	                    safeDouble(item.getTwentyDaysMa());

	            double ma60 =
	                    safeDouble(item.getSixtyDaysMa());

	            double ma120 =
	                    safeDouble(item.getOneTwentyDaysMa());

	            double ma240 =
	                    safeDouble(item.getTwentyDaysMa());

	            /*
	             * bias
	             */
	            bias5.add(
	                    bias(close, ma5)
	            );
	            
	            bias10.add(
	                    bias(close, ma10)
	            );

	            bias20.add(
	                    bias(close, ma20)
	            );

	            bias60.add(
	                    bias(close, ma60)
	            );

	            bias120.add(
	                    bias(close, ma120)
	            );

	            bias240.add(
	                    bias(close, ma240)
	            );

	            /*
	             * MA hierarchy
	             */
	            gap5_10.add(
	                    gap(ma5, ma10)
	            );
	            
	            gap10_20.add(
	                    gap(ma10, ma20)
	            );

	            gap20_60.add(
	                    gap(ma20, ma60)
	            );

	            gap60_120.add(
	                    gap(ma60, ma120)
	            );

	            gap120_240.add(
	                    gap(ma120, ma240)
	            );
	        }

	        StockDayPrice latest =
	                list.get(list.size() - 1);

	       TrendRegime  trendRegime =
	                calculateTrendRegime(latest
	                );

	        return StockTrendCache.builder()
	                .stockCode(
	                        latest.getStockCode()
	                )
	                .lastDay(
	                        dates.get(
	                                dates.size() - 1
	                        )
	                )
	                .trendRegime(
	                        trendRegime
	                )
	                .dates(dates)
	                .returns(returns)
	                .dailyReturns(dailyReturns)
	                .volumeRatio(volumeRatio)
	                .k(k)
	                .d(d)
	                .rsv(rsv)
	                .bias5(bias5)
	                .bias10(bias10)
	                .bias20(bias20)
	                .bias60(bias60)
	                .bias120(bias120)
	                .bias240(bias240)
	                .gap5_10(gap5_10)
	                .gap10_20(gap10_20)
	                .gap20_60(gap20_60)
	                .gap60_120(gap60_120)
	                .gap120_240(gap120_240)
	                .build();
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
	
	public StockDayPrice buildStockDayPrice(StockDayPriceRank source) {
		if (source == null) {
			return null;
		}

		StockDayPrice target = new StockDayPrice();

		target.setStockCode(source.getStockCode());
		target.setTradingDay(source.getTradingDay());
		target.setMonthOfYear(source.getMonthOfYear());

		target.setOpeningPrice(source.getOpeningPrice());
		target.setClosingPrice(source.getClosingPrice());
		target.setHighPrice(source.getHighPrice());
		target.setLowPrice(source.getLowPrice());

		target.setStartOfWeekDate(source.getStartOfWeekDate());
		target.setEndOfWeekDate(source.getEndOfWeekDate());

		target.setChange(source.getChange());
		target.setChangeRate(source.getChangeRate());
		target.setWeekOfYear(source.getWeekOfYear());

		target.setTradingVolume(source.getTradingVolume());

		target.setLimitUp(source.getLimitUp());
		target.setLimitDown(source.getLimitDown());

		target.setLineKvalue(source.getLineKvalue());
		target.setLineDvalue(source.getLineDvalue());
		target.setLineRSVvalue(source.getLineRSVvalue());

		target.setFiveDaysMa(source.getFiveDaysMa());
		target.setTenDaysMa(source.getTenDaysMa());
		target.setTwentyDaysMa(source.getTwentyDaysMa());
		target.setSixtyDaysMa(source.getSixtyDaysMa());
		target.setOneTwentyDaysMa(source.getOneTwentyDaysMa());
		target.setTwoFourtyDaysMa(source.getTwoFourtyDaysMa());

		return target;
	}

	private double safeDouble(String value) {

	    if (StringUtils.isBlank(value)) {
	        return 0D;
	    }

	    try {

	        return Double.parseDouble(
	                value.replace(",", "")
	        );

	    } catch (Exception e) {

	        return 0D;
	    }
	}

	private TrendRegime calculateTrendRegime(StockDayPrice item) {

	    double close = safeDouble(item.getClosingPrice());

	    double ma120 = safeDouble(item.getOneTwentyDaysMa());
	    double ma240 = safeDouble(item.getTwoFourtyDaysMa());

	    if (close == 0 || ma120 == 0 || ma240 == 0) {
	    	return TrendRegime.SIDEWAY;  // sideway / unknown
	    }

	    if (close > ma120 && ma120 > ma240) {
	        return TrendRegime.BULL; // bull
	    }

	    if (close < ma120 && ma120 < ma240) {
	        return TrendRegime.BEAR; // bear
	    }

	    return TrendRegime.SIDEWAY; // sideway
	}
	private double gap(double shortMa, double longMa) {
	    if (longMa == 0) {
	        return 0D;
	    }

	    return (shortMa - longMa) / longMa;
	}
	
	private double bias(double close, double ma) {
	    if (ma == 0) {
	        return 0D;
	    }

	    return (close - ma) / ma;
	}
}
