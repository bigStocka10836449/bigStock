package com.bigstock.biz.dto;

import lombok.Data;

@Data
public class ThreeInstitutionalTradingResponse {

    /** Stock code (e.g., 2330, 00919, 006201) */
    private String stockCode;

    /** Stock name */
    private String stockName;

    /** Foreign investors total buy shares (TWSE: non-dealer + foreign dealer; TPEX: total) */
    private long foreignBuy;

    /** Foreign investors total sell shares */
    private long foreignSell;

    /** Investment trust buy shares */
    private long investmentTrustBuy;

    /** Investment trust sell shares */
    private long investmentTrustSell;

    /** Dealers total buy shares (TWSE: proprietary + hedge; TPEX: total) */
    private long dealerBuy;

    /** Dealers total sell shares */
    private long dealerSell;

    /** Trade date in yyyy-MM-dd */
    private String tradeDate;

    /** Market: TWSE / TPEX */
    private String market;
}
