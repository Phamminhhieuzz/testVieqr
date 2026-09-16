package com.demo.vietqr.controller;

import com.demo.vietqr.dto.TokenGenerateResponse;
import com.demo.vietqr.dto.TransactionSyncPayload;
import com.demo.vietqr.dto.TransactionSyncResponse;
import com.demo.vietqr.security.CallbackTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
public class VietQrCallbackController {

    private final CallbackTokenService callbackTokenService;

    @Value("${vietqr-callback.username}")
    private String callbackUsername;

    @Value("${vietqr-callback.password}")
    private String callbackPassword;

    @PostMapping("/api/token_generate")
    public ResponseEntity<?> generateTokenForVietQR(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        if (authorization == null || !authorization.startsWith("Basic ")) {
            log.warn("VietQR gọi token_generate thiếu Basic Auth header");
            return ResponseEntity.status(400).body(Map.of(
                    "status", "FAILED",
                    "message", "Thiếu hoặc sai định dạng Authorization header"
            ));
        }

        String base64Credentials = authorization.substring("Basic ".length()).trim();
        String credentials;
        try {
            credentials = new String(Base64.getDecoder().decode(base64Credentials), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(Map.of(
                    "status", "FAILED",
                    "message", "Base64 không hợp lệ"
            ));
        }

        String[] parts = credentials.split(":", 2);
        if (parts.length != 2) {
            return ResponseEntity.status(400).body(Map.of(
                    "status", "FAILED",
                    "message", "Định dạng username:password không hợp lệ"
            ));
        }

        String username = parts[0];
        String password = parts[1];

        if (!callbackUsername.equals(username) || !callbackPassword.equals(password)) {
            log.warn("VietQR gọi token_generate sai credentials: username={}", username);
            return ResponseEntity.status(401).body(Map.of(
                    "status", "FAILED",
                    "message", "Sai username hoặc password"
            ));
        }

        String token = callbackTokenService.generateToken(username);
        log.info("Đã cấp token cho VietQR gọi Transaction Sync");

        return ResponseEntity.ok(new TokenGenerateResponse(
                token,
                "Bearer",
                callbackTokenService.getExpirationSeconds()
        ));
    }

    @PostMapping("/bank/api/transaction-sync")
    public ResponseEntity<?> receiveTransactionSync(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody TransactionSyncPayload payload) {

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(
                    TransactionSyncResponse.failed("UNAUTHORIZED", "Thiếu Bearer token")
            );
        }

        String token = authorization.substring("Bearer ".length()).trim();
        if (!callbackTokenService.isValid(token)) {
            log.warn("VietQR gọi transaction-sync với token không hợp lệ");
            return ResponseEntity.status(401).body(
                    TransactionSyncResponse.failed("INVALID_TOKEN", "Token không hợp lệ hoặc đã hết hạn")
            );
        }

        log.info("=== NHẬN TRANSACTION SYNC TỪ VIETQR ===");
        log.info("bankaccount      : {}", payload.getBankaccount());
        log.info("amount           : {}", payload.getAmount());
        log.info("transType        : {}", payload.getTransType());
        log.info("content          : {}", payload.getContent());
        log.info("transactionid    : {}", payload.getTransactionid());
        log.info("referencenumber  : {}", payload.getReferencenumber());
        log.info("orderId          : {}", payload.getOrderId());
        log.info("========================================");

        if ("C".equalsIgnoreCase(payload.getTransType())) {
            log.info("✅ Ghi có {} VND vào tài khoản {} — orderId={}",
                    payload.getAmount(), payload.getBankaccount(), payload.getOrderId());
        }

        String reftransactionid = UUID.randomUUID().toString();
        return ResponseEntity.ok(TransactionSyncResponse.success(reftransactionid));
    }
}