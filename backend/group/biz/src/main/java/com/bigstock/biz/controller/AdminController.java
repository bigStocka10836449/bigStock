//package com.bigstock.biz.controller;
//
//import java.util.concurrent.CompletableFuture;
//
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.PatchMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import com.bigstock.biz.dto.GrapAndInserDateRangeStockPriceRequest;
//import com.bigstock.biz.service.GraspHistoryStockPrice;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//
//@RestController
//@RequiredArgsConstructor
//@RequestMapping("schedule")
//@Slf4j
//public class AdminController {
//
//	private final GraspHistoryStockPrice graspHistoryStockPrice;
//
//
//	@PatchMapping("grapAndInserDateRangeStockPrice")
//	public ResponseEntity<String> getStockShareholderStructure(
//			@RequestBody GrapAndInserDateRangeStockPriceRequest request) {
//		CompletableFuture.runAsync(() -> {
//			graspHistoryStockPrice.manualGrapRangeHistoryStockPrice(request.getDateRangeDto().getStockType(), request.getDateRangeDto().isEnableflag());
//		});
//		return ResponseEntity.ok().build();
//	}
//
//}
