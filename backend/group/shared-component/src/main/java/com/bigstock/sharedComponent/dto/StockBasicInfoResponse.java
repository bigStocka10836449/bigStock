package com.bigstock.sharedComponent.dto;

import lombok.Data;

@Data
public class StockBasicInfoResponse {

    private String stockId;
    private String market; // TWSE / TPEX
    private String stockName;

    private String mainBusiness;
    private String industryCategory;
    private String listingDate; // yyyy-MM-dd (nullable)
}
