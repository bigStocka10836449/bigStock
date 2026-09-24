package com.bigstock.sharedComponent.entity;

import java.time.LocalDateTime;

import com.bigstock.sharedComponent.entity.StockIntradayPrice.StockIntradayPriceId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "stock_intraday_price", schema = "bstock")
@IdClass(StockIntradayPriceId.class)
public class StockIntradayPrice {

    @Id
    @Column(name = "stock_code")
    private String stockCode;

    @Id
    @Column(name = "trading_time")
    private LocalDateTime tradingTime;

    @Id
    @Column(name = "period")
    private String period;

    @Column(name = "opening_price")
    private Double openingPrice;

    @Column(name = "closing_price")
    private Double closingPrice;

    @Column(name = "high_price")
    private Double highPrice;

    @Column(name = "low_price")
    private Double lowPrice;

    @Column(name = "trading_volume")
    private Long tradingVolume;

    @Column(name = "five_ma")
    private Double fiveMa;

    @Column(name = "ten_ma")
    private Double tenMa;

    @Column(name = "twenty_ma")
    private Double twentyMa;

    @Column(name = "sixty_ma")
    private Double sixtyMa;

    @Column(name = "line_rsv_value")
    private Double lineRsvValue;

    @Column(name = "line_k_value")
    private Double lineKValue;

    @Column(name = "line_d_value")
    private Double lineDValue;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public class StockIntradayPriceId {

        private String stockCode;

        private LocalDateTime tradingTime;

        private String period;
    }
}