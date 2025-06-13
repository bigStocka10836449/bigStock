package com.bigstock.sharedComponent.rabbitmq;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeoutException;

import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import com.google.common.collect.Maps;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.Delivery;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class RabbitMqService {

	@Value("${spring.rabbitmq.host}")
	String rabbitMqHost;
	@Value("${spring.rabbitmq.port}")
	String rabbitMqPort;
	
	@Value("${spring.rabbitmq.username}")
	String rabbitMqUsername;
	@Value("${spring.rabbitmq.password}")
	String rabbitMqPassword;
	private final RabbitTemplate rabbitTemplate;

	private static final Map<String, Object> tmpStoredReceivedData = Maps.newConcurrentMap();

	public void sendMessage(Object message, String exchange, String routeKey, String sessionId) {
		MessagePostProcessor messagePostProcessor = messageProperties -> {
			messageProperties.getMessageProperties().setHeader("sessionId", sessionId);
			return messageProperties;
		};
		rabbitTemplate.convertAndSend(exchange, routeKey, message, messagePostProcessor, new CorrelationData());
	}
	
	public void sendMessage(Object message, String exchange, String routeKey, String UUidString, String exchangePrefix,
			String queueNamePrefix) {
		MessagePostProcessor messagePostProcessor = messageProperties -> {
			messageProperties.getMessageProperties().setHeader("UUID", UUidString);
			messageProperties.getMessageProperties().setHeader("sendQueueName",
					String.join("-", queueNamePrefix, UUidString));
			messageProperties.getMessageProperties().setHeader("sendExchangeName",
					String.join("-", exchangePrefix, UUidString));
			return messageProperties;
		};
		rabbitTemplate.convertAndSend(exchange, routeKey, message, messagePostProcessor, new CorrelationData());
	}

//	public void sendMessage(String exchange, String routeKey, String UUidString) {
//		rabbitTemplate.convertAndSend("receiveTestExchange", null, UUidString, new CorrelationData());
//	}

	public String createConsumer(CountDownLatch latch, String queueName, String exchangeName)
			throws IOException, TimeoutException {
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost(rabbitMqHost); // 或者你的 RabbitMQ 服务器的主机名
		factory.setPort(Integer.valueOf(rabbitMqPort) ); // AMQP 协议的默认端口
		String UUIDString = UUID.randomUUID().toString();
		factory.setUsername(rabbitMqUsername);
		factory.setPassword(rabbitMqPassword);
		Connection connection = null;
		Channel channel = null;

		connection = factory.newConnection();
		channel = connection.createChannel();
		channel.queueDeclare(String.join("-", queueName, UUIDString), false, false, true, null);
		channel.exchangeDeclare(String.join("-", exchangeName, UUIDString), BuiltinExchangeType.DIRECT.getType(), false,
				false, null);
		// 将现有队列绑定到现有交换机上
		// 使用AtomicReference包装Channel
		channel.queueBind(String.join("-", queueName, UUIDString), String.join("-", exchangeName, UUIDString),
				UUIDString);

		// 定义消息消费者

		// 开始消费消息
		channel.basicConsume(String.join("-", queueName, UUIDString), true,
				new GatewayDeliverCallback(connection, channel, latch, String.join("-", queueName, UUIDString), String.join("-", exchangeName, UUIDString)), consumerTag -> {
				});
		return UUIDString;
	}

	public Object getValueFromTmpStoredReceivedData(String key) {
		Object result = tmpStoredReceivedData.get(key);
		tmpStoredReceivedData.remove(key);
		return result;
	}

	class GatewayDeliverCallback implements DeliverCallback {

		public Connection connection;

		public Channel channel;

		public CountDownLatch latch;

		private String queueName;

		private String exchangeName;

		public GatewayDeliverCallback(Connection connection, Channel channel, CountDownLatch latch, String queueName,
				String exchangeName) {
			this.channel = channel;
			this.connection = connection;
			this.latch = latch;
			this.queueName = queueName;
			this.exchangeName = exchangeName;
		}

		@Override
		public void handle(String consumerTag, Delivery delivery) throws IOException {
			// TODO Auto-generated method stub
			try {
				String uuid = delivery.getProperties().getHeaders().get("UUID").toString();
				String message = new String(delivery.getBody(), "UTF-8");
				// 記錄到暫存的Map，讓Gateway Controller取得資料
				tmpStoredReceivedData.put(uuid, message);
				channel.queueUnbind(queueName, exchangeName, uuid);
				channel.exchangeDelete(exchangeName);
				// 处理完消息后取消绑定指定的路由键
				latch.countDown();
			} catch (Exception ex) {
				ex.printStackTrace();
			} finally {
				if (channel != null) {
					try {
						channel.close();
					} catch (IOException | TimeoutException e) {
						e.printStackTrace();
					}
				}
				if (connection != null) {
					try {
						connection.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}

	}
}
