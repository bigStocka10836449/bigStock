package com.bigstock.sharedComponent.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class DynamicFilterStockPriceCondition {
	String name;
	List<String> value;
	String operator;
}
