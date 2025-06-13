package com.bigstock.gateway.web;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bigstock.sharedComponent.dto.DynamicFilterStockCodeVo;
import com.bigstock.sharedComponent.dto.SingleStockPriceBizVo;
import com.bigstock.sharedComponent.rabbitmq.RabbitMqService;
import com.bigstock.sharedComponent.service.SocketPushService;
import com.fasterxml.jackson.databind.ObjectMapper;

//import io.micrometer.tracing.annotation.NewSpan;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;

@RestController()
@RequestMapping(value = "gateway")
@Slf4j
public class GatewayController {

	@Autowired
	RabbitMqService rabbitMqService;
	
	@Autowired
	SocketPushService socketPushService;


	@PostMapping("forTest")
	@PreAuthorize("hasRole('Admin7')")
	public String forTest() {
		return "Sueescc";
	}

	//小小紀錄一下，如果要增加單個API Header作法，特別注意Authorization是保留字，加上去沒用 @Parameter(name = "Authorization1114", description = "jwt , start wih 'Bearer ....'", required = true, in = ParameterIn.HEADER)
	@Operation(summary = "個別股票價格", description = "")
	@PostMapping("SingleStockPrice")
	public ResponseEntity<String> SingleStockPrice(@RequestBody SingleStockPriceBizVo singleStockPriceBizVo) {
		try {
			CountDownLatch latch = new CountDownLatch(1);
			String uuid = rabbitMqService.createConsumer(latch, "gatewayQueue", "gatewayExchange");
			ObjectMapper objectMapper = new ObjectMapper();
			objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd"));
			String jsonMessage = objectMapper.writeValueAsString(singleStockPriceBizVo);
			rabbitMqService.sendMessage(jsonMessage, "SingleStockPriceExchange", "SingleStockPriceQueue", uuid,
					"gatewayExchange", "gatewayQueue");
			latch.await(120, TimeUnit.SECONDS);
			Optional<Object> mqResultOp = Optional.ofNullable(rabbitMqService.getValueFromTmpStoredReceivedData(uuid));
			if (mqResultOp.isEmpty()) {
				return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
			}
			return ResponseEntity.ok(mqResultOp.get().toString());
		} catch (InterruptedException | IOException | TimeoutException e) {
			log.error(e.getMessage(), e);
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Operation(summary = "依照查詢條件篩選符合的股票代碼", description = "")
	@PostMapping("StockCodeByFilter")
	public ResponseEntity<String> getStockCodeByFilter(@RequestBody List<DynamicFilterStockCodeVo> dynamicFilterStockCodeVos) {
		try {
			if(ObjectUtils.isEmpty(dynamicFilterStockCodeVos) || dynamicFilterStockCodeVos.get(0).getConditions().isEmpty()) {
				return ResponseEntity.ok().build();
			}
			CountDownLatch latch = new CountDownLatch(1);
			String uuid = rabbitMqService.createConsumer(latch, "gatewayQueue", "gatewayExchange");
			ObjectMapper objectMapper = new ObjectMapper();
			objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd"));
			String jsonMessage = objectMapper.writeValueAsString(dynamicFilterStockCodeVos);
			rabbitMqService.sendMessage(jsonMessage, "StockCodeFilterTypeExchange", "StockCodeFilterTypeQueue", uuid,
					"gatewayExchange", "gatewayQueue");
			latch.await(120, TimeUnit.SECONDS);
			Optional<Object> mqResultOp = Optional.ofNullable(rabbitMqService.getValueFromTmpStoredReceivedData(uuid));
			if (mqResultOp.isEmpty()) {
				return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
			}
			return ResponseEntity.ok(mqResultOp.get().toString());
		} catch (InterruptedException | IOException | TimeoutException e) {
			log.error(e.getMessage(), e);
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	@Operation(summary = "持股大戶連續2個星期增加的股票查詢", description = "")
	@GetMapping("shareholderStructureContinueIncreaseLastTowWeeks")
	public ResponseEntity<String> getShareholderStructureContinueIncreaseLastTowWeeks() {
		try {
			CountDownLatch latch = new CountDownLatch(1);
			String uuid = rabbitMqService.createConsumer(latch, "gatewayQueue", "gatewayExchange");
			rabbitMqService.sendMessage(uuid, "ShareholderStructureIncreaseExchange",
					"ShareholderStructureIncreaseQueue", uuid, "gatewayExchange", "gatewayQueue");
			latch.await(120, TimeUnit.SECONDS);
			Optional<Object> mqResultOp = Optional.ofNullable(rabbitMqService.getValueFromTmpStoredReceivedData(uuid));
			if (mqResultOp.isEmpty()) {
				return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
			}
			return ResponseEntity.ok(mqResultOp.get().toString());
		} catch (InterruptedException | IOException | TimeoutException e) {
			log.error(e.getMessage(), e);
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	@Operation(summary = "個別股票價格，版本2作法，直接打給MQ,然後前端再透過查詢進度的方式抓到當前進度", description = "")
	@PostMapping("sse/singleStockPrice")
	public ResponseEntity<String> SingleStockPriceSSE(@RequestBody SingleStockPriceBizVo singleStockPriceBizVo) {
		try {
			CountDownLatch latch = new CountDownLatch(1);
			String uuid = rabbitMqService.createConsumer(latch, "gatewayQueue", "gatewayExchange");
			ObjectMapper objectMapper = new ObjectMapper();
			objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd"));
			String jsonMessage = objectMapper.writeValueAsString(singleStockPriceBizVo);
			rabbitMqService.sendMessage(jsonMessage, "SingleStockPriceExchange", "SingleStockPriceQueue", uuid,
					"gatewayExchange", "gatewayQueue");
			latch.await(120, TimeUnit.SECONDS);
			Optional<Object> mqResultOp = Optional.ofNullable(rabbitMqService.getValueFromTmpStoredReceivedData(uuid));
			if (mqResultOp.isEmpty()) {
				return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
			}
			return ResponseEntity.ok(mqResultOp.get().toString());
		} catch (InterruptedException | IOException | TimeoutException e) {
			log.error(e.getMessage(), e);
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Operation(summary = "依照查詢條件篩選符合的股票代碼，版本2作法，直接打給MQ,然後前端再透過查詢進度的方式抓到當前進度", description = "")
	@PostMapping("sse/stockCodeByFilter")
	public ResponseEntity<String> getStockCodeByFilterSSE(@RequestBody List<DynamicFilterStockCodeVo> dynamicFilterStockCodeVos) {
		try {
			if(ObjectUtils.isEmpty(dynamicFilterStockCodeVos) || dynamicFilterStockCodeVos.get(0).getConditions().isEmpty()) {
				return ResponseEntity.ok().build();
			}
			ObjectMapper objectMapper = new ObjectMapper();
			objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd"));
			String jsonMessage = objectMapper.writeValueAsString(dynamicFilterStockCodeVos);
			rabbitMqService.sendMessage(jsonMessage, "SSEStockCodeFilterTypeExchange", "stock.filter.*", dynamicFilterStockCodeVos.stream().findFirst().get().getSessionId());
			return new ResponseEntity<>(HttpStatus.OK);
		} catch (IOException e) {
			log.error(e.getMessage(), e);
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	@Operation(summary = "持股大戶連續2個星期增加的股票查詢，版本2作法，直接打給MQ,然後前端再透過查詢進度的方式抓到當前進度", description = "")
	@GetMapping("sse/shareholderStructureContinueIncreaseLastTowWeeks")
	public ResponseEntity<String> getShareholderStructureContinueIncreaseLastTowWeeksSSE() {
		try {
			CountDownLatch latch = new CountDownLatch(1);
			String uuid = rabbitMqService.createConsumer(latch, "gatewayQueue", "gatewayExchange");
			rabbitMqService.sendMessage(uuid, "ShareholderStructureIncreaseExchange",
					"ShareholderStructureIncreaseQueue", uuid, "gatewayExchange", "gatewayQueue");
			latch.await(120, TimeUnit.SECONDS);
			Optional<Object> mqResultOp = Optional.ofNullable(rabbitMqService.getValueFromTmpStoredReceivedData(uuid));
			if (mqResultOp.isEmpty()) {
				return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
			}
			return ResponseEntity.ok(mqResultOp.get().toString());
		} catch (InterruptedException | IOException | TimeoutException e) {
			log.error(e.getMessage(), e);
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping("sse/session")
	public ResponseEntity<Map<String,String>> createSSESession(Authentication authentication){
	    String userId = authentication.getName();
	    String sessionId = userId + "-" + System.currentTimeMillis(); 
	    socketPushService.storeSessionIfAbsent(sessionId, userId, Duration.ofSeconds(3600));
	    return ResponseEntity.ok(Map.of("sessionId", sessionId));
	}
}
