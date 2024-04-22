package com.bigstock.biz.ctroller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bigstock.biz.service.BizService;
import com.bigstock.sharedComponent.entity.ShareholderStructure;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("biz")
public class BizController {

	private final BizService bizService;

	@GetMapping("stockShareholderStructure/{stockCode}")
	public ResponseEntity<List<ShareholderStructure>> getStockShareholderStructure(
			@PathVariable("stockCode") String stockCode) {
		return ResponseEntity.ok(bizService.getStockShareholderStructure(stockCode,52));
	}
	
}
