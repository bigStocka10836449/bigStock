package com.bigstock.sharedComponent.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class SingleStockWeekPriceVo {
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
	private String firstTradingDate;
	
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
}
