package com.bigstock.sharedComponent.infra;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CloseableHttpClientConfig {
	@Bean
	public CloseableHttpClient closeableHttpClient() {

	    PoolingHttpClientConnectionManager cm =
	            new PoolingHttpClientConnectionManager();

	    cm.setMaxTotal(50);
	    cm.setDefaultMaxPerRoute(20);

	    RequestConfig config = RequestConfig.custom()
	            .setConnectTimeout(10000)
	            .setConnectionRequestTimeout(10000)
	            .build();

	    return HttpClients.custom()
	            .setConnectionManager(cm)
	            .setDefaultRequestConfig(config)
	            .disableAutomaticRetries() // scheduler job → we control retry
	            .build();
	}
}
