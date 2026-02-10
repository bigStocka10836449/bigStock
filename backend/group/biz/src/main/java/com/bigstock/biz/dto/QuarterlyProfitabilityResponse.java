package com.bigstock.biz.dto;

import lombok.Data;

@Data
public class QuarterlyProfitabilityResponse {

    private String stockId;
    private String stockName;

    private int year;
    private int quarter;

    private Long pretaxProfit;                 // 稅前純益
    private Long nonOperatingIncomeExpense;    // 業外收支淨額
    private Double nonOperatingRatio;          // 業外佔淨利比(以稅前純益為分母) (%)
    private Double earningsPerShare;           // EPS
}
