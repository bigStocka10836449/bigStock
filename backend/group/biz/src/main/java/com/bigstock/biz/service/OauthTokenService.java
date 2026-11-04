package com.bigstock.biz.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpException;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import com.bigstock.biz.dto.WebTokenRequest;
import com.bigstock.biz.infra.TurnstileVerificationService;
import com.bigstock.biz.vo.UserInloginInfo;
import com.bigstock.sharedComponent.dto.TempTokenVo;
import com.bigstock.sharedComponent.entity.FcmRecord;
import com.bigstock.sharedComponent.entity.RoleInfo;
import com.bigstock.sharedComponent.entity.UserAccount;
import com.bigstock.sharedComponent.service.FcmRecordService;
import com.bigstock.sharedComponent.service.RoleInfoService;
import com.bigstock.sharedComponent.service.UserAccountService;
import com.google.common.collect.Lists;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OauthTokenService {

	@Value("${server.oauth2.secret-key}")

	private String secretKey;

	@Value("${server.oauth2.key}")

	private String keyPath;

	private final RoleInfoService roleInfoService;

	private final UserAccountService userAccountService;

	private final RedissonClient redissonClient;

	private final FcmRecordService fcmRecordService;

	private final TurnstileVerificationService turnstileVerificationService;

	public ResponseEntity<?> userLoginHandle(UserInloginInfo userInloginInfo) {

		BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

		String username = userInloginInfo.getUserName();

		String password = userInloginInfo.getPassword();

		Optional<UserAccount> userAccountOp = Optional.empty();

		userAccountOp = userAccountService.findUserByEmail(username);

		UserAccount userAccount = userAccountOp.orElseThrow(() -> new JwtException("user can not found"));

		// 验证密码

		if (!passwordEncoder.matches(password, userAccount.getUserPassword())) {

			throw new JwtException("invalid password");

		}

		Date expiration = new Date(System.currentTimeMillis() + Duration.ofHours(1).toMillis());

		String accessToken = generateAccessToken(username, userAccount, expiration);

		String refreshToken = generateRefreshToken(username);

		// 將新的 refresh token 存入資料庫

		RBucket<Object> refreshTokenRb = redissonClient.getBucket("refresh_token:" + username);

		refreshTokenRb.set(refreshToken);

		refreshTokenRb.expire(Duration.ofHours(4));

		// 將新的access token倒回去Redis

		RBucket<Object> accessTokenRb = redissonClient.getBucket("access_token:" + username);

		accessTokenRb.set(accessToken);

		accessTokenRb.expire(Duration.ofHours(1));

		return ResponseEntity.ok().header("X-Refreshed-Token", accessToken)

				.body(Map.of("accessToken", accessToken, "exp", expiration.getTime()));

	}

	/** Web: Turnstile verified once per new session; no FCM or database writes. */
	public ResponseEntity<?> getTmpTokenForWeb(WebTokenRequest request) {
		if (request == null || request.turnstileToken() == null) {
			throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Turnstile token required");
		}
		turnstileVerificationService.verify(request.turnstileToken());

		// New random visitor identity. Existing guest_id cookie is not trusted.
		String guestId = UUID.randomUUID().toString();
		Instant now = Instant.now();
		Instant expiresAt = now.plus(Duration.ofHours(1));
		String jwt = Jwts.builder().subject(guestId).claim("roles", List.of("4")).claim("token_type", "web_guest")
				.issuedAt(Date.from(now)).expiration(Date.from(expiresAt))
				.signWith(Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256).compact();

		Duration ttl = Duration.between(Instant.now(), expiresAt);
		if (ttl.isZero() || ttl.isNegative())
			throw new IllegalStateException("JWT expiration invalid");
		redissonClient.<String>getBucket("web:jwt:" + guestId).set(jwt, ttl);

		ResponseCookie cookie = ResponseCookie.from("bstock_guest_token", jwt).httpOnly(true).secure(true)
				.sameSite("Strict").path("/").maxAge(ttl).build();

		return ResponseEntity.ok().cacheControl(CacheControl.noStore())
				.header(HttpHeaders.SET_COOKIE, cookie.toString())
				.body(Map.of("authenticated", true, "exp", expiresAt.getEpochSecond()));
	}

	public ResponseEntity<?> getTmpToken(HttpServletRequest request, String guestId, TempTokenVo tempTokenVo)
			throws IOException, HttpException {
		// Mobile contract remains unchanged: Bearer JWT in body and header.
		String fcmToken = requireFcmToken(tempTokenVo);
		FcmRecord fcmRecord = fcmRecordService.validateVerifiedDevice(fcmToken);
		guestId = guestIdFor(fcmToken);

		String token = getOrCreateGuestToken(guestId, fcmRecord);
		Claims claims = parseJwtToken(token);

		ResponseCookie cookie = ResponseCookie.from("guest_id", guestId).httpOnly(true).secure(true).sameSite("Strict")
				.path("/").maxAge(Duration.ofHours(1)).build();

		return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString())
				.header("X-Refreshed-Token", "Bearer " + token)
				.body(Map.of("token", token, "exp", claims.getExpiration()));
	}

	public ResponseEntity<?> refreshToken(String refreshToken) {

		Claims claims = parseJwtToken(refreshToken);

		String subject = claims.getSubject();

		String role = claims.get("roles", List.class).get(0).toString();

		if ("4".equals(role)) {

			// Guest 不應進入 refresh 流程，直接拋錯或略過

			throw new JwtException("Guest token cannot be refreshed");

		}

		RBucket<String> refreshTokenBucket = redissonClient.getBucket("refresh_token:" + subject);

		if (!refreshTokenBucket.isExists()) {

			throw new JwtException("Refresh token expired");

		}

		// 若為 USER 則重新產生 accessToken 並延長 refreshToken

		Date expiration = new Date(System.currentTimeMillis() + Duration.ofHours(1).toMillis());

		String newAccessToken = generateAccessToken(subject, expiration);

		RBucket<Object> accessTokenRb = redissonClient.getBucket("access_token:" + subject);

		accessTokenRb.set(newAccessToken, Duration.ofHours(1));

		refreshTokenBucket.expire(Duration.ofHours(4)); // 延長 refreshToken 有效期

		return ResponseEntity.ok()

				.header("X-Refreshed-Token", newAccessToken)

				.body(Map.of("accessToken", newAccessToken, "exp", expiration.getTime()));

	}

	public Claims parseJwtToken(String token) throws JwtException {

		Jws<Claims> jws = Jwts.parser().verifyWith(Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8)))
				.build()

				.parseSignedClaims(token);

		return jws.getPayload();

	}

	public String generateAccessToken(String subject, UserAccount userAccount, Date expiration) {

		// 使用 Jwts.builder() 建立 JWT

		JwtBuilder builder = Jwts.builder();

		String roleId = userAccount.getRoleId();

		RoleInfo roleInfo = roleInfoService.getAll().stream().filter(data -> data.getRoleId().equals(roleId)).findAny()

				.orElseThrow(() -> new JwtException("role can not found"));

		// 設定 JWT 主體

		builder.subject(subject);

		builder.claim("roles", Lists.newArrayList(roleInfo.getRoleId()));

		// 設定 JWT 發行時間

		builder.issuedAt(new Date());

		// 設定 JWT 有效期

		builder.expiration(expiration);

		// 添加 header

		builder.header().add("typ", "JWT").and();

		builder.header().add("alg", "HS256").and();

		// 設定 JWT 簽名

		builder.signWith(Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256);

		// 建立並返回 JWT

		String token = builder.compact();

		// 返回 Bearer 令牌

		return token;

	}

	public String generateAccessToken(String subject, Date expiration) {

		// 使用 Jwts.builder() 建立 JWT

		JwtBuilder builder = Jwts.builder();

		Optional<UserAccount> userAccountsOp = Optional.empty();

		userAccountsOp = userAccountService.findUserByEmail(subject);

		if (userAccountsOp.isEmpty()) {

			throw new JwtException("invalid token");

		}

		String roleId = userAccountsOp.get().getRoleId();

		RoleInfo roleInfo = roleInfoService.getAll().stream().filter(data -> data.getRoleId().equals(roleId)).findAny()

				.orElseThrow(() -> new JwtException("role can not found"));

		// 設定 JWT 主體

		builder.subject(subject);

		builder.claim("roles", Lists.newArrayList(roleInfo.getRoleId()));

		// 設定 JWT 發行時間

		builder.issuedAt(new Date());

		// 設定 JWT 有效期

		builder.expiration(expiration);

		// 添加 header

		builder.header().add("typ", "JWT").and();

		builder.header().add("alg", "HS256").and();

		// 設定 JWT 簽名

		builder.signWith(Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256);

		// 建立並返回 JWT

		return builder.compact();

	}

	private String generateRefreshToken(String subject) {

		// 使用 Jwts.builder() 建立 JWT

		JwtBuilder builder = Jwts.builder();

		// 設定 JWT 主體

		builder.subject(subject);

		// 設定 JWT 發行時間

		builder.issuedAt(new Date());

		// 設定 JWT 有效期

		builder.expiration(new Date(System.currentTimeMillis() + Duration.ofHours(4).toMillis()));

		// 添加 header

		builder.header().add("typ", "JWT").and();

		builder.header().add("alg", "HS256").and();

		// 設定 JWT 簽名

		builder.signWith(Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256);

		// 建立並返回 JWT

		return builder.compact();

	}

	public String generateRegistryToken(String subject) {

		JwtBuilder builder = Jwts.builder();

		builder.subject(subject);

		// 設定 JWT 發行時間

		builder.issuedAt(new Date());

		// 設定 JWT 有效期

		builder.expiration(new Date(System.currentTimeMillis() + Duration.ofHours(1).toMillis()));

		// 添加 header

		builder.header().add("typ", "JWT").and();

		builder.header().add("alg", "HS256").and();

		// 設定 JWT 簽名

		builder.signWith(Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256);

		// 設定 JWT 簽名

		// 建立並返回 JWT

		return builder.compact();

	}

	public String createTempAccessToken(String subject, String role) {

		JwtBuilder builder = Jwts.builder();

		// 設定 JWT 主體

		builder.subject(subject);

		builder.claim("roles", Lists.newArrayList("4"));

		// 設定 JWT 發行時間

		builder.issuedAt(new Date());

		// 設定 JWT 有效期

		builder.expiration(new Date(System.currentTimeMillis() + Duration.ofHours(2).toMillis()));

		// 添加 header

		builder.header().add("typ", "JWT").and();

		builder.header().add("alg", "HS256").and();

		// 設定 JWT 簽名

		builder.signWith(Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256);

		// 建立並返回 JWT

		return builder.compact();

	}

	/**
	 * 
	 * 統計在線人數
	 * 
	 * **@return**
	 * 
	 */

	public Long countValidAccessTokens() {

		List<String> keys = Lists.newArrayList(redissonClient.getKeys().getKeysByPattern("access_token:*"));

		long validTokenCount = keys.stream().filter(key -> {

			RBucket<Object> bucket = redissonClient.getBucket(key);

			return bucket.remainTimeToLive() > 0;

		}).count();

		return validTokenCount;

	}

	// Mobile guest issuance below remains FCM-bound; Web issuance is independent.
	/**
	 * Mobile-only Redis guest-token issuance path. The Redis lock prevents
	 * concurrent issuance from overwriting FCM allowedJwt. Existing mobile response
	 * format remains unchanged.
	 */
	private String getOrCreateGuestToken(String guestId, FcmRecord fcmRecord) throws IOException {
		RLock lock = redissonClient.getLock("lock:jwt:" + guestId);
		boolean locked = false;
		try {
			// No fixed lease time: Redisson watchdog renews while the process owns the
			// lock.
			locked = lock.tryLock(5, TimeUnit.SECONDS);
			if (!locked) {
				throw new IllegalStateException("Guest JWT issuance is busy");
			}

			RBucket<String> bucket = redissonClient.getBucket("jwt:" + guestId);
			String token = bucket.get();
			boolean valid = false;
			if (token != null) {
				try {
					Claims claims = parseJwtToken(token);
					valid = guestId.equals(claims.getSubject()) && claims.getExpiration() != null
							&& claims.getExpiration().toInstant().isAfter(Instant.now().plusSeconds(60));
				} catch (JwtException | IllegalArgumentException ex) {
					valid = false;
				}
			}

			if (!valid) {
				token = createTempAccessToken(guestId, "Guest");
			}

			// Preserve token binding to the verified FCM record.
			fcmRecord.setAllowedJwt(token);
			fcmRecordService.save(fcmRecord, token);

			if (!valid) {
				Claims issued = parseJwtToken(token);
				Duration ttl = Duration.between(Instant.now(), issued.getExpiration().toInstant());
				if (ttl.isNegative() || ttl.isZero()) {
					throw new JwtException("Issued JWT already expired");
				}
				bucket.set(token, ttl);
			}
			return token;
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while issuing guest JWT", ex);
		} finally {
			if (locked && lock.isHeldByCurrentThread()) {
				lock.unlock();
			}
		}
	}

	private String requireFcmToken(TempTokenVo request) {
		if (request == null || request.getToken() == null || request.getToken().isBlank()) {
			throw new IllegalArgumentException("FCM token is required");
		}
		return request.getToken();
	}

	private String guestIdFor(String fcmToken) {
		return DigestUtils.md5Hex(fcmToken).toUpperCase(Locale.ROOT);
	}

	/** Only accept the web-issued JWT type and its matching Redis session. */
	public ResponseEntity<Map<String, Object>> getWebSession(String cookieToken,
			JwtAuthenticationToken authentication) {
		if (cookieToken == null || cookieToken.isBlank() || authentication == null
				|| !authentication.isAuthenticated()) {
			return webSessionUnauthorized();
		}
		Jwt jwt = authentication.getToken();
		String token = jwt.getTokenValue();
		String guestId = jwt.getSubject();
		Instant expiresAt = jwt.getExpiresAt();

		if (!secureTokenEquals(cookieToken, token) || !"web_guest".equals(jwt.getClaimAsString("token_type"))
				|| guestId == null || !guestId.matches("[0-9a-fA-F-]{36}") || expiresAt == null
				|| !expiresAt.isAfter(Instant.now())) {
			return webSessionUnauthorized();
		}
		boolean isGuest = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority)
				.anyMatch("4"::equals);
		if (!isGuest)
			return webSessionUnauthorized();

		String current = redissonClient.<String>getBucket("web:jwt:" + guestId).get();
		if (!secureTokenEquals(token, current))
			return webSessionUnauthorized();

		return ResponseEntity.ok().cacheControl(CacheControl.noStore())
				.body(Map.of("authenticated", true, "exp", expiresAt.getEpochSecond()));
	}

	private boolean secureTokenEquals(

			String expected,

			String actual

	) {

		if (expected == null || actual == null) {

			return false;

		}

		return MessageDigest.isEqual(

				expected.getBytes(StandardCharsets.UTF_8),

				actual.getBytes(StandardCharsets.UTF_8)

		);

	}

	/**
	 * 
	 * Invalid Web Session response.
	 * 
	 */

	private ResponseEntity<Map<String, Object>> webSessionUnauthorized() {

		return ResponseEntity

				.status(HttpStatus.UNAUTHORIZED)

				.cacheControl(CacheControl.noStore())

				.body(Map.of(

						"authenticated", false

				));

	}

}
