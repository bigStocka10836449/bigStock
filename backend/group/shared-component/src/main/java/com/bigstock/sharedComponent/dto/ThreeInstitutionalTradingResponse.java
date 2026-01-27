package com.bigstock.sharedComponent.dto;

import lombok.Data;

@Data
public class ThreeInstitutionalTradingResponse {

    /** 個股編號 (e.g., 2330, 00919, 006201) */
    private String stockCode;

    /** 個股名稱 */
    private String stockName;

    /** 外資買超 */
    private long foreignBuy;

    /** 外資賣超 */
    private long foreignSell;

    /** 投信買超 */
    private long investmentTrustBuy;

    /** 投信賣超 */
    private long investmentTrustSell;

    /** 自營商買超 */
    private long dealerBuy;

    /** 自營商賣超 */
    private long dealerSell;

    /** 資料日期 YYYY-MM-DD */
    private String tradeDate;

    /** 上市、上櫃(暫時不含興櫃) */
    private String market;
}
