package com.bigstock.sharedComponent.entity;

import java.io.Serializable;
import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tmp_trade_volume_info", schema = "bstock")
@Data
@IdClass(TradeVolumeInfo.TradeVolumeInfoId.class)
public class TradeVolumeInfo {

    @Id
    @Column(name = "stock_code", nullable = false)
    private String stockCode;

    @Id
    @Column(name = "trading_day", nullable = false)
    private Date tradingDay;

    @Column(name = "trade_volume")
    private String tradeVolume;
    
    @Getter
	@Setter
    public static class TradeVolumeInfoId implements Serializable {

		private static final long serialVersionUID = -5963859583431440361L;

		private String stockCode;

        private Date tradingDay;
    }
}
