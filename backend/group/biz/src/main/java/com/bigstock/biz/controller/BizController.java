package com.bigstock.biz.controller;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.json.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;

import com.bigstock.biz.dto.MarginTradingAndShortSellingInfoVO;
import com.bigstock.biz.dto.RankingResponse;
import com.bigstock.biz.schedule.GraspStockPrice;
import com.bigstock.biz.service.BizService;
import com.bigstock.biz.service.RankStockChangeService;
import com.bigstock.sharedComponent.entity.ShareholderStructure;
import com.bigstock.sharedComponent.service.SecuritiesFirmsDayOperateService;
import com.fasterxml.jackson.databind.ObjectMapper;

//import io.micrometer.tracing.annotation.NewSpan;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("biz")
@Tag(name = "BIZ Controller", description = "Auth for loggin and api")
public class BizController {

	private final BizService bizService;
	
	private final SecuritiesFirmsDayOperateService securitiesFirmsDayOperateService;
	
	private final GraspStockPrice graspStockPrice;
	
	private final RankStockChangeService  rankStockChangeService;
	
	
//	@NewSpan("stockShareholderStructure")
	@Operation(summary = "個別股票持股分布", description = "")
	@GetMapping("stockShareholderStructure/{stockCode}")
	public ResponseEntity<List<ShareholderStructure>> getStockShareholderStructure(
			@PathVariable("stockCode") String stockCode) {
		return ResponseEntity.ok(bizService.getStockShareholderStructure(stockCode, 52));
	}
	
	@Operation(summary = "個股漲跌幅排行", description = "market 分為 TEPX 與 TWSE")
	@GetMapping("ranking/raiseChange")
	public List<RankingResponse> getRanking(@RequestParam String market, @RequestParam(defaultValue = "10") int limit,
			@RequestParam(defaultValue = "desc") String order) {

		return rankStockChangeService.getRanking(market, limit, order);
	}
	
	
	@Operation(summary = "個別股票資券資訊(依照最後交易日抓取52個交易天的內容", description = "")
	@GetMapping("stockCodeMarginShortInfo/{stockCode}")
	public ResponseEntity<List<MarginTradingAndShortSellingInfoVO>> getStockMarginTradingAndShortSelling(
			@PathVariable("stockCode") String stockCode) {
		List<MarginTradingAndShortSellingInfoVO> MarginTradingAndShortSellingInfoVOs = bizService.getStockMarginTradingAndShortSelling(stockCode).stream()
		            .map(MarginTradingAndShortSellingInfoVO::fromEntity) // 使用 VO 的轉換方法
		            .collect(Collectors.toList());
		return ResponseEntity.ok(MarginTradingAndShortSellingInfoVOs);
	}

//	@Operation(summary = "個股買賣日報表", description = "")
//	@GetMapping("stockShareholderStructure/{stockCode}/{tradingDate}")
//	public ResponseEntity<List<SecuritiesFirmsDayOperate>> getSecuritiesFirmsDayOperate(
//			@PathVariable("stockCode") String stockCode, @PathVariable("tradingDate") Date tradingDate) {
//		return ResponseEntity.ok(securitiesFirmsDayOperateService.getByStockCodeAndTradingDate(stockCode, tradingDate));
//	}

	@Operation(summary = "個股買賣日報表資料匯入", description = "")
	@PostMapping("stockShareholderStructure")
	public void getSecuritiesFirmsDayOperate(@RequestBody Map<String, Object> request) throws RestClientException, InterruptedException, URISyntaxException {
		ObjectMapper mapper = new ObjectMapper();
		try {
	        String jsonString = mapper.writeValueAsString(request);
	        graspStockPrice.grepSecuritiesFirmsDayOperate(new JSONObject(jsonString));
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}
}
