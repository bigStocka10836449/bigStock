package com.bigstock.biz.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NewsDataInfo {
	private String id;
	private String time;
	private Integer type;

	private NewsData data;

	private Boolean important;
	private List<Remark> remark;

	private List<Integer> channel;
	private List<Object> tags;

	@JsonProperty("m_type")
	private Integer mType;
	private Integer action;

	private String date;
	private String hms;
	private String relevance;
}
