package com.bigstock.schedule.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bigstock.schedule.dto.GrapAndInserDateRangeStockPrice.GrapAndInserDateRangeStockPriceRequest;
import com.bigstock.schedule.dto.GrapAndInserDateRangeStockPrice.GrapAndInserDateRangeStockPriceResponse;
import com.bigstock.schedule.service.GraspHistoryStockPrice;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("schedule")
public class AdminController {
	
	private final GraspHistoryStockPrice graspHistoryStockPrice;
	@PatchMapping("grapAndInserDateRangeStockPrice")
	public ResponseEntity<GrapAndInserDateRangeStockPriceResponse> getStockShareholderStructure(@RequestBody 
			GrapAndInserDateRangeStockPriceRequest request) {
		graspHistoryStockPrice.manualGrapRangeHistoryStockPrice(request.getDateRangeDto().getStartDate(),
				request.getDateRangeDto().getEndDate());
		return ResponseEntity.ok(new GrapAndInserDateRangeStockPriceResponse());
	}
}
