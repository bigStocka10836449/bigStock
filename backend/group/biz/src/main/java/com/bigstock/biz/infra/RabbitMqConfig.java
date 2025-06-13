package com.bigstock.biz.infra;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
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
	
	
	
	
	//---------------------------SSE 版本-----------------------------------
	//---------------------------單一個股每次開盤收盤行情---------------------
	@Bean
	public TopicExchange sseSingleStockPriceExchange() {
		return new TopicExchange("SSESingleStockPriceExchange", true, false);
	}

	@Bean
	public Queue sseSingleStockPriceQueue() {
		return QueueBuilder.durable("SSESingleStockPriceQueue").build();
	}

	@Bean
	public Binding sseBindingSingleStockPrice() {
		return BindingBuilder.bind(sseSingleStockPriceQueue()).to(sseSingleStockPriceExchange()).with("stock.price.*");
	}

	@Bean
	public DirectExchange sseSingleStockPriceExchangeError() {
		return new DirectExchange("SSESingleStockPriceExchangeError", true, false);
	}

	@Bean
	public Queue sseSingleStockPriceQueueError() {
		return QueueBuilder.durable("SSESingleStockPriceQueueError").build();
	}

	@Bean
	public Binding sseBindingSingleStockPriceError() {
		return BindingBuilder.bind(sseSingleStockPriceQueueError()).to(sseSingleStockPriceExchangeError()).withQueueName();
	}
	
	
	
	
	
	
	
	//-------------------Server sent envet 版本 持股分布----------------------------------------------------
	@Bean
	public TopicExchange sseShareholderStructureIncreaseExchange() {
		return new TopicExchange("SSEShareholderStructureIncreaseExchange", true, false);
	}

	@Bean
	public Queue sseShareholderStructureIncreaseQueue() {
		return QueueBuilder.durable("SSEShareholderStructureIncreaseQueue").build();
	}

	@Bean
	public Binding sseShareholderStructureIncreaseBinding() {
		return BindingBuilder.bind(sseShareholderStructureIncreaseQueue()).to(sseShareholderStructureIncreaseExchange()).with("stock.shareholder.*");
	}

	@Bean
	public DirectExchange sseShareholderStructureIncreaseExchangeError() {
		return new DirectExchange("SSEShareholderStructureIncreaseExchangeError", true, false);
	}

	@Bean
	public Queue sseShareholderStructureIncreaseQueueError() {
		return QueueBuilder.durable("SSEShareholderStructureIncreaseQueueError").build();
	}

	@Bean
	public Binding sseBindingSingleStockPriceQueueError() {
		return BindingBuilder.bind(sseShareholderStructureIncreaseQueueError()).to(sseShareholderStructureIncreaseExchangeError()).withQueueName();
	}
	
	
	//--------------------------------動態選股查詢---------------------------------
	@Bean
	public TopicExchange sseStockCodeFilterTypeExchange() {
		return new TopicExchange("SSEStockCodeFilterTypeExchange", true, false);
	}

	@Bean
	public Queue sseStockCodeFilterTypeQueue() {
		return QueueBuilder.durable("SSEStockCodeFilterTypeQueue").build();
	}

	@Bean
	public Binding sseStockCodeFilterTypeBinding() {
		return BindingBuilder.bind(sseStockCodeFilterTypeQueue()).to(sseStockCodeFilterTypeExchange()).with("stock.filter.*");
	}

	@Bean
	public DirectExchange sseStockCodeFilterTypeExchangeError() {
		return new DirectExchange("SSEStockCodeFilterTypeExchangeError", true, false);
	}

	@Bean
	public Queue sseStockCodeFilterTypeQueueError() {
		return QueueBuilder.durable("SSEStockCodeFilterTypeQueueError").build();
	}

	@Bean
	public Binding sseBindingStockCodeFilterTypeQueueError() {
		return BindingBuilder.bind(sseStockCodeFilterTypeQueueError()).to(sseStockCodeFilterTypeExchangeError()).withQueueName();
	}
	
	
	//------socketIo 分散式情況下實作推播給其他真正有client資訊的jvm
	@Bean
	public TopicExchange socketExchange() {
		return new TopicExchange("exchange.socketio");
	}

	@Bean
	public Queue socketPushQueue() {
		return new Queue("queue.socketio.push");
	}

	@Bean
	public Binding socketPushBinding() {
		return BindingBuilder.bind(socketPushQueue()).to(socketExchange()).with("push");
	}
}