package com.bigstock.sharedComponent.dto;

import java.util.Date;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class SingleStockPriceBizVo {
	
	@Schema(name = "stockCode", description = "股票代號")
	private String stockCode;
	@Schema(name = "searchStartDate", description = "查詢起始日期")
	private Date searchStartDate;
	@Schema(name = "searchEndDate", description = "查詢結束日期")
	private Date searchEndDate;
}
