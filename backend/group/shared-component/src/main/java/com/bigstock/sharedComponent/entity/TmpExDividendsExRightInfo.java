package com.bigstock.sharedComponent.entity;

import java.io.Serializable;
import java.util.Date;

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

@Entity
@Table(name = "tmp_ex_dividends_ex_right_info", schema = "bstock")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(TmpExDividendsExRightInfo.TmpExDividendsExRightInfoId.class)
public class TmpExDividendsExRightInfo {

    @Id
    @Column(name = "trading_day", nullable = true)
    private Date tradingDay;
    @Id
    @Column(name = "stock_code", nullable = true)
    private String stockCode;

    @Column(name = "limit_up", nullable = true)
    private String limitUp;

    @Column(name = "limit_down", nullable = true)
    private String limitDown;
    
    @Column(name = "reference_price", nullable = true)
    private String referencePrice;
    
    @Getter
	@Setter
    public static class TmpExDividendsExRightInfoId implements Serializable {
        /**
		 * 
		 */
		private static final long serialVersionUID = -5700132828663593976L;
		private Date tradingDay;
        private String stockCode;
    }
}
