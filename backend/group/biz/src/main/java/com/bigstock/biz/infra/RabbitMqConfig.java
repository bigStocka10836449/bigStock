package com.bigstock.biz.infra;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {


	//---------------------------單一個股每次開盤收盤行情---------------------
	@Bean
	public DirectExchange singleStockPriceExchange() {
		return new DirectExchange("SingleStockPriceExchange", true, false);
	}

	@Bean
	public Queue singleStockPriceQueue() {
		return QueueBuilder.durable("SingleStockPriceQueue").build();
	}

	@Bean
	public Binding bindingSingleStockPrice() {
		return BindingBuilder.bind(singleStockPriceQueue()).to(singleStockPriceExchange()).withQueueName();
	}

	@Bean
	public DirectExchange singleStockPriceExchangeError() {
		return new DirectExchange("SingleStockPriceExchangeError", true, false);
	}

	@Bean
	public Queue singleStockPriceQueueError() {
		return QueueBuilder.durable("SingleStockPriceQueueError").build();
	}

	@Bean
	public Binding bindingSingleStockPriceError() {
		return BindingBuilder.bind(singleStockPriceQueueError()).to(singleStockPriceExchangeError()).withQueueName();
	}
	
	
	
	//-------------------持股分布----------------------------------------------------
	@Bean
	public DirectExchange shareholderStructureIncreaseExchange() {
		return new DirectExchange("ShareholderStructureIncreaseExchange", true, false);
	}

	@Bean
	public Queue shareholderStructureIncreaseQueue() {
		return QueueBuilder.durable("ShareholderStructureIncreaseQueue").build();
	}

	@Bean
	public Binding shareholderStructureIncreaseBinding() {
		return BindingBuilder.bind(shareholderStructureIncreaseQueue()).to(shareholderStructureIncreaseExchange()).withQueueName();
	}

	@Bean
	public DirectExchange shareholderStructureIncreaseExchangeError() {
		return new DirectExchange("ShareholderStructureIncreaseExchangeError", true, false);
	}

	@Bean
	public Queue shareholderStructureIncreaseQueueError() {
		return QueueBuilder.durable("ShareholderStructureIncreaseQueueError").build();
	}

	@Bean
	public Binding bindingSingleStockPriceQueueError() {
		return BindingBuilder.bind(shareholderStructureIncreaseQueueError()).to(shareholderStructureIncreaseExchangeError()).withQueueName();
	}
	
	
	//--------------------------------動態選股查詢---------------------------------
	@Bean
	public DirectExchange stockCodeFilterTypeExchange() {
		return new DirectExchange("StockCodeFilterTypeExchange", true, false);
	}

	@Bean
	public Queue stockCodeFilterTypeQueue() {
		return QueueBuilder.durable("StockCodeFilterTypeQueue").build();
	}

	@Bean
	public Binding stockCodeFilterTypeBinding() {
		return BindingBuilder.bind(stockCodeFilterTypeQueue()).to(stockCodeFilterTypeExchange()).withQueueName();
	}

	@Bean
	public DirectExchange stockCodeFilterTypeExchangeError() {
		return new DirectExchange("StockCodeFilterTypeExchangeError", true, false);
	}

	@Bean
	public Queue stockCodeFilterTypeQueueError() {
		return QueueBuilder.durable("StockCodeFilterTypeQueueError").build();
	}

	@Bean
	public Binding bindingStockCodeFilterTypeQueueError() {
		return BindingBuilder.bind(stockCodeFilterTypeQueueError()).to(stockCodeFilterTypeExchangeError()).withQueueName();
	}
	
	
}