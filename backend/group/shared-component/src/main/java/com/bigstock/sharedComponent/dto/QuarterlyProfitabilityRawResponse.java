package com.bigstock.sharedComponent.dto;

import lombok.Data;

@Data
public class QuarterlyProfitabilityRawResponse {

    private String stockId;
    private String stockName;

    /** TWSE / TPEX */
    private String market;

    private int year;
    private int quarter;

    private Long operatingProfit;              // 營業利益
    private Long nonOperatingIncomeExpense;    // 業外收支淨額
    private Double earningsPerShare;           // EPS
}
