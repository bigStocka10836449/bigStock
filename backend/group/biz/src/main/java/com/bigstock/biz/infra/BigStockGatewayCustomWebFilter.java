package com.bigstock.biz.infra;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import reactor.core.publisher.Mono;

@Component
public class BigStockGatewayCustomWebFilter implements WebFilter {

	@Value("${server.oauth2.secret-key}")
	private String secretKey;



	private static final Logger log = LoggerFactory.getLogger(BigStockGatewayCustomWebFilter.class);

	@Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        String host = request.getURI().getHost();
        if (!request.getPath().value().startsWith("/actuator/")) {
            log.info("Request URL: {}", request.getURI());
        }

        try {
            String path = request.getPath().value();
            if (path.startsWith("/actuator/") || path.startsWith("/auth/")
                    || path.startsWith("/api/guest-token") || path.startsWith("/api/refresh-token")
                    || path.startsWith("/api/biz/swagger") || path.startsWith("/gateway/swagger/")
                    || path.contains("/webjars/")) {
                return chain.filter(exchange);
            }

            String token = request.getHeaders().getFirst("Authorization");
            if (token == null || !token.startsWith("Bearer ")) {
                return unauthorized(response);
            }

            token = token.substring(7);
            Claims claims = parseJwtToken(token);
            String role = claims.get("role", String.class);

            if ("GUEST".equals(role)) {
                return chain.filter(exchange);
            }

            Date expiration = claims.getExpiration();
            if (expiration != null && new Date().after(expiration)) {
                return unauthorized(response); // token 已過期，返回 401，由前端觸發 refresh
            }

            return chain.filter(exchange);

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


	private Mono<Void> unauthorized(ServerHttpResponse response) {
		response.setStatusCode(HttpStatus.UNAUTHORIZED);
		return response.setComplete();
	}
}
