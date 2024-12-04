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
	
	@Column(name = "change_rate")
	private Double changeRate;
	
	@Column(name = "week_of_year")
	private String weekOfYear;

	@Column(name = "trading_volume")
	private String tradingVolume;
	
	@Column(name = "lmit_up")
	private String limitUp;
	
	@Column(name = "limit_down")
	private String limitDown;
	
	@Column(name = "line_k_value")
	private String lineKvalue;
	
	@Column(name = "line_d_value")
	private String lineDvalue;
	
	@Column(name = "line_rsv_value")
	private String lineRSVvalue;
	
	@Column(name = "five_days_ma")
	private String fiveDaysMa;
	
	@Column(name = "twenty_days_ma")
	private String twentyDaysMa;
	
	@Column(name = "ten_days_ma")
	private String tenDaysMa;
	
	@Column(name = "sixty_days_ma")
	private String sixtyDaysMa;
	
	@Column(name = "one_twenty_days_ma")
	private String oneTwentyDaysMa;
	
	@Column(name = "two_fourty_days_ma")
	private String twoFourtyDaysMa;
	
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
