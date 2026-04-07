package com.bigstock.biz.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class IndustryData {

    private String tag;

    @JsonProperty("tag_name")
    private String tagName;

    @JsonProperty("stock_codes")
    private String stockCodes;
}
