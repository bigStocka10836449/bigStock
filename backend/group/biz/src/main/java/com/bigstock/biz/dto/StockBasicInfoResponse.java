package com.bigstock.biz.dto;

import lombok.Data;

@Data
public class StockBasicInfoResponse {

    private String stockId;
    private String stockName;
    private String market; // TWSE / TPEX / null

    // 交易所資料
    private String mainBusiness;       // 主要經營業務
    private String industryCategory;   // 產業分類
    private String listingDate;        // yyyy-MM-dd（若抓得到）

    // 預留
    private String subIndustryCategory;

    // 財報資料（最新一期）
    private Long capitalStock;               // 股本（先用你 DB/財報的單位，市值乘法先照做）
    private Double netAssetValuePerShare;    // 每股淨值
    private Double earningsPerShare;         // EPS

    // 股價衍生：用「最新月份第一個交易日收盤價」
    private String marketCapPriceDate; // yyyy-MM-dd
    private Double marketCapPrice;     // close
    private Long marketCap;            // 市值
    private Double peRatio;            // 本益比

    // 近一年高低（日K close）
    private Double oneYearHigh;
    private Double oneYearLow;

    // 近三年高低（週K close，約 156 週）
    private Double threeYearHigh;
    private Double threeYearLow;

    // 均線狀態（先固定 0）
    private int maStatus;
}
