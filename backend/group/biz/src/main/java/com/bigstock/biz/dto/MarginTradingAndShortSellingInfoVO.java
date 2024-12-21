package com.bigstock.biz.dto;

import java.text.SimpleDateFormat;

import com.bigstock.sharedComponent.entity.MarginTradingAndShortSellingInfo;

import lombok.Data;

@Data
public class MarginTradingAndShortSellingInfoVO {

    private String tradingDay; // 格式化後的文字日期
    private String stockCode;

    private String marginPurchaseBalancePreviousDay;
    private String marginPurchase;
    private String marginSales;
    private String cashRedemption;
    private String marginPurchaseBalance;
    private String marginPurchaseQuota;
    private String shortSaleBalancePreviousDay;
    private String shortSale;
    private String shortConvering;
    private String stockRedemption;
    private String shortSaleBalance;
    private String shortSaleQuota;
    private String offsetting;

    // 可添加建構子或方法方便從 Entity 轉換
    public static MarginTradingAndShortSellingInfoVO fromEntity(MarginTradingAndShortSellingInfo entity) {
        MarginTradingAndShortSellingInfoVO vo = new MarginTradingAndShortSellingInfoVO();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd"); // 格式化日期格式
        vo.setTradingDay(formatter.format(entity.getTradingDay())); // 格式化日期
        vo.setStockCode(entity.getStockCode());
        vo.setMarginPurchaseBalancePreviousDay(entity.getMarginPurchaseBalancePreviousDay());
        vo.setMarginPurchase(entity.getMarginPurchase());
        vo.setMarginSales(entity.getMarginSales());
        vo.setCashRedemption(entity.getCashRedemption());
        vo.setMarginPurchaseBalance(entity.getMarginPurchaseBalance());
        vo.setMarginPurchaseQuota(entity.getMarginPurchaseQuota());
        vo.setShortSaleBalancePreviousDay(entity.getShortSaleBalancePreviousDay());
        vo.setShortSale(entity.getShortSale());
        vo.setShortConvering(entity.getShortConvering());
        vo.setStockRedemption(entity.getStockRedemption());
        vo.setShortSaleBalance(entity.getShortSaleBalance());
        vo.setShortSaleQuota(entity.getShortSaleQuota());
        vo.setOffsetting(entity.getOffsetting());
        return vo;
    }
}
