package com.bigstock.gateway;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication(scanBasePackages = {"com.bigstock.gateway","com.bigstock.sharedComponent"})
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
@EnableCaching
public class GatewayApp {
	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(GatewayApp.class);
		app.run(args);
	}

}
