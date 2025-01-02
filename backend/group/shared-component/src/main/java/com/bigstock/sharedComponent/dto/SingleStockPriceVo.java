package com.bigstock.sharedComponent.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.Data;
@Data
public class SingleStockPriceVo {
	List<SingleStockDayPriceVo> singleStockDayPriceVos;
	List<SingleStockWeekPriceVo> singleStockWeekPriceVos;
	List<SingleStockMonthPriceVo> singleStockMonthPriceVos;
}
