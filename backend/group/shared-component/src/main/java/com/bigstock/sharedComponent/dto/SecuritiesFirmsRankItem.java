package com.bigstock.sharedComponent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SecuritiesFirmsRankItem {

    private Integer rank;

    private String securitiesFirms;

    /**
     * 買進股數
     */
    private Long buyAmount;

    /**
     * 賣出股數
     */
    private Long sellAmount;

    /**
     * 買賣超股數 = buyAmount - sellAmount
     */
    private Long netAmount;

    /**
     * 買進張數 = buyAmount / 1000
     */
    private Long buyLots;

    /**
     * 賣出張數 = sellAmount / 1000
     */
    private Long sellLots;

    /**
     * 買賣超張數 = netAmount / 1000
     */
    private Long netLots;
}