package com.bigstock.sharedComponent.entity;

import java.math.BigDecimal;
import java.util.Date;

import com.bigstock.sharedComponent.entity.StockMonthPrice.StockMonthPriceId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "stock_month_price", schema = "bstock")
@IdClass(StockMonthPriceId.class)
public class StockMonthPrice {

	@Id
	@Column(name = "stock_code", nullable = false)
	private String stockCode;

	@Id
	@Column(name = "year", nullable = false)
	private String year;

	@Id
	@Column(name = "month", nullable = false)
	private Integer month;
	
	
	@Column(name = "month_of_year")
	private String monthOfYear;

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

	@Column(name = "five_week_ma")
	private BigDecimal fiveWeekMa;

	@Column(name = "twenty_week_ma")
	private BigDecimal twentyWeekMa;

	@Column(name = "ten_week_ma")
	private BigDecimal tenWeekMa;

	@Column(name = "sixty_week_ma")
	private BigDecimal sixtyWeekMa;

	@Column(name = "one_twenty_week_ma")
	private BigDecimal oneTwentyWeekMa;

	@Column(name = "two_fourty_week_ma")
	private BigDecimal twoFourtyWeekMa;

	@Column(name = "opening_price")
	private BigDecimal openingPrice;

	@Column(name = "closing_price")
	private BigDecimal closingPrice;

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class StockMonthPriceId {
		@Id
		@Column(name = "stock_code", nullable = false)
		private String stockCode;

		@Id
		@Column(name = "year", nullable = false)
		private String year;

		@Id
		@Column(name = "month", nullable = false)
		private Integer month;
	}

}
