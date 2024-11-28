package com.bigstock.sharedComponent.entity;

import java.io.Serializable;
import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(schema = "bstock", name = "margin_trading_and_short_selling_info", uniqueConstraints = @UniqueConstraint(columnNames = {
		"trading_day", "stock_code" }))
@Data
@IdClass(MarginTradingAndShortSellingInfo.MarginTradingAndShortSellingInfoId.class)
public class MarginTradingAndShortSellingInfo {

	@Id
	@Column(name = "trading_day")
	private Date tradingDay;
	@Id
	@Column(name = "stock_code")
	private String stockCode;

	@Column(name = "margin_purchase_balance_previous_day")
	private String marginPurchaseBalancePreviousDay;

	@Column(name = "margin_purchase")
	private String marginPurchase;

	@Column(name = "margin_sales")
	private String marginSales;

	@Column(name = "cash_redemption")
	private String cashRedemption;

	@Column(name = "margin_purchase_balance")
	private String marginPurchaseBalance;

	@Column(name = "margin_purchase_quota")
	private String marginPurchaseQuota;

	@Column(name = "short_sale_balance_previous_day")
	private String shortSaleBalancePreviousDay;

	@Column(name = "short_sale")
	private String shortSale;

	@Column(name = "short_convering")
	private String shortConvering;

	@Column(name = "stock_redemption")
	private String stockRedemption;

	@Column(name = "short_sale_balance")
	private String shortSaleBalance;

	@Column(name = "short_sale_quota")
	private String shortSaleQuota;

	@Column(name = "offsetting")
	private String offsetting;

	@Getter
	@Setter
	public static class MarginTradingAndShortSellingInfoId implements Serializable {
		private static final long serialVersionUID = -6247467462242462679L;

		@Id
		@Column(name = "trading_day")
		private Date tradingDay;
		@Id
		@Column(name = "stock_code")
		private String stockCode;
	}
}