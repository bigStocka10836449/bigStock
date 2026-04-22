package com.bigstock.biz.dto;

import java.util.List;

import com.bigstock.sharedComponent.entity.FinancialCalendar;

import lombok.Data;

@Data
public class FinancialCalendarImportChunkRequest {
	List<FinancialCalendar> batch;
}
