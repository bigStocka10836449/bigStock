package com.bigstock.sharedComponent.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "stock_basic_info", schema = "bstock")
public class StockBasicInfo {

    @EmbeddedId
    private StockBasicInfoId id;

    @Column(name = "stock_name", nullable = false, columnDefinition = "text")
    private String stockName;

    @Column(name = "main_business", columnDefinition = "text")
    private String mainBusiness;

    @Column(name = "industry_category", columnDefinition = "text")
    private String industryCategory;

    @Column(name = "listing_date")
    private LocalDate listingDate; // yyyy-MM-dd

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public StockBasicInfo() {}

    public StockBasicInfo(StockBasicInfoId id) {
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

    public StockBasicInfoId getId() { return id; }
    public void setId(StockBasicInfoId id) { this.id = id; }

    public String getStockName() { return stockName; }
    public void setStockName(String stockName) { this.stockName = stockName; }

    public String getMainBusiness() { return mainBusiness; }
    public void setMainBusiness(String mainBusiness) { this.mainBusiness = mainBusiness; }

    public String getIndustryCategory() { return industryCategory; }
    public void setIndustryCategory(String industryCategory) { this.industryCategory = industryCategory; }

    public LocalDate getListingDate() { return listingDate; }
    public void setListingDate(LocalDate listingDate) { this.listingDate = listingDate; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
