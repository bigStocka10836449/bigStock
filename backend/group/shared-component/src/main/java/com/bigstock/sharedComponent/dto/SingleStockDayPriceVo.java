package com.bigstock.sharedComponent.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 1個股編號 2.開盤價(周)  3.收盤價(周)  4.最高價(周)  5.最低價(周) 6.成交量(可能會有誤差)
 */
@Data
public class SingleStockDayPriceVo {

	@Schema(name = "股票代號", description = "", example = "")
	private String stockCode;
	
	@Schema(name = "股票名稱", description = "", example = "")
	private String stockName;
	
	@Schema(name = "周開盤價", description = "", example = "")
	private String openingPrice;

	@Schema(name = "周收盤價", description = "", example = "")
	private String closingPrice;

	@Schema(name = "最高價(周)", description = "", example = "")
	private String highPrice;

	@Schema(name = "最低價(周)", description = "", example = "")
	private String lowPrice;
	
	@Schema(name = "交易日", description = "", example = "")
	private String tradingDate;
	
	@Schema(name = "成交量(可能會有誤差)", description = "", example = "")
	private String tradingVolume;
	
	private BigDecimal fiveMa;
	
	private BigDecimal tenMa;
	
	private BigDecimal twentyMa;
	
	private BigDecimal sixtyMa;
	
	private BigDecimal oneTwentyMa;
	
	private BigDecimal twoFourtyMa;
	
	private BigDecimal lineKvalue;
	
	private BigDecimal lineDvalue;

    //新增：融資融券欄位（與 margin_trading_and_short_selling_info 對應）
    @Schema(description = "融資：前日餘額")
    private String marginPurchaseBalancePreviousDay;

    @Schema(description = "融資：買進")
    private String marginPurchase;

    @Schema(description = "融資：賣出")
    private String marginSales;

    @Schema(description = "融資：現金償還")
    private String cashRedemption;

    @Schema(description = "融資：今日餘額")
    private String marginPurchaseBalance;

    @Schema(description = "融資：限額")
    private String marginPurchaseQuota;

    @Schema(description = "融券：前日餘額")
    private String shortSaleBalancePreviousDay;

    @Schema(description = "融券：賣出")
    private String shortSale;

    @Schema(description = "融券：回補")
    private String shortConvering;

    @Schema(description = "融券：現券償還")
    private String stockRedemption;

    @Schema(description = "融券：今日餘額")
    private String shortSaleBalance;

    @Schema(description = "融券：限額")
    private String shortSaleQuota;

    @Schema(description = "資券：互抵")
    private String offsetting;
}
