package com.demo.vietqr.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TokenGenerateResponse {
    private String access_token;
    private String token_type;
    private long expires_in;
}