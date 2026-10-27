package com.bigstock.sharedComponent.dto;

import java.util.Map;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class YearlyRevenueResponse {

    private int year;

    /**
     * Key: 2026-01, 2026-02 ...
     * Value: MonthlyRevenueVo list
     */
    private Map<String, List<MonthlyRevenueVo>> months;
}
