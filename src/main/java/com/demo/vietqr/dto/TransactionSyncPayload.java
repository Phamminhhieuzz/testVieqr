package com.demo.vietqr.dto;

import lombok.Data;

@Data
public class TransactionSyncPayload {

    private String bankaccount;
    private Long amount;
    private String transType;        // C = ghi có, D = ghi nợ
    private String content;
    private String transactionid;
    private Long transactiontime;    // epoch millis
    private String referencenumber;
    private String orderId;

    private String terminalCode;
    private String subTerminalCode;
    private String serviceCode;
    private String urlLink;
    private String sign;
}