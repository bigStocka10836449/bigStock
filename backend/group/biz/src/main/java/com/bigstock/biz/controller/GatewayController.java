package com.bigstock.biz.controller;

import java.io.IOException;
import java.text.ParseException;
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

import com.bigstock.biz.service.BizService;
import com.bigstock.sharedComponent.dto.DynamicFilterStockCodeVo;
import com.bigstock.sharedComponent.dto.SingleStockPriceBizVo;
import com.bigstock.sharedComponent.dto.SingleStockPriceVo;
import com.bigstock.sharedComponent.dto.StockInfoVo;
import com.bigstock.sharedComponent.dto.StructureContinueIncreaseVo;
import com.bigstock.sharedComponent.rabbitmq.RabbitMqService;
import com.bigstock.sharedComponent.service.SocketPushService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

//import io.micrometer.tracing.annotation.NewSpan;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;

@RestController()
@RequestMapping(value = "gateway")
@Slf4j
public class GatewayController {

	
	@Autowired
	SocketPushService socketPushService;
	@Autowired
	BizService bizService;

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
			ObjectMapper objectMapper = new ObjectMapper();
			objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd"));
			SingleStockPriceVo vo = bizService.getSingleStockPrices(singleStockPriceBizVo.getStockCode(),
					singleStockPriceBizVo.getSearchStartDate(), singleStockPriceBizVo.getSearchEndDate());
			String voString = objectMapper.writeValueAsString(vo);
			return ResponseEntity.ok(voString);
		} catch (ParseException| IOException e) {
			log.error(e.getMessage(), e);
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Operation(summary = "依照查詢條件篩選符合的股票代碼", description = "")
	@PostMapping("StockCodeByFilter")
	public ResponseEntity<String> getStockCodeByFilter(@RequestBody List<DynamicFilterStockCodeVo> dynamicFilterStockCodeVos)  {
		try {
			if(ObjectUtils.isEmpty(dynamicFilterStockCodeVos) || dynamicFilterStockCodeVos.get(0).getConditions().isEmpty()) {
				return ResponseEntity.ok().build();
			}
			ObjectMapper objectMapper = new ObjectMapper();
			objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd"));
			List<StockInfoVo> vos = bizService.getMatchStockCodeByCondition(dynamicFilterStockCodeVos);
			String voString = objectMapper.writeValueAsString(vos);

			return ResponseEntity.ok(voString);
		} catch ( JsonProcessingException e) {
			log.error(e.getMessage(), e);
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	@Operation(summary = "持股大戶連續2個星期增加的股票查詢", description = "")
	@GetMapping("shareholderStructureContinueIncreaseLastTowWeeks")
	public ResponseEntity<String> getShareholderStructureContinueIncreaseLastTowWeeks() {
		try {
			CountDownLatch latch = new CountDownLatch(1);
			ObjectMapper objectMapper = new ObjectMapper();
			objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd"));
			List<StructureContinueIncreaseVo> vos = bizService.getShareholderStructureContinueIncreaseLastTowWeeks();
			String voString = objectMapper.writeValueAsString(vos);
			return ResponseEntity.ok(voString);
		} catch (IOException e) {
			log.error(e.getMessage(), e);
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	@Operation(summary = "個別股票價格，版本2作法，直接打給MQ,然後前端再透過查詢進度的方式抓到當前進度", description = "")
	@PostMapping("sse/singleStockPrice")
	public ResponseEntity<String> SingleStockPriceSSE(@RequestBody SingleStockPriceBizVo singleStockPriceBizVo) {
		try {
			ObjectMapper objectMapper = new ObjectMapper();
			objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd"));
			SingleStockPriceVo vo = bizService.getSingleStockPrices(singleStockPriceBizVo.getStockCode(),
					singleStockPriceBizVo.getSearchStartDate(), singleStockPriceBizVo.getSearchEndDate());

			String voString = objectMapper.writeValueAsString(vo);
			return ResponseEntity.ok(voString);
		} catch (ParseException | IOException  e) {
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
