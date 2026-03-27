package com.bigstock.schedule.infra;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ExecutorConfig {
	@Bean(name = "infraExecutor")
	public ExecutorService schedulerExecutor() {

	    ThreadFactory factory = r -> {

	        Thread t = new Thread(r);
	        t.setName("infra-cleanup-" + t.getId());
	        t.setDaemon(true);
	        return t;
	    };

	    return new ThreadPoolExecutor(
	            1,                      // core threads
	            2,                      // max threads
	            60,
	            TimeUnit.SECONDS,
	            new LinkedBlockingQueue<>(50),
	            factory,
	            new ThreadPoolExecutor.DiscardPolicy() // do NOT block refresh
	    );
	}
}
