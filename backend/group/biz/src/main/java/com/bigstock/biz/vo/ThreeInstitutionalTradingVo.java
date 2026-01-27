package com.bigstock.biz.vo;

import lombok.Data;

@Data
public class ThreeInstitutionalTradingVo {

    private String stockCode;
    private String stockName;

    private long foreignBuy;
    private long foreignSell;

    private long investmentTrustBuy;
    private long investmentTrustSell;

    private long dealerBuy;
    private long dealerSell;

    private String tradeDate;  // yyyy-MM-dd
    private String market;     // TWSE / TPEX

    // 方便 App 直接用（可選）
    private long foreignNet;
    private long investmentTrustNet;
    private long dealerNet;
}