package com.bigstock.sharedComponent.entity;

import java.io.Serializable;
import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@Data
@Entity
@Table(name = "stock_exchange_detail", schema = "bstock")
@IdClass(StockExchangeDetail.StockExchangeDetailId.class)
public class StockExchangeDetail {

	@Id
	@Column(name = "seq", nullable = false)
	private Integer seq;
	
	@Id
	@Column(name = "stock_code", nullable = false)
	private String stockCode;

	@Id
	@Column(name = "exchange_time", nullable = false)
	private String exchangeTime;

	@Id
	@Column(name = "trading_date", nullable = false)
	@Temporal(TemporalType.DATE)
	private Date tradingDate;

	@Column(name = "exchage_price", nullable = false)
	private String exchangePrice;

	@Column(name = "exchage_quantity", nullable = false)
	private int exchangeQuantity;

	@Getter
	@Setter
	public static class StockExchangeDetailId implements Serializable {
		private static final long serialVersionUID = -6247467462242462679L;
		
		@Id
		@Column(name = "seq", nullable = false)
		private Integer seq;
		
		@Id
		@Column(name = "stock_code", nullable = false)
		private String stockCode;

		@Id
		@Column(name = "exchange_time", nullable = false)
		private String exchangeTime;

		@Id
		@Column(name = "trading_date", nullable = false)
		@Temporal(TemporalType.DATE)
		private Date tradingDate;

	}
}
