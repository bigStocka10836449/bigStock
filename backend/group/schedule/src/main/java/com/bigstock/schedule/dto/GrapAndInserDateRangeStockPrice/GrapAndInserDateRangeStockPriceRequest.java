package com.bigstock.schedule.dto.GrapAndInserDateRangeStockPrice;

import com.bigstock.schedule.annotation.DateRange;
import com.bigstock.sharedComponent.dto.DateRangeDto;

import lombok.Data;

@Data
public class GrapAndInserDateRangeStockPriceRequest {

	@DateRange
	private DateRangeDto dateRangeDto;

}
