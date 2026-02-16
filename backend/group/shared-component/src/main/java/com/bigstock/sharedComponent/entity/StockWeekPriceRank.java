package com.bigstock.sharedComponent.entity;

import java.math.BigDecimal;
import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "stock_week_price_rank", schema = "bstock", uniqueConstraints = @UniqueConstraint(columnNames = {
		"stock_code", "year", "weekofyear" }))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(StockWeekPriceRank.StockWeekPriceRankId.class)
public class StockWeekPriceRank {

	@Id
	@Column(name = "stock_code")
	private String stockCode;

	@Id
	@Column(name = "week_of_year")
	private String weekOfYear;

	@Id
	@Column(name = "year")
	private String year;

	@Column(name = "month")
	private Integer month;

	@Column(name = "first_trading_day")
	private Date firstTradingDay;

	@Column(name = "high_price")
	private BigDecimal highPrice;

	@Column(name = "low_price")
	private BigDecimal lowPrice;

	@Column(name = "change_rate")
	private BigDecimal changeRate;

	@Column(name = "trading_volume")
	private Integer tradingVolume;

	@Column(name = "line_k_value")
	private BigDecimal lineKValue;

	@Column(name = "line_d_value")
	private BigDecimal lineDValue;

	@Column(name = "line_rsv_value")
	private BigDecimal lineRsvValue;

	@Column(name = "five_ma")
	private BigDecimal fiveWeekMa;

	@Column(name = "ten_ma")
	private BigDecimal tenWeekMa;

	@Column(name = "twenty_ma")
	private BigDecimal twentyWeekMa;

	@Column(name = "sixty_ma")
	private BigDecimal sixtyWeekMa;

	@Column(name = "one_twenty_ma")
	private BigDecimal oneTwentyWeekMa;

	@Column(name = "two_fourty_ma")
	private BigDecimal twoFourtyWeekMa;

	@Column(name = "opening_price")
	private BigDecimal openingPrice;

	@Column(name = "closing_price")
	private BigDecimal closingPrice;

	@Column(name = "rank_no")
	private Integer rankNo;

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class StockWeekPriceRankId {

		@Column(name = "stock_code")
		private String stockCode;

		@Column(name = "week_of_year")
		private String weekOfYear;

		@Column(name = "year")
		private String year;
	}
}
