package com.bigstock.sharedComponent.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DynamicFilterStockCodeVo {

	String sessionId;
	String aspect;
	List<DynamicFilterStockPriceCondition> conditions;

}
