package com.bigstock.biz.infra;

//package 請保留你目前專案的實際路徑

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Date;

import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.bigstock.sharedComponent.entity.FcmRecord;
import com.bigstock.sharedComponent.service.FcmRecordService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

//請保留專案實際的 FcmRecord / FcmRecordService imports。

@Component
@RequiredArgsConstructor
public class BigStockJwtAuthFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(BigStockJwtAuthFilter.class);

	private static final ZoneId TAIPEI_ZONE = ZoneId.of("Asia/Taipei");

	private static final int MAX_STOCK_PRICE_BODY_BYTES = 64 * 1024;

	// ============================================================
	// Dependencies
	// ============================================================

	private final RedissonClient redissonClient;

	private final FcmRecordService fcmRecordService;

	private final ObjectMapper objectMapper;

	// ============================================================
	// JWT
	// ============================================================

	@Value("${server.oauth2.secret-key}")
	private String secretKey;

	// ============================================================
	// 全站 Token Bucket
	// ============================================================

	@Value("${bstock.rate-limit.global.rate:4}")
	private double globalRate;

	@Value("${bstock.rate-limit.global.burst:30}")
	private int globalBurst;

	// ============================================================
	// 三支股票 API 共用的 Token Bucket
	// ============================================================

	@Value("${bstock.rate-limit.history.enabled:true}")
	private boolean stockApiLimitEnabled;

	@Value("${bstock.rate-limit.history.rate:0.5}")
	private double stockApiRate;

	@Value("${bstock.rate-limit.history.burst:20}")
	private int stockApiBurst;

	// ============================================================
	// 每日不同股票配額
	// ============================================================

	@Value("${bstock.rate-limit.daily.enabled:true}")
	private boolean dailyQuotaEnabled;

	@Value("${bstock.rate-limit.daily.max-distinct-stocks:300}")
	private int dailyMaxDistinctStocks;

	// ============================================================
	// Lua
	// ============================================================

	private String tokenBucketScript;

	private String dailyQuotaScript;

	@PostConstruct
	public void init() {

		tokenBucketScript = loadScript("rate_limit_token_bucket.lua");

		dailyQuotaScript = loadScript("rate_limit_daily_distinct.lua");

		if (globalRate <= 0 || globalBurst < 1 || stockApiRate <= 0 || stockApiBurst < 1
				|| dailyMaxDistinctStocks < 1) {

			throw new IllegalStateException("Invalid rate limit configuration");
		}

		log.info("[RATE LIMIT] Initialized global={}/{}, stock={}/{}, daily={}", globalRate, globalBurst, stockApiRate,
				stockApiBurst, dailyMaxDistinctStocks);
	}

	private String loadScript(String filename) {

		ClassPathResource resource = new ClassPathResource(filename);

		try (InputStream input = resource.getInputStream()) {

			return new String(input.readAllBytes(), StandardCharsets.UTF_8);

		} catch (IOException e) {

			throw new IllegalStateException("Cannot load Lua script: " + filename, e);
		}
	}

	// ============================================================
	// Main Filter
	// ============================================================

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		String path = request.getRequestURI();

		// ========================================================
		// 1. 優先辨識需要保護的三支 API
		//
		// 即使 stockShareholderStructure 原本在白名單，
		// 這三支 API 仍要經過 JWT + FCM + Redis 限流。
		// ========================================================

		BigStockProtectedStockApi.Match protectedApi = BigStockProtectedStockApi.resolve(request.getMethod(), path);

		boolean isProtectedStockApi = protectedApi != null;

		// ========================================================
		// 2. 白名單
		//
		// 完全保留原本的判斷內容。
		//
		// 唯一差異：
		// 明確受保護的三支 API 不會跳過驗證。
		// ========================================================

		if (!isProtectedStockApi && (

		path.startsWith("/actuator/")

				|| path.startsWith("/auth/")

				|| path.startsWith("/api/guest-token")

				|| path.startsWith("/api/refresh-token")

				|| path.contains("/swagger")

				|| path.contains("/webjars") ||

				path.contains("/auth/") || path.contains("/stockShareholderStructure")
				|| path.contains("/financialCalendarImport") || path.contains("/device") || path.contains("/ws")

		)) {

			filterChain.doFilter(request, response);
			return;
		}

		// ========================================================
		// 3. Authorization
		// ========================================================

		String authHeader = request.getHeader("Authorization");

		if (authHeader == null || !authHeader.startsWith("Bearer ")) {

			unauthorized(response);
			return;
		}

		String token = authHeader.substring(7).trim();

		if (token.isBlank()) {

			unauthorized(response);
			return;
		}

		// ========================================================
		// 4. JWT
		// ========================================================

		Claims claims;

		try {

			claims = Jwts.parser().verifyWith(Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8))).build()
					.parseSignedClaims(token).getPayload();

		} catch (JwtException | IllegalArgumentException e) {

			unauthorized(response);
			return;
		}

		Date expiration = claims.getExpiration();

		if (expiration == null || !expiration.after(new Date())) {

			unauthorized(response);
			return;
		}

		// ========================================================
		// 5. FCM Record
		// ========================================================

		FcmRecord fcmRecord;

		try {

			fcmRecord = fcmRecordService.getFcmRecord(token);

		} catch (RuntimeException e) {

			log.error("[FCM] Verification lookup failed", e);

			serviceUnavailable(response);
			return;
		}

		if (fcmRecord == null || fcmRecord.getFcmToken() == null || fcmRecord.getFcmToken().isBlank()) {

			unauthorized(response);
			return;
		}

		// getFcmRecord(token) 必須只接受已完成
		// FCM Challenge 驗證且尚未失效的紀錄。

		// ========================================================
		// 6. Device Hash
		// ========================================================

		String deviceHash = DigestUtils.md5DigestAsHex(fcmRecord.getFcmToken().getBytes(StandardCharsets.UTF_8));

		// ========================================================
		// 7. POST Body Wrapper
		//
		// 只有 SingleStockPrice 需要。
		// ========================================================

		HttpServletRequest requestForController = request;

		String stockCode = protectedApi == null ? null : protectedApi.stockCode();

		if (protectedApi != null && protectedApi.type() == BigStockProtectedStockApi.ApiType.STOCK_PRICE) {

			if (request.getContentLengthLong() > MAX_STOCK_PRICE_BODY_BYTES) {

				payloadTooLarge(response);
				return;
			}

			try {

				CachedBodyHttpServletRequest wrapped = new CachedBodyHttpServletRequest(request,
						MAX_STOCK_PRICE_BODY_BYTES);

				JsonNode body = objectMapper.readTree(wrapped.getCachedBody());

				JsonNode stockNode = body == null ? null : body.get("stockCode");

				if (stockNode == null || !stockNode.isTextual()) {

					badRequest(response, "STOCK_CODE_REQUIRED");

					return;
				}

				stockCode = stockNode.asText().trim();

				requestForController = wrapped;

			} catch (CachedBodyHttpServletRequest.RequestBodyTooLargeException e) {

				payloadTooLarge(response);
				return;

			} catch (IOException e) {

				badRequest(response, "INVALID_REQUEST_BODY");

				return;
			}
		}

		if (isProtectedStockApi
				&& (stockCode == null || stockCode.isBlank() || !stockCode.matches("[A-Za-z0-9]{1,12}"))) {

			badRequest(response, "INVALID_STOCK_CODE");

			return;
		}

		// ========================================================
		// 8. Redis Rate Limit
		// ========================================================

		try {

			// ----------------------------------------------------
			// A. 全站 Token Bucket
			// ----------------------------------------------------

			String globalKey = "rl:guest-token:tb:" + deviceHash;

			boolean globalAllowed = acquireToken(globalKey, globalRate, globalBurst);

			if (!globalAllowed) {

				rateLimitExceeded(response, "GLOBAL_RATE_LIMIT_EXCEEDED", 1);

				return;
			}

			// ----------------------------------------------------
			// B. 三支 API 共用專屬 Token Bucket
			// ----------------------------------------------------

			if (isProtectedStockApi) {

				if (stockApiLimitEnabled) {

					String stockApiKey = "rl:stock-api:tb:v1:" + deviceHash;

					boolean stockAllowed = acquireToken(stockApiKey, stockApiRate, stockApiBurst);

					if (!stockAllowed) {

						rateLimitExceeded(response, "STOCK_API_RATE_LIMIT_EXCEEDED", 2);

						return;
					}
				}

				// ------------------------------------------------
				// C. 每日不同股票配額
				// ------------------------------------------------

				if (dailyQuotaEnabled) {

					boolean dailyAllowed = acquireDailyQuota(deviceHash, stockCode);

					if (!dailyAllowed) {

						rateLimitExceeded(response, "DAILY_STOCK_QUOTA_EXCEEDED", secondsUntilNextTaipeiDay());

						return;
					}
				}
			}

		} catch (RuntimeException e) {

			log.error("[RATE LIMIT] Redis failure, path={}", path, e);

			serviceUnavailable(response);
			return;
		}

		// ========================================================
		// 9. 通過驗證
		//
		// POST 使用 Wrapper，確保 Controller 仍可讀取 Body。
		// ========================================================

		filterChain.doFilter(requestForController, response);
	}

	// ============================================================
	// Redis Token Bucket
	// ============================================================

	private boolean acquireToken(String key, double rate, int burst) {

		Long allowed = redissonClient.getScript(StringCodec.INSTANCE).eval(RScript.Mode.READ_WRITE, tokenBucketScript,
				RScript.ReturnType.INTEGER, Collections.singletonList(key), String.valueOf(rate),
				String.valueOf(burst));

		return allowed != null && allowed == 1L;
	}

	// ============================================================
	// Daily Distinct Stock Quota
	// ============================================================

	private boolean acquireDailyQuota(String deviceHash, String stockCode) {

		LocalDate today = LocalDate.now(TAIPEI_ZONE);

		String quotaKey = "rl:stock-api:daily:" + today + ":" + deviceHash;

		long ttl = secondsUntilNextTaipeiDay() + 3600;

		Long allowed = redissonClient.getScript(StringCodec.INSTANCE).eval(RScript.Mode.READ_WRITE, dailyQuotaScript,
				RScript.ReturnType.INTEGER, Collections.singletonList(quotaKey), stockCode,
				String.valueOf(dailyMaxDistinctStocks), String.valueOf(ttl));

		return allowed != null && allowed == 1L;
	}

	private long secondsUntilNextTaipeiDay() {

		ZonedDateTime now = ZonedDateTime.now(TAIPEI_ZONE);

		ZonedDateTime tomorrow = now.toLocalDate().plusDays(1).atStartOfDay(TAIPEI_ZONE);

		return Math.max(1, tomorrow.toEpochSecond() - now.toEpochSecond());
	}

	// ============================================================
	// HTTP Responses
	// ============================================================

	private void unauthorized(HttpServletResponse response) throws IOException {

		writeError(response, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
	}

	private void badRequest(HttpServletResponse response, String error) throws IOException {

		writeError(response, HttpStatus.BAD_REQUEST, error);
	}

	private void payloadTooLarge(HttpServletResponse response) throws IOException {

		writeError(response, HttpStatus.PAYLOAD_TOO_LARGE, "REQUEST_BODY_TOO_LARGE");
	}

	private void rateLimitExceeded(HttpServletResponse response, String error, long retryAfter) throws IOException {

		response.setHeader("Retry-After", String.valueOf(Math.max(1, retryAfter)));

		writeError(response, HttpStatus.TOO_MANY_REQUESTS, error);
	}

	private void serviceUnavailable(HttpServletResponse response) throws IOException {

		response.setHeader("Retry-After", "5");

		writeError(response, HttpStatus.SERVICE_UNAVAILABLE, "RATE_LIMIT_SERVICE_UNAVAILABLE");
	}

	private void writeError(HttpServletResponse response, HttpStatus status, String error) throws IOException {

		response.setStatus(status.value());

		response.setCharacterEncoding("UTF-8");

		response.setContentType("application/json;charset=UTF-8");

		response.setHeader("Cache-Control", "no-store");

		response.getWriter().write("{\"error\":\"" + error + "\"}");
	}
}