package com.bigstock.sharedComponent.dto;

import lombok.Data;

@Data
public class StockRevenueResponse {

    /** 個股代號 (e.g., 2330) */
    private String stockId;

    /** 個股名稱 */
    private String stockName;

    /** 月營收 */
    private long revenue;

    /** 月份 YYYY-MM (e.g., 2025-11) */
    private String date;

    /** 市場 TWSE / TPEX */
    private String market;
}
