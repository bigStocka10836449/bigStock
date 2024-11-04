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
	
	
	//--------------------------------個股每日交易明細---------------------------------
	@Bean
	public DirectExchange stockExchangeDetailExchange() {
		return new DirectExchange("StockExchangeDetailExchange", true, false);
	}

	@Bean
	public Queue stockExchangeDetailQueue() {
		return QueueBuilder.durable("StockExchangeDetailQueue").build();
	}

	@Bean
	public Binding stockExchangeDetailBinding() {
		return BindingBuilder.bind(stockExchangeDetailQueue()).to(stockExchangeDetailExchange()).withQueueName();
	}

	@Bean
	public DirectExchange stockExchangeDetailExchangeError() {
		return new DirectExchange("StockExchangeDetailExchangeError", true, false);
	}

	@Bean
	public Queue stockExchangeDetailQueueError() {
		return QueueBuilder.durable("StockExchangeDetailQueueError").build();
	}

	@Bean
	public Binding bindingStockExchangeDetailQueueError() {
		return BindingBuilder.bind(stockExchangeDetailQueueError()).to(stockExchangeDetailExchangeError()).withQueueName();
	}
	
	
}