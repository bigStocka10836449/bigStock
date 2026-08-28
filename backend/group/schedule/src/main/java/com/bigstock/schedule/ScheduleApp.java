package com.bigstock.schedule;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.FilterType;

@SpringBootApplication()
@ComponentScan(basePackages = { "com.bigstock.schedule", "com.bigstock.sharedComponent" }, excludeFilters = {
		@ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.bigstock\\.sharedComponent\\.rabbitmq\\..*"),
		@ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.bigstock\\.sharedComponent\\.jaeger\\..*") })
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
@EnableCaching
public class ScheduleApp {
	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone("GMT+08:00"));
		SpringApplication app = new SpringApplication(ScheduleApp.class);
		app.run(args);
	}

}
