package com.bigstock.biz.vo;

import lombok.Data;

@Data
public class QuarterlyProfitabilityVo {

    // Stock identity
    private String stockId;
    private String stockName;

    // Period
    private int year;       // 年 / Year
    private int quarter;    // 季 / Quarter

    // 1) 稅前純益 / Pretax Profit
    private Long pretaxProfit;

    // 2) 業外收支 / Non-operating Income / Expense
    private Long nonOperatingIncomeExpense;

    // 3) 業外佔淨利比(以「稅前純益」為分母) / Non-operating Ratio (%)
    private Double nonOperatingRatio;

    // 4) EPS / Earnings Per Share
    private Double earningsPerShare;
}
