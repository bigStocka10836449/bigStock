package com.bigstock.biz.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bigstock.biz.service.BizService;
import com.bigstock.sharedComponent.entity.ShareholderStructure;

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
	
//	@NewSpan("stockShareholderStructure")
	@Operation(summary = "個別股票持股分布", description = "")
	@GetMapping("stockShareholderStructure/{stockCode}")
	public ResponseEntity<List<ShareholderStructure>> getStockShareholderStructure(
			@PathVariable("stockCode") String stockCode) {
		return ResponseEntity.ok(bizService.getStockShareholderStructure(stockCode, 52));
	}

}
