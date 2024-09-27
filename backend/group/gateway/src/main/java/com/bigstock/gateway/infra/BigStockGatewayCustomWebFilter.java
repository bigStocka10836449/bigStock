package com.bigstock.gateway.infra;

import java.time.Duration;

import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import com.bigstock.gateway.domain.vo.TokenInfo;
import com.bigstock.gateway.protocol.OAuth2Client;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import reactor.core.publisher.Mono;

@Component
public class BigStockGatewayCustomWebFilter implements WebFilter {

	@Autowired
	private RedissonClient redissonClient;
	@Value("${server.oauth2.secret-key}")
	private String secretKey;
//	@Value("${server.oauth2.url}")
//	private String oauth2Url;

	@Autowired
	OAuth2Client oauth2Client;

	private static final Logger log = LoggerFactory.getLogger(BigStockGatewayCustomWebFilter.class);

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

		ServerHttpRequest request = exchange.getRequest();
		ServerHttpResponse response = exchange.getResponse();
		try {
			if (request.getPath().value().startsWith("/actuator/") || request.getPath().value().startsWith("/auth/")
					|| request.getPath().value().startsWith("/api/biz/swagger")
					|| request.getPath().value().startsWith("/gateway/swagger/")
					|| request.getPath().value().contains("/webjars/")) {
				return chain.filter(exchange);
			}
			log.info("Request URL: {}", exchange.getRequest().getURI().toString());
			String token = request.getHeaders().getFirst("Authorization");
			if (token == null || !token.startsWith("Bearer ")) {
				return unauthorized(response);
			}

			token = token.substring(7);

			Claims claims = parseJwtToken(token);
			RBucket<Object> refreshToken = redissonClient.getBucket("refresh_token:" + claims.getSubject());
			RBucket<Object> accessTokenRB = redissonClient.getBucket("access_token:" + claims.getSubject());
			// 假設accessTokenRB不存在或client带的token与Redis的token不一样的时候，依样导回登入页
			if (!request.getPath().value().startsWith("/actuator/")

					&& !request.getPath().value().startsWith("/auth/")) {
				if ((!accessTokenRB.isExists() || !token.equals(accessTokenRB.get().toString()))
						&& !refreshToken.isExists()) {
					return unauthorized(response);
				}
			}
			if (accessTokenRB.isExists()) {
				refreshToken.expire(Duration.ofHours(4));
				accessTokenRB.expire(Duration.ofHours(1));
				return chain.filter(exchange);
			}

			// 如果 access token 无效，再进行 refresh token 的流程
			if (refreshToken.isExists()) {
				String accessToken = tryRefreshToken(refreshToken.get().toString());
				refreshToken.expire(Duration.ofHours(4));
				request.getHeaders().set("Authorization", accessToken);
				return chain.filter(exchange);
			}
			// 如果 access token 和 refresh token 都无效，則导回登入页
			return unauthorized(response);
		} catch (JwtException e) {
			return handleJwtException(response, e);
		}
	}

	private Claims parseJwtToken(String token) throws JwtException {
		Jws<Claims> jws = Jwts.parser().verifyWith(Keys.hmacShaKeyFor(secretKey.getBytes())).build()
				.parseSignedClaims(token);
		return jws.getPayload();
	}

	/**
	 * 使用refresh token 處理 token 刷新
	 * 
	 * @param claims
	 * @return
	 */
	private Mono<Void> handleJwtException(ServerHttpResponse response, JwtException e) {
		// 处理 JWT 异常的逻辑
		// 这里使用了假设的方法 handleJwtException，你需要实现它来处理 JWT 异常
		response.setStatusCode(HttpStatus.UNAUTHORIZED);
		return response.setComplete();
	}

	/**
	 * 從token的聲明(Claims) 取出 事先放入到acces token的角色資訊(roles) 跟 userName(Subject)
	 * ，並且把這些資訊放入到UsernamePasswordAuthenticationToken
	 * 讓Controller中有使用 @Secured @PreAuthorize 等註解可以生效
	 * 
	 * @param claims jwt 的聲明類
	 * @return Authentication
	 */
//	@SuppressWarnings("unchecked")
//	private Authentication createAuthentication(Claims claims) {
//		List<String> roles = (List<String>) claims.get("roles", List.class);
//		List<SimpleGrantedAuthority> authorities = roles.stream().map(SimpleGrantedAuthority::new)
//				.collect(Collectors.toList());
//		return new UsernamePasswordAuthenticationToken(claims.getSubject(), null, authorities);
//	}

//	private boolean handleTokenRefresh(Claims claims, RBucket<Object> refreshToken) {
//		if (refreshToken.isExists()) {
//			Authentication newAuthentication = tryRefreshToken(refreshToken.get().toString());
//			if (newAuthentication != null) {
//				SecurityContextHolder.getContext().setAuthentication(newAuthentication);
//				refreshToken.expire(Duration.ofHours(4));
//				return true;
//			}
//		}
//		return false;
//	}

//	private void handleJwtException(HttpServletResponse response, JwtException e)
//			throws IOException, java.io.IOException {
//		if (e instanceof MalformedJwtException) {
//			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid JWT token");
//		} else if (e instanceof ExpiredJwtException) {
//			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "JWT token expired");
//		} else {
//			response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
//		}
//	}

	/**
	 * 利用refreshToken 重新產生access token
	 * 
	 * @param refreshToken
	 * @return
	 */
	private String tryRefreshToken(String refreshToken) {

		// Create an OAuth2Client instance
//		OAuth2Client oAuth2Client = Feign.builder().contract(new SpringMvcContract()).client(new OkHttpClient())
//				.encoder(new JacksonEncoder()).target(OAuth2Client.class, oauth2Url);
		TokenInfo refreshTokenInfo = new TokenInfo();
		refreshTokenInfo.setRefreshToken(refreshToken);
		// Call the refresh token API
		String newAccessToken = oauth2Client.refreshToken(refreshTokenInfo);
//		String newAccessToken = oAuth2Client.refreshToken(refreshTokenInfo);

		// Store the new refresh token in Redis using redissonClient
		return newAccessToken;
//		return createAuthentication(parseJwtToken(newAccessToken));
	}

	private Mono<Void> unauthorized(ServerHttpResponse response) {
		response.setStatusCode(HttpStatus.UNAUTHORIZED);
		return response.setComplete();
	}
}
