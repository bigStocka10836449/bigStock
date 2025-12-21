package com.bigstock.biz.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.apache.http.HttpException;
import org.redisson.api.RBucket;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import com.bigstock.biz.vo.UserInloginInfo;
import com.bigstock.sharedComponent.entity.RoleInfo;
import com.bigstock.sharedComponent.entity.UserAccount;
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
	
	public ResponseEntity<?> getTmpToken(HttpServletRequest request, String guestId) throws IOException, HttpException {

		// 1. 取得 IP，優先從 X-Forwarded-For 中讀取（多個時取第一個），否則 fallback 到 remote IP
		String ip = Optional.ofNullable(request.getHeader("X-Forwarded-For"))
		        .map(xff -> xff.split(",")[0].trim())
		        .orElseGet(request::getRemoteAddr);

		// 2. 取得 User-Agent，預設為 unknown
		String userAgent = Optional.ofNullable(request.getHeader("User-Agent"))
		        .orElse("unknown");

		// 3. 將 IP + User-Agent 做 MD5 hash，作為限流 key
		String identifier = ip + ":" + userAgent;
		String key = "rl:guest-token:tb:" + DigestUtils.md5DigestAsHex(identifier.getBytes(StandardCharsets.UTF_8));
		long now = System.currentTimeMillis() / 1000;
		RScript script = redissonClient.getScript(StringCodec.INSTANCE);
		String scriptText = new String(
				Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("rate_limit_token_bucket.lua"))
						.readAllBytes(),
				StandardCharsets.UTF_8);

		Long allowed = script.eval(RScript.Mode.READ_WRITE, scriptText, RScript.ReturnType.INTEGER,
				Collections.singletonList(key), "1", // 每秒補 1 token
				"10", // 最大桶容量
				String.valueOf(now));
		if (allowed == null || allowed == 0) {
			throw new HttpException("Rate limit exceeded");
		}

		if (guestId == null || !(redissonClient.getBucket("guest:" + guestId)).isExists()) {
			guestId = UUID.randomUUID().toString();
			redissonClient.getBucket("guest:" + guestId).set("1", Duration.ofHours(1));
		}

		RBucket<String> jwtBucket = redissonClient.getBucket("jwt:" + guestId);
		String token = jwtBucket.get();

		if (token == null) {
			token = createTempAccessToken(guestId, "Guest");
			jwtBucket.set(token, Duration.ofHours(1));
		}
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

	    refreshTokenBucket.expire(Duration.ofHours(4)); //  延長 refreshToken 有效期

	    return ResponseEntity.ok()
                .header("X-Refreshed-Token", newAccessToken)
                .body(Map.of("accessToken", newAccessToken, "exp", expiration.getTime()));
	}

	public Claims parseJwtToken(String token) throws JwtException {
		Jws<Claims> jws = Jwts.parser().verifyWith(Keys.hmacShaKeyFor(secretKey.getBytes())).build()
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
		builder.signWith(Keys.hmacShaKeyFor(secretKey.getBytes()), Jwts.SIG.HS256);
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
		builder.signWith(Keys.hmacShaKeyFor(secretKey.getBytes()), Jwts.SIG.HS256);
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
		builder.signWith(Keys.hmacShaKeyFor(secretKey.getBytes()), Jwts.SIG.HS256);

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
		builder.signWith(Keys.hmacShaKeyFor(secretKey.getBytes()), Jwts.SIG.HS256);
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
		builder.expiration(new Date(System.currentTimeMillis() + Duration.ofHours(1).toMillis()));
		// 添加 header
		builder.header().add("typ", "JWT").and();
		builder.header().add("alg", "HS256").and();
		// 設定 JWT 簽名
		builder.signWith(Keys.hmacShaKeyFor(secretKey.getBytes()), Jwts.SIG.HS256);
		// 建立並返回 JWT
		return builder.compact();
    }
	
	
	/**
	 * 統計在線人數
	 * @return
	 */
	public Long countValidAccessTokens() {
		List<String> keys = Lists.newArrayList(redissonClient.getKeys().getKeysByPattern("access_token:*"));
		long validTokenCount = keys.stream().filter(key -> {
			RBucket<Object> bucket = redissonClient.getBucket(key);
			return bucket.remainTimeToLive() > 0;
		}).count();
		return validTokenCount;
	}
}
