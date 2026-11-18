package com.bigstock.schedule.job.config;

import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
@EnableScheduling
public class JobRuntimeConfig {
	@Bean
	public TransactionTemplate jobTransactionTemplate(
			org.springframework.transaction.PlatformTransactionManager manager) {
		return new TransactionTemplate(manager);
	}

	@Bean("managedJobExecutor")
	public ThreadPoolTaskExecutor managedJobExecutor() {
		ThreadPoolTaskExecutor e = new ThreadPoolTaskExecutor();
		e.setCorePoolSize(2);
		e.setMaxPoolSize(2);
		e.setQueueCapacity(20);
		e.setThreadNamePrefix("bstock-job-");
		e.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
		e.initialize();
		return e;
	}
}
