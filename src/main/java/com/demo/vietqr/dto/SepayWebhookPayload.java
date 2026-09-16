package com.demo.vietqr.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SepayWebhookPayload {

    private Long id;
    private String gateway;
    private String transactionDate;
    private String accountNumber;
    private String subAccount;
    private String code;
    private String content;
    private String transferType;    // "in" (tiền vào) hoặc "out" (tiền ra)
    private String description;
    private Long transferAmount;
    private Long accumulated;
    private String referenceCode;
}
