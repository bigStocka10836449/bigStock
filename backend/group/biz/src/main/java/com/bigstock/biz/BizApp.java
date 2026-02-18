package com.bigstock.biz;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication(scanBasePackages = {"com.bigstock.biz","com.bigstock.sharedComponent"})
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
@EnableCaching
public class BizApp {
	public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT+08:00"));

		SpringApplication.run(BizApp.class, args);
	}
}
