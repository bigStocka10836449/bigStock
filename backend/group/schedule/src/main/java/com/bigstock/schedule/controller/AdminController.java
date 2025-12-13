package com.bigstock.schedule.controller;

import java.util.concurrent.CompletableFuture;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bigstock.schedule.dto.GrapAndInserDateRangeStockPrice.GrapAndInserDateRangeStockPriceRequest;
import com.bigstock.schedule.service.GraspHistoryStockPrice;
import com.bigstock.schedule.service.GraspStockPrice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequiredArgsConstructor
@RequestMapping("schedule")
@Slf4j
public class AdminController {

	private final GraspHistoryStockPrice graspHistoryStockPrice;

	private final GraspStockPrice graspStockPrice;

	@PatchMapping("grapAndInserDateRangeStockPrice")
	public ResponseEntity<String> getStockShareholderStructure(
			@RequestBody GrapAndInserDateRangeStockPriceRequest request) {
		CompletableFuture.runAsync(() -> {
			graspHistoryStockPrice.manualGrapRangeHistoryStockPrice(request.getDateRangeDto().getStockType(), request.getDateRangeDto().isEnableflag());
		});
		return ResponseEntity.ok().build();
	}

//	@PatchMapping("doGrepSecuritiesFirmsDayOperate")
//	public ResponseEntity<String> doGrepSecuritiesFirmsDayOperate() {
//		try {
//			graspStockPrice.tryRedoGrepSecuritiesFirmsDayOperate();
//		} catch (Exception e) {
//			log.error(e.getMessage(), e);
//			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
//		}
//		return ResponseEntity.ok().build();
//	}

}
