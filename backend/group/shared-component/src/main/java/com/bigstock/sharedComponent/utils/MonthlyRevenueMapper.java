package com.bigstock.sharedComponent.utils;

import com.bigstock.sharedComponent.dto.MonthlyRevenueVo;
import com.bigstock.sharedComponent.dto.StockRevenueResponse;

public final class MonthlyRevenueMapper {

    private MonthlyRevenueMapper() {}

    public static StockRevenueResponse toStockRevenueResponse(MonthlyRevenueVo vo) {
        StockRevenueResponse dto = new StockRevenueResponse();
        dto.setStockId(safe(vo.getStockId()));
        dto.setStockName(safe(vo.getStockName()));
        dto.setRevenue(vo.getRevenue());
        dto.setDate(safe(vo.getDate()));     // yyyy-MM
        dto.setMarket(safe(vo.getMarket())); // TWSE / TPEX
        return dto;
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }
}