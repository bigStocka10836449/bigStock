package com.bigstock.sharedComponent.dto;

import lombok.Data;

@Data
public class QuarterlyFinancialResponse {

    private String stockId;
    private String stockName;

    /** TWSE / TPEX */
    private String market;

    private int year;
    private int quarter;

    /** 1 / 4 / 7 / 10 */
    private int periodStartMonth;

    /** 3 / 6 / 9 / 12 */
    private int periodEndMonth;

    /** TWD */
    private String unit;

    // Income statement (cumulative)
    private Long operatingRevenue;              // 營業收入
    private Long operatingProfit;               // 營業利益
    private Long nonOperatingIncomeExpense;     // 營業業外收支淨額
    private Long netProfitAfterTax;             // 稅後純益或稅後淨利(淨損)

    // Capital & per-share
    private Long capitalStockEndPeriod;         // 本期末股本
    private Double earningsPerShare;            // 每股盈餘 (保留 2 位)
    private Double netAssetValuePerShare;       // 每股淨值 (保留 2 位)

    // Ratios (% , keep 2 decimals)
    private Double quickRatio;                  // 速動比率
    private Double currentRatio;                // 流動比率
}
