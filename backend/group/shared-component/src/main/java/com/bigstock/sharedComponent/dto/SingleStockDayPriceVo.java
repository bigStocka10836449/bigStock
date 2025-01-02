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
	
	private BigDecimal fiveDaysMa;
	
	private BigDecimal tenDaysMa;
	
	private BigDecimal twentyDaysMa;
	
	private BigDecimal sixtyDaysMa;
	
	private BigDecimal oneTwentyDaysMa;
	
	private BigDecimal twoFourtyDaysMa;
	
	private BigDecimal lineKvalue;
	
	private BigDecimal lineDvalue;
}
