package com.bigstock.biz.MqListener;

import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.List;

import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.bigstock.biz.service.BizService;
import com.bigstock.sharedComponent.dto.DynamicFilterStockCodeVo;
import com.bigstock.sharedComponent.dto.SingleStockPriceBizVo;
import com.bigstock.sharedComponent.dto.SingleStockPriceVo;
import com.bigstock.sharedComponent.dto.StockInfoVo;
import com.bigstock.sharedComponent.dto.StructureContinueIncreaseVo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class BigStockRabbitMqListener {

	public final BizService bizService;

//	private static final SsiInfoDataAccessMapper mapper = Mappers.getMapper(SsiInfoDataAccessMapper.class);
//	private static final String URL_401_PREFIX = "stock/deliverySSI";
//
//	private static final String URL_001_PREFIX = "stock/delivery001";

//	private static final String URL_TRANSFER_DON_PREFIX = "dummy/bd/settlement";
//
//	private static final String URL_DUMMY_ICB = "dummy/icb";

//	private final RestTemplate restTemplate;

	private final RabbitTemplate rabbitTemplate;

//	private final SsiInfoRepository ssiInfoRepository;
//
//	private final SequenceGenerator sequenceGenerator;
//
//	@Qualifier(value = "xsdSchema")
//	private final Map<String, Schema> bcssSchemaMap;
//
//	private final XSDValidate xadValidate;

//	@Value("${bcss.url-redirection}")
//	private String urlRedirection;

	@Value("${server.port}")
	private String serverPort;

//	@RabbitListener(queues = "SingleStockPriceQueue")
	@RabbitListener(bindings = @QueueBinding(value = @Queue(value = "SingleStockPriceQueue"), exchange = @Exchange(value = "SingleStockPriceExchange", type = ExchangeTypes.DIRECT), // 这里指定交换机类型为
																																														// TOPIC
			key = "SingleStockPriceQueue" // 这里指定 routing key
	), ackMode = "AUTO")
	public void receiveTransferDoneMessage(@Payload String jsonMessage,
			@Header(name = "UUID", required = false) String uuid,
			@Header(name = "sendQueueName", required = false) String sendQueueName,
			@Header(name = "sendExchangeName", required = false) String sendExchangeName)
	{
		try {
			ObjectMapper objectMapper = new ObjectMapper();
			objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd"));
			SingleStockPriceBizVo singleStockPriceBizVo = objectMapper.readValue(jsonMessage,
					SingleStockPriceBizVo.class);

			MessagePostProcessor messagePostProcessor = messageProperties -> {
				messageProperties.getMessageProperties().setHeader("UUID", uuid);
				return messageProperties;
			};
			SingleStockPriceVo vo = bizService.getSingleStockPrices(singleStockPriceBizVo.getStockCode(),
					singleStockPriceBizVo.getSearchStartDate(), singleStockPriceBizVo.getSearchEndDate());
			
			String voString =
					objectMapper.writeValueAsString(vo);
			rabbitTemplate.convertAndSend(sendExchangeName, uuid, voString, messagePostProcessor,
					new CorrelationData());
//		rabbitTemplate.convertAndSend("sendExchange", message, message,new CorrelationData());
		} catch (Exception e) {
			log.error(e.getMessage(),e);
			rabbitTemplate.convertAndSend("SingleStockPriceExchangeError", "SingleStockPriceQueueError", jsonMessage);
		}
	}

	@RabbitListener(bindings = @QueueBinding(value = @Queue(value = "ShareholderStructureIncreaseQueue"), exchange = @Exchange(value = "ShareholderStructureIncreaseQueueExchange", type = ExchangeTypes.DIRECT), // 这里指定交换机类型为
			// TOPIC
			key = "ShareholderStructureIncreaseQueue" // 这里指定 routing key
	), ackMode = "AUTO")
	public void shareholderStructureIncreaseListener(@Payload String jsonMessage,
			@Header(name = "UUID", required = false) String uuid,
			@Header(name = "sendQueueName", required = false) String sendQueueName,
			@Header(name = "sendExchangeName", required = false) String sendExchangeName)
			throws URISyntaxException, JsonMappingException, JsonProcessingException {
		try {
			ObjectMapper objectMapper = new ObjectMapper();
			objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd"));
			MessagePostProcessor messagePostProcessor = messageProperties -> {
				messageProperties.getMessageProperties().setHeader("UUID", uuid);
				return messageProperties;
			};
			 List<StructureContinueIncreaseVo> vos = bizService.getShareholderStructureContinueIncreaseLastTowWeeks();
			String voString = objectMapper.writeValueAsString(vos);
			rabbitTemplate.convertAndSend(sendExchangeName, uuid, voString, messagePostProcessor,
					new CorrelationData());
		} catch (Exception e) {
			//這裡應該也要給mq處理
			log.error(e.getMessage(),e);
			rabbitTemplate.convertAndSend("ShareholderStructureIncreaseExchangeError", "ShareholderStructureIncreaseExchangeError", jsonMessage);
		}
	}
	
	
	@RabbitListener(bindings = @QueueBinding(value = @Queue(value = "StockCodeFilterTypeQueue"), exchange = @Exchange(value = "StockCodeFilterTypeExchange", type = ExchangeTypes.DIRECT), // 这里指定交换机类型为
			// TOPIC
			key = "StockCodeFilterTypeQueue" // 这里指定 routing key
	), ackMode = "AUTO")
	public void stockExchangeDetailListener(@Payload String jsonMessage,
			@Header(name = "UUID", required = false) String uuid,
			@Header(name = "sendQueueName", required = false) String sendQueueName,
			@Header(name = "sendExchangeName", required = false) String sendExchangeName)
			throws URISyntaxException, JsonMappingException, JsonProcessingException {
		try {
			ObjectMapper objectMapper = new ObjectMapper();
			DynamicFilterStockCodeVo singleStockPriceBizVo = objectMapper.readValue(jsonMessage,
					DynamicFilterStockCodeVo.class);
			MessagePostProcessor messagePostProcessor = messageProperties -> {
				messageProperties.getMessageProperties().setHeader("UUID", uuid);
				return messageProperties;
			};
			List<StockInfoVo> vos = bizService.getMatchStockCodeByCondition(singleStockPriceBizVo);
			String voString = objectMapper.writeValueAsString(vos);
			rabbitTemplate.convertAndSend(sendExchangeName, uuid, voString, messagePostProcessor,
					new CorrelationData());
		} catch (Exception e) {
			//這裡應該也要給mq處理
			log.error(e.getMessage(),e);
			rabbitTemplate.convertAndSend("StockExchangeDetailExchangeError", "StockExchangeDetailExchangeError", jsonMessage);
		}
	}
}
