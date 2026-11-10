package com.bigstock.biz.dto;

import lombok.Data;

@Data
public class SingleStockIntradayPriceBizVo {

    private String stockCode;

    /**
     * 5m  = 5 分 K
     * 60m = 60 分 K
     */
    private String period;
}