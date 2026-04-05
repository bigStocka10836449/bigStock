package com.bigstock.sharedComponent.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "stock_info_tag_mapping", schema = "bstock")
@Data

public class StockInfoTagMapping {
    @Id
    @Column(name = "tag", nullable = false)
    private String tag;

    @Column(name = "tag_name")
    private String tagName;
}
