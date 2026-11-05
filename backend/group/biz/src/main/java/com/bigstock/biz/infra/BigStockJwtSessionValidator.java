package com.bigstock.biz.infra;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collection;

import org.redisson.api.RedissonClient;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import org.springframework.stereotype.Component;

import com.bigstock.sharedComponent.entity.FcmRecord;
import com.bigstock.sharedComponent.service.FcmRecordService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class BigStockJwtSessionValidator
        implements OAuth2TokenValidator<Jwt> {

    private final RedissonClient redissonClient;

    private final FcmRecordService fcmRecordService;

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {

        String guestId = jwt.getSubject();

        if (guestId == null || guestId.isBlank()) {
            return invalid();
        }

        Object roles = jwt.getClaims().get("roles");

        if (!(roles instanceof Collection<?> roleList)
                || roleList.stream()
                        .noneMatch(role ->
                                "4".equals(String.valueOf(role)))) {
            return invalid();
        }

        String tokenType = jwt.getClaimAsString("token_type");

        String token = jwt.getTokenValue();

        try {

            // ==========================================
            // Web Guest
            // ==========================================
            if ("web_guest".equals(tokenType)) {

                String storedJwt = redissonClient
                        .<String>getBucket("web:jwt:" + guestId)
                        .get();

                return secureEquals(token, storedJwt)
                        ? OAuth2TokenValidatorResult.success()
                        : invalid();
            }

            // ==========================================
            // Legacy Mobile Guest
            // ==========================================
            if (tokenType == null || tokenType.isBlank()) {

                String storedJwt = redissonClient
                        .<String>getBucket("jwt:" + guestId)
                        .get();

                if (!secureEquals(token, storedJwt)) {
                    return invalid();
                }

                FcmRecord fcmRecord =
                        fcmRecordService.getFcmRecord(token);

                if (fcmRecord == null
                        || fcmRecord.getFcmToken() == null
                        || fcmRecord.getFcmToken().isBlank()
                        || !secureEquals(
                                token,
                                fcmRecord.getAllowedJwt())) {

                    return invalid();
                }

                return OAuth2TokenValidatorResult.success();
            }

            // 未知 token_type 一律拒絕
            return invalid();

        } catch (RuntimeException  | IOException ex) {
            // Redis / FCM 查詢異常：拒絕認證，不得放行
            return invalid();
        } 
    }

    private boolean secureEquals(
            String left,
            String right
    ) {

        return left != null
                && right != null
                && MessageDigest.isEqual(
                        left.getBytes(StandardCharsets.UTF_8),
                        right.getBytes(StandardCharsets.UTF_8)
                );
    }

    private OAuth2TokenValidatorResult invalid() {

        return OAuth2TokenValidatorResult.failure(
                new OAuth2Error(
                        "invalid_token",
                        "JWT session validation failed",
                        null
                )
        );
    }
}