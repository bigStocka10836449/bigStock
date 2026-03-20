package com.bigstock.sharedComponent.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class RankingResponse {

    private int rank;              // 1-based ranking
    private String stockCode;
    private BigDecimal changeRate;
    private BigDecimal change;
    private Long tradingVolume;
    private String stockName;
    private BigDecimal openingPrice;
    private BigDecimal closingPrice;
    private BigDecimal limitUp;
    private BigDecimal limitDown;
}
