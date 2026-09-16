package com.demo.vietqr.controller;

import com.demo.vietqr.dto.SepayWebhookPayload;
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

    /**
     * Webhook nhận biến động số dư thực tế từ SePay khi có tiền vào tài khoản ngân hàng.
     * POST /api/webhook/sepay
     */
    @PostMapping("/sepay")
    public ResponseEntity<?> handleSepayWebhook(@RequestBody SepayWebhookPayload payload) {
        log.info("=== NHẬN WEBHOOK TỪ SEPAY ===");
        log.info("ID             : {}", payload.getId());
        log.info("Gateway        : {}", payload.getGateway());
        log.info("Account Number : {}", payload.getAccountNumber());
        log.info("Amount         : {} VND", payload.getTransferAmount());
        log.info("Transfer Type  : {}", payload.getTransferType());
        log.info("Content        : {}", payload.getContent());
        log.info("Reference Code : {}", payload.getReferenceCode());
        log.info("=============================");

        // Chỉ xử lý giao dịch tiền vào
        if ("in".equalsIgnoreCase(payload.getTransferType())
                && payload.getTransferAmount() != null
                && payload.getTransferAmount() > 0) {

            String transactionId = payload.getReferenceCode() != null && !payload.getReferenceCode().isBlank()
                    ? "SEPAY-" + payload.getReferenceCode()
                    : "SEPAY-" + payload.getId();

            // Chống xử lý trùng lặp
            var existing = transactionStore.findByTransactionId(transactionId);
            if (existing.isPresent()) {
                log.info("Giao dịch SePay trùng: {} đã xử lý trước đó", transactionId);
                return ResponseEntity.ok(Map.of("success", true, "message", "Duplicate ignored"));
            }

            TransactionSyncPayload syncPayload = new TransactionSyncPayload();
            syncPayload.setTransactionid(transactionId);
            syncPayload.setBankaccount(payload.getAccountNumber());
            syncPayload.setAmount(payload.getTransferAmount());
            syncPayload.setTransType("C");
            syncPayload.setContent(payload.getContent());
            syncPayload.setReferencenumber(payload.getReferenceCode() != null
                    ? payload.getReferenceCode()
                    : String.valueOf(payload.getId()));
            syncPayload.setTransactiontime(System.currentTimeMillis());

            var saved = transactionStore.save(syncPayload);
            log.info("✅ SePay Webhook đã xử lý và đối chiếu đơn thành công: refTxId={}", saved.getReftransactionid());
        }

        // SePay yêu cầu trả về {"success": true}
        return ResponseEntity.ok(Map.of("success", true));
    }

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