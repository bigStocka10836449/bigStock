package com.bigstock.biz.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

	private final JavaMailSender emailSender;

	private final TemplateEngine templateEngine;
	

	@Value("${server.registry-verify-url}")
	private String registryVerifyUrl;

	@Value("${spring.mail.from}")
	private String mailFrom;

	@Value("${server.oauth2.secret-key}")
	private String secretKeyString;


	@Autowired
	public EmailService(JavaMailSender emailSender, TemplateEngine templateEngine) throws Exception {
		this.emailSender = emailSender;
		this.templateEngine = templateEngine;
	}


	public void sendHtmlMessage(String userRegistrytoken,String to, String subject, String content) throws MessagingException {
		// 生成唯一标识符
		// 生成动态URL
		String url = registryVerifyUrl + userRegistrytoken;


		// 准备模板数据
		Map<String, Object> templateModel = new HashMap<>();
		templateModel.put("subject", subject);
		templateModel.put("content", content);
		templateModel.put("url", url);

		Context context = new Context();
		context.setVariables(templateModel);
		String htmlBody = templateEngine.process("emailTemplate", context);

		MimeMessage message = emailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
		helper.setFrom(mailFrom);
		helper.setTo(to);
		helper.setSubject(subject);
		helper.setText(htmlBody, true);
		emailSender.send(message);
	}

}
