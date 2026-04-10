package com.bigstock.biz.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NewsData {

    private String pic;
    private String title;
    private String content;
    private String source;

    @JsonProperty("source_link")
    private String sourceLink;
}
