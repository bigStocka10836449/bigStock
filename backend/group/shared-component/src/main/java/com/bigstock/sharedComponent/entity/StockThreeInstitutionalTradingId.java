package com.bigstock.sharedComponent.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;
//複合主鍵
@Embeddable
public class StockThreeInstitutionalTradingId implements Serializable {

    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;

    @Column(name = "market", nullable = false, length = 4)
    private String market; // TWSE / TPEX

    @Column(name = "stock_code", nullable = false, length = 10)
    private String stockCode;

    public StockThreeInstitutionalTradingId() {}

    public StockThreeInstitutionalTradingId(LocalDate tradeDate, String market, String stockCode) {
        this.tradeDate = tradeDate;
        this.market = market;
        this.stockCode = stockCode;
    }

    public LocalDate getTradeDate() { return tradeDate; }
    public void setTradeDate(LocalDate tradeDate) { this.tradeDate = tradeDate; }

    public String getMarket() { return market; }
    public void setMarket(String market) { this.market = market; }

    public String getStockCode() { return stockCode; }
    public void setStockCode(String stockCode) { this.stockCode = stockCode; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StockThreeInstitutionalTradingId that)) return false;
        return Objects.equals(tradeDate, that.tradeDate)
                && Objects.equals(market, that.market)
                && Objects.equals(stockCode, that.stockCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tradeDate, market, stockCode);
    }
}