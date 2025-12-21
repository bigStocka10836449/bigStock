package com.bigstock.biz;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication(scanBasePackages = {"com.bigstock.biz","com.bigstock.sharedComponent"})
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
@EnableCaching
public class BizApp {
	public static void main(String[] args) {
		SpringApplication.run(BizApp.class, args);
	}
}
