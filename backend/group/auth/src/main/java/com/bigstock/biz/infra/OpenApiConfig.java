package com.bigstock.biz.infra;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class OpenApiConfig {


	@Bean
	public OpenAPI customOpenAPI() {
		return new OpenAPI()
				.addServersItem(new Server().url("http://127.0.0.1:10093"))
				.info(new Info().title("Auth Service API").version("v1"));
	}

	@Bean
	public GroupedOpenApi authApi() {
		return GroupedOpenApi.builder().group("auth").pathsToMatch("/auth/**").build();
	}
}
