package com.bigstock.sharedComponent.dto;

import lombok.Data;

@Data
public class MonthlyRevenueVo {
    private String stockId;     // 公司代號
    private String stockName;   // 公司名稱
    private Long revenue;       // 本月營收
    private String date;        // YYYY-MM（西元）
    private String market;      // TWSE / TPEX（方便你 debug，看來源）
}
