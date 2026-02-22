package com.bigstock.biz.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RankingResponse {

    private int rank;              // 1-based ranking
    private String stockCode;
    private Double changeRate;
}
