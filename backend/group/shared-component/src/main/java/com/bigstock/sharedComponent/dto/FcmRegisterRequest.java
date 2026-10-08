package com.bigstock.sharedComponent.dto;

import com.bigstock.sharedComponent.enums.DeviceType;

import lombok.Data;

@Data
public class FcmRegisterRequest {
	  private String fcmToken;
	  private DeviceType deviceType;
}
