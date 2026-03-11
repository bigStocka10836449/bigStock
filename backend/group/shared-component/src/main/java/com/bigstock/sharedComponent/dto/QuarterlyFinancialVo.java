package com.bigstock.sharedComponent.dto;

import lombok.Data;

@Data
public class QuarterlyFinancialVo {

    // Stock identity
    private String stockId;
    private String stockName;
    private String market; // TWSE / TPEX

    // Period (cumulative)
    private int year;
    private int quarter;
    private int periodStartMonth;
    private int periodEndMonth;

    // Common
    private String unit; // TWD

    // Income statement
    private Long operatingRevenue;                 // 營業收入
    private Long operatingProfit;                  // 營業利益
    private Long nonOperatingIncomeExpense;        // 營業外收支淨額
    private Long netProfitAfterTax;                // 稅後純益(淨損)

    // Capital & per-share
    private Long capitalStockEndPeriod;            // 期末股本
    private Double earningsPerShare;               // EPS
    private Double netAssetValuePerShare;          // 每股淨值

    // Ratio
    private Double quickRatio;                     // 速動比
    private Double currentRatio;                   // 流動比率(%)
    private Double depn;                           // 淨值佔總資產比(1 - depn = 負債比)          
}
