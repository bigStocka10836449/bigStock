package com.bigstock.sharedComponent.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TurnstileVerifyResponse {

    private boolean success;

    private String hostname;

    private String action;

    private String cdata;

    @JsonProperty("challenge_ts")
    private String challengeTs;

    @JsonProperty("error-codes")
    private List<String> errorCodes;
}
