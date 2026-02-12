package com.bigstock.sharedComponent.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class StockBasicInfoId implements Serializable {

    @Column(name = "stock_id", nullable = false, length = 16)
    private String stockId;

    @Column(name = "market", nullable = false, length = 8)
    private String market; // TWSE / TPEX

    public StockBasicInfoId() {}

    public StockBasicInfoId(String stockId, String market) {
        this.stockId = stockId;
        this.market = market;
    }

    public String getStockId() { return stockId; }
    public void setStockId(String stockId) { this.stockId = stockId; }

    public String getMarket() { return market; }
    public void setMarket(String market) { this.market = market; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StockBasicInfoId that)) return false;
        return Objects.equals(stockId, that.stockId) && Objects.equals(market, that.market);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stockId, market);
    }
}
