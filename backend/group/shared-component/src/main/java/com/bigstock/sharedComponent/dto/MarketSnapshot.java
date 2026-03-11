package com.bigstock.sharedComponent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarketSnapshot {
    private long ts;          // epoch millis
    private double sp500;
    private double nasdaq;
    private double dow;
    private String data;
}