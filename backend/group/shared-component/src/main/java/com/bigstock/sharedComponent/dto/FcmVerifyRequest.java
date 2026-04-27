package com.bigstock.sharedComponent.dto;

import lombok.Data;

@Data
public class FcmVerifyRequest {
	private String fcmToken;
	private String challenge;
}
