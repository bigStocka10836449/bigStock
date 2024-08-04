package com.bigstock.auth.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bigstock.auth.domain.vo.TokenInfo;
import com.bigstock.auth.domain.vo.UserInloginInfo;
import com.bigstock.auth.domain.vo.UserRegistryInfo;
import com.bigstock.auth.service.OauthTokenService;
import com.bigstock.auth.service.UserRegistryService;

//import io.micrometer.tracing.Span;
//import io.micrometer.tracing.Tracer;
//import io.micrometer.tracing.annotation.NewSpan;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "auth")
@Tag(name = "Auth Controller", description = "Auth for loggin and api")
@Slf4j
public class OauthController {

	private final OauthTokenService oauthTokenService;

	private final UserRegistryService userRegistryService;
	

	@Operation(summary = "token 刷新", description = "acctoken 若失效，但refresh token還有效時，gateway自動跟auth要新的access token刷新")
	@PostMapping(value = "refreshToken", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String refreshToken(@RequestBody TokenInfo refreshTokenInfo) {
		return oauthTokenService.refreshToken(refreshTokenInfo.getRefreshToken());
	}

	@Operation(summary = "使用者登入", description = "auth 產生 access token 跟 refresh token ，回傳access token")
	@PostMapping(value = "login", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String login(@RequestBody UserInloginInfo userInloginInfo) {		return oauthTokenService.userLoginHandle(userInloginInfo);
	}

	@Operation(summary = "使用者註冊", description = "向Auth 註冊 使用者，若註冊成功，會發送驗證信", responses = {
			@ApiResponse(responseCode = "成功 200 不回東西") })
	@PostMapping(value = "userRegistry", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Object> userRegistry(@RequestBody UserRegistryInfo userRegistryInfo) {
		try {
			userRegistryService.registryUser(userRegistryInfo);
			return ResponseEntity.ok().build();
		} catch (MessagingException e) {
			log.error(e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
		}
	}
	
	@Operation(summary = "使用者註冊後的驗證", description = "驗證成功，取出要驗證的帳號，將狀態修改成已驗證，並綁定普通使用者", responses = {
			@ApiResponse(responseCode = "成功 200 不回東西") })
	@GetMapping(value = "verify")
	public ResponseEntity<Object> verifyRegistryToken(@RequestParam("token") String token) {
		try {
			userRegistryService.varifyUserRegistryToken(token);
			return ResponseEntity.ok().build();
		} catch (MessagingException e) {
			log.error(e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
		}
	}
	
	@Operation(summary = "取得在線人數", responses = {
			@ApiResponse(responseCode = "成功 200 回傳人數") })
	@GetMapping(value = "getRecentlyMembers")
	public ResponseEntity<String> getRecentlyMembers() {
		try {
			return ResponseEntity.ok(oauthTokenService.countValidAccessTokens().toString());
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
		}
	}
}
