package com.demo.vietqr.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class WebhookPayload {

    @JsonProperty("transactionRefId")
    private String transactionRefId;

    @JsonProperty("orderId")
    private String orderId;

    @JsonProperty("amount")
    private Long amount;

    @JsonProperty("content")
    private String content;

    @JsonProperty("bankAccount")
    private String bankAccount;

    @JsonProperty("bankCode")
    private String bankCode;

    @JsonProperty("status")
    private String status;

    @JsonProperty("transactionTime")
    private Long transactionTime;
}