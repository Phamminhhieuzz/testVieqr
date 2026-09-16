package com.demo.vietqr.controller;

import com.demo.vietqr.dto.TransactionSyncPayload;
import com.demo.vietqr.dto.WebhookPayload;
import com.demo.vietqr.service.TransactionStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private final TransactionStore transactionStore;



    @PostMapping("/vietqr")
    public ResponseEntity<?> handlePaymentCallback(@RequestBody WebhookPayload payload) {
        log.info("=== NHẬN CALLBACK THANH TOÁN ===");
        log.info("OrderId       : {}", payload.getOrderId());
        log.info("Amount        : {} VND", payload.getAmount());
        log.info("Status        : {}", payload.getStatus());
        log.info("TransactionRef: {}", payload.getTransactionRefId());
        log.info("================================");

        if ("SUCCESS".equalsIgnoreCase(payload.getStatus())) {
            log.info("✅ Thanh toán thành công — orderId={}", payload.getOrderId());
        } else {
            log.warn("❌ Thất bại — orderId={}, status={}", payload.getOrderId(), payload.getStatus());
        }

        return ResponseEntity.ok(Map.of("code", "00", "message", "Received"));
    }
}