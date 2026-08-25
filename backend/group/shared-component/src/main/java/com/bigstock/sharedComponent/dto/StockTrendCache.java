package com.bigstock.sharedComponent.dto;

import java.time.LocalDate;
import java.util.List;

import com.bigstock.sharedComponent.enums.TrendRegime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockTrendCache {

    private String stockCode;

    private LocalDate lastDay;

    private TrendRegime trendRegime;

    private List<LocalDate> dates;

    /**
     * cumulative return from first day
     */
    private List<Double> returns;

    /**
     * daily return
     */
    private List<Double> dailyReturns;

    private List<Double> volumeRatio;

    private List<Double> k;

    private List<Double> d;

    private List<Double> rsv;

    /**
     * price vs moving average
     */
    private List<Double> bias5;
    private List<Double> bias10;
    private List<Double> bias20;
    private List<Double> bias60;
    private List<Double> bias120;
    private List<Double> bias240;

    /**
     * moving average structure
     */
    private List<Double> gap5_10;
    private List<Double> gap10_20;
    private List<Double> gap20_60;
    private List<Double> gap60_120;
    private List<Double> gap120_240;
}