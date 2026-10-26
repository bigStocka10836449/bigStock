package com.bigstock.sharedComponent.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WebFcmRegisterRequest
        extends FcmRegisterRequest {

    private String turnstileToken;
}