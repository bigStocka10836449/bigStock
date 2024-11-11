package com.bigstock.sharedComponent.entity;

import java.io.Serializable;
import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@Data
@Entity
@Table(name = "stock_day_price", schema = "bstock")
@IdClass(StockDayPrice.StockDayPriceId.class)
public class StockDayPrice {

	@Id
	@Column(name = "stock_code")
	private String stockCode;

	@Id
	@Column(name = "trading_day")
	private Date tradingDay;

	@Column(name = "opening_price")
	private String openingPrice;

	@Column(name = "closing_price")
	private String closingPrice;

	@Column(name = "high_price")
	private String highPrice;

	@Column(name = "low_price")
	private String lowPrice;
	
	@Column(name = "start_of_week_date")
	private Date startOfWeekDate;
	
	@Column(name = "end_of_week_date")
	private Date endOfWeekDate;
	
	@Column(name = "change")
	private String change;
	
	@Column(name = "week_of_year")
	private String weekOfYear;

	@Getter
	@Setter
	public static class StockDayPriceId implements Serializable {
		private static final long serialVersionUID = -6247467462242462679L;

		@Id
		@Column(name = "stock_code")
		private String stockCode;

		@Id
		@Column(name = "trading_day")
		private Date tradingDay;
	}
}
