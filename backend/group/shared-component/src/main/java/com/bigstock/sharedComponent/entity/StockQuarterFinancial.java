package com.bigstock.sharedComponent.entity;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "stock_quarter_financial", schema = "bstock")
@IdClass(StockQuarterFinancial.StockQuarterFinancialId.class)
public class StockQuarterFinancial {

    // Composite Primary Key

    @Id
    @Column(name = "stock_id", nullable = false, length = 16)
    private String stockId;

    @Id
    @Column(name = "market", nullable = false, length = 8)
    private String market;

    @Id
    @Column(name = "year", nullable = false)
    private Integer year;

    @Id
    @Column(name = "quarter", nullable = false)
    private Integer quarter;

    // Stock Information

    @Column(name = "stock_name", nullable = false, length = 64)
    private String stockName;

    @Column(name = "period_start_month", nullable = false)
    private Integer periodStartMonth;

    @Column(name = "period_end_month", nullable = false)
    private Integer periodEndMonth;

    @Builder.Default
    @Column(name = "unit", nullable = false, length = 8)
    private String unit = "TWD";

    // Income Statement

    @Column(name = "operating_revenue")
    private Long operatingRevenue;

    @Column(name = "operating_profit")
    private Long operatingProfit;

    @Column(name = "non_operating_income_expense")
    private Long nonOperatingIncomeExpense;

    @Column(name = "net_profit_after_tax")
    private Long netProfitAfterTax;

    // Capital & Per-share

    @Column(name = "capital_stock_end_period")
    private Long capitalStockEndPeriod;

    @Column(name = "earnings_per_share", precision = 18, scale = 2)
    private BigDecimal earningsPerShare;

    @Column(name = "net_asset_value_per_share", precision = 18, scale = 2)
    private BigDecimal netAssetValuePerShare;

    // Ratios

    @Column(name = "quick_ratio", precision = 18, scale = 2)
    private BigDecimal quickRatio;

    @Column(name = "current_ratio", precision = 18, scale = 2)
    private BigDecimal currentRatio;

    @Column(name = "depn", precision = 18, scale = 2)
    private BigDecimal depn;

    // Audit Columns

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Composite Primary Key Class

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockQuarterFinancialId implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private String stockId;
        private String market;
        private Integer year;
        private Integer quarter;
    }
}