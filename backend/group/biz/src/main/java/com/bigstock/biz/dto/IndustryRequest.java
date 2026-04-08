package com.bigstock.biz.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class IndustryRequest {
    @JsonProperty("Title")
    private List<String> title;

    @JsonProperty("Data")
    private List<IndustryData> data;
}
