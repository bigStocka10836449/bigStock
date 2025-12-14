package com.bigstock.biz.service;

import java.util.Date;

import org.mapstruct.factory.Mappers;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bigstock.biz.builder.UserAccountBuilder;
import com.bigstock.biz.domain.vo.UserRegistryInfo;
import com.bigstock.sharedComponent.entity.UserAccount;
import com.bigstock.sharedComponent.service.RoleInfoService;
import com.bigstock.sharedComponent.service.UserAccountService;
import com.bigstock.sharedComponent.utils.SeqGenerator;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserRegistryService {

	public final UserAccountService userAccountService;

	public final EmailService emailService;
	
	public final OauthTokenService oauthTokenService;
	
	public final RoleInfoService roleInfoService;

	private final UserAccountBuilder userAccountBuilder = Mappers.getMapper(UserAccountBuilder.class);

	@Transactional(rollbackFor = { Exception.class })
	public void registryUser(UserRegistryInfo userRegistryInfo) throws MessagingException {
		UserAccount userAccount = userAccountBuilder.userRegistryInfoToUserAccount(userRegistryInfo);
		BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
		String id = SeqGenerator.createUserAccountId();
		userAccount.setId(id);
		userAccount.setUserPassword(passwordEncoder.encode(userRegistryInfo.getUserPassword()));
		userAccountService.save(userAccount);
		String registryToken = oauthTokenService.generateRegistryToken(id);
		emailService.sendHtmlMessage(registryToken, userAccount.getEmail(), "帳號註冊驗證信", "這封信由系統自動寄出，請勿回覆");
	}
	
	@Transactional(rollbackFor = { Exception.class })
	public void varifyUserRegistryToken(String registryToken) throws MessagingException {
		Claims clams = oauthTokenService.parseJwtToken(registryToken);
		if(!clams.getExpiration().after(new Date())) {
			throw new JwtException("token expired");
		}
		UserAccount userAccount = userAccountService.getById(clams.getSubject()).orElseThrow(() -> new JwtException("invalid token"));
		userAccount.setStatus("1");
		userAccount.setRoleId("3");
		userAccountService.save(userAccount);
		
	}
}
