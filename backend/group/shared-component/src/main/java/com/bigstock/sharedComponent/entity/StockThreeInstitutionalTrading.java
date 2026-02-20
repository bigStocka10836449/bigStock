package com.bigstock.sharedComponent.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "stock_three_institutional_trading", schema = "bstock")
public class StockThreeInstitutionalTrading {

    @EmbeddedId
    private StockThreeInstitutionalTradingId id;

    @Column(name = "stock_name", nullable = false, columnDefinition = "text")
    private String stockName;

    @Column(name = "foreign_buy", nullable = false)
    private long foreignBuy;

    @Column(name = "foreign_sell", nullable = false)
    private long foreignSell;

    @Column(name = "investment_trust_buy", nullable = false)
    private long investmentTrustBuy;

    @Column(name = "investment_trust_sell", nullable = false)
    private long investmentTrustSell;

    @Column(name = "dealer_buy", nullable = false)
    private long dealerBuy;

    @Column(name = "dealer_sell", nullable = false)
    private long dealerSell;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public StockThreeInstitutionalTrading() {}

    public StockThreeInstitutionalTrading(StockThreeInstitutionalTradingId id) {
        this.id = id;
    }

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        this.createdAt = (this.createdAt == null) ? now : this.createdAt;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public StockThreeInstitutionalTradingId getId() { return id; }
    public void setId(StockThreeInstitutionalTradingId id) { this.id = id; }

    public String getStockName() { return stockName; }
    public void setStockName(String stockName) { this.stockName = stockName; }

    public long getForeignBuy() { return foreignBuy; }
    public void setForeignBuy(long foreignBuy) { this.foreignBuy = foreignBuy; }

    public long getForeignSell() { return foreignSell; }
    public void setForeignSell(long foreignSell) { this.foreignSell = foreignSell; }

    public long getInvestmentTrustBuy() { return investmentTrustBuy; }
    public void setInvestmentTrustBuy(long investmentTrustBuy) { this.investmentTrustBuy = investmentTrustBuy; }

    public long getInvestmentTrustSell() { return investmentTrustSell; }
    public void setInvestmentTrustSell(long investmentTrustSell) { this.investmentTrustSell = investmentTrustSell; }

    public long getDealerBuy() { return dealerBuy; }
    public void setDealerBuy(long dealerBuy) { this.dealerBuy = dealerBuy; }

    public long getDealerSell() { return dealerSell; }
    public void setDealerSell(long dealerSell) { this.dealerSell = dealerSell; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}