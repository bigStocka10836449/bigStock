package com.bigstock.biz.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Remark {

    private String type;
    private String title;
    private String symbol;
    private String content;
    private Long id;
}
