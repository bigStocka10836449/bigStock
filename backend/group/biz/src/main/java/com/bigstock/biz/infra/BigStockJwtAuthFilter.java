package com.bigstock.biz.infra;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.Objects;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.http.HttpException;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.bigstock.sharedComponent.entity.FcmRecord;
import com.bigstock.sharedComponent.service.FcmRecordService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class BigStockJwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(BigStockJwtAuthFilter.class);

    @Value("${server.oauth2.secret-key}")
    private String secretKey;
    
    @Autowired
	private RedissonClient redissonClient;
	
    @Autowired
	private FcmRecordService fcmRecordService;


    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        if (!path.startsWith("/actuator/")) {
            log.info("Request URL: {}", request.getRequestURI());
        }

        // 白名單 ,"/device/**"
        if (path.startsWith("/actuator/")
                || path.startsWith("/auth/")
                || path.startsWith("/api/guest-token")
                || path.startsWith("/api/refresh-token")
                || path.contains("/swagger")
                || path.contains("/webjars")||
                path.contains("/auth/") || path.contains("/stockShareholderStructure")|| path.contains("/financialCalendarImport")|| path.contains("/device") || path.contains("/ws")) {

            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            unauthorized(response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            Claims claims = parseJwtToken(token);
            Date expiration = claims.getExpiration();
            if (expiration != null && new Date().after(expiration)) {
            	unauthorized(response);
            	return;
            }

			FcmRecord fcmRecord = fcmRecordService.getFcmRecord(token);
			if(ObjectUtils.isEmpty(fcmRecord)) {
				  unauthorized(response);
			}
			// 3. 將 IP + User-Agent 做 MD5 hash，作為限流 key
			String identifier = fcmRecord.getFcmToken() ;
			String key = "rl:guest-token:tb:" + DigestUtils.md5DigestAsHex(identifier.getBytes(StandardCharsets.UTF_8));
			long now = System.currentTimeMillis() / 1000;
    		RScript script = redissonClient.getScript(StringCodec.INSTANCE);
    		String scriptText = new String(
    				Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("rate_limit_token_bucket.lua"))
    						.readAllBytes(),
    				StandardCharsets.UTF_8);

    		Long allowed = script.eval(RScript.Mode.READ_WRITE, scriptText, RScript.ReturnType.INTEGER,
    				Collections.singletonList(key), "4", // 每秒補 10 token
    				"30", // 最大桶容量
    				String.valueOf(now));
    		if (allowed == null || allowed == 0) {
    			throw new HttpException("Rate limit exceeded");
    		}
            String role = claims.get("role", String.class);
            if ("Guest".equals(role)) {
                filterChain.doFilter(request, response);
                return;
            }


            // 若之後要放入 SecurityContext，可在這裡處理
            filterChain.doFilter(request, response);

        } catch (JwtException e) {
            unauthorized(response);
        } catch (HttpException e) {
        	ratelimitexceeded(response);
		}
    }

    private Claims parseJwtToken(String token) {
        Jws<Claims> jws = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(secretKey.getBytes()))
                .build()
                .parseSignedClaims(token);
        return jws.getPayload();
    }

    private void unauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.getWriter().write("Unauthorized");
    }
    private void ratelimitexceeded(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.BANDWIDTH_LIMIT_EXCEEDED.value());
        response.getWriter().write("Rate limit exceeded");
    }
}