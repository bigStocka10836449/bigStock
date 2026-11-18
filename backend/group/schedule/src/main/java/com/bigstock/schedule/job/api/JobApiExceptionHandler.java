package com.bigstock.schedule.job.api;

import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice(assignableTypes = JobManagementController.class)
public class JobApiExceptionHandler {
	@ExceptionHandler(NoSuchElementException.class)
	ResponseEntity<Map<String, String>> notFound(Exception e) {
		return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
	}

	@ExceptionHandler(IllegalStateException.class)
	ResponseEntity<Map<String, String>> conflict(Exception e) {
		return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	ResponseEntity<Map<String, String>> badRequest(Exception e) {
		return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
	}
}
