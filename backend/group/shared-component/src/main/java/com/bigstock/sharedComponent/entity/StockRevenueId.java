package com.bigstock.sharedComponent.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/**
 * 複合主鍵： (revenue_month, market, stock_id)
 */
@Embeddable
public class StockRevenueId implements Serializable {

    @Column(name = "revenue_month", nullable = false)
    private LocalDate revenueMonth; // 用每月 1 號表示月份

    @Column(name = "market", nullable = false, length = 4)
    private String market; // TWSE / TPEX

    @Column(name = "stock_id", nullable = false, length = 10)
    private String stockId;

    public StockRevenueId() {}

    public StockRevenueId(LocalDate revenueMonth, String market, String stockId) {
        this.revenueMonth = revenueMonth;
        this.market = market;
        this.stockId = stockId;
    }

    public LocalDate getRevenueMonth() { return revenueMonth; }
    public void setRevenueMonth(LocalDate revenueMonth) { this.revenueMonth = revenueMonth; }

    public String getMarket() { return market; }
    public void setMarket(String market) { this.market = market; }

    public String getStockId() { return stockId; }
    public void setStockId(String stockId) { this.stockId = stockId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StockRevenueId that)) return false;
        return Objects.equals(revenueMonth, that.revenueMonth)
                && Objects.equals(market, that.market)
                && Objects.equals(stockId, that.stockId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(revenueMonth, market, stockId);
    }
}
