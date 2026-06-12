package com.bigstock.sharedComponent.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class SecuritiesFirmsRankResult {

    /**
     * DONE / NOT_READY / PROCESSING / FAILED
     */
    private String status;

    private String stockCode;

    /**
     * 目前先固定是 FIXED
     */
    private String rangeType;

    /**
     * 1 / 3 / 5 / 10 / 20 / 60 / 120
     */
    private Integer rangeDays;

    private String startDate;

    private String endDate;

    private Integer actualTradingDays;

    /**
     * REDIS / DB / SCHEDULE
     */
    private String source;

    private String message;

    private List<SecuritiesFirmsRankItem> buyTop15 = new ArrayList<>();

    private List<SecuritiesFirmsRankItem> sellTop15 = new ArrayList<>();
}