package com.bigstock.sharedComponent.entity;

import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "stock_info_tag", schema = "bstock")
@Data
public class StockInfoTag {

    @Id
    @Column(name = "stock_code", nullable = false)
    private String stockCode;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "tags", columnDefinition = "text[]")
    private String[] tags;
}
