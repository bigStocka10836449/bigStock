package com.bigstock.sharedComponent.entity;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "financial_calendar", schema = "bstock")
public class FinancialCalendar{

	@Id
	@Column(name = "id")
	private String id;

	@Column(name = "month")
	private String month;

	@Column(name = "year")
	private String year;

	@Column(name = "event_date")
	private Date eventDate;

	private String title;

	@Column(name = "data_type")
	private String dataType;

	@Column(name = "data_type_name")
	private String dataTypeName;

	@Column(name = "article_id")
	private Long articleId;

	@Column(name = "hyper_link")
	private String hyperLink;

	@Column(name = "soruce_platfont") //
	private String sorucePlatfont;

}