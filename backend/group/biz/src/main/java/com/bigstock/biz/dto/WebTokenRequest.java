package com.bigstock.biz.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WebTokenRequest(@NotBlank @Size(max = 2048) String turnstileToken) {
}
