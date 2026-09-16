package com.demo.vietqr.controller;

import com.demo.vietqr.dto.TokenGenerateResponse;
import com.demo.vietqr.dto.TransactionSyncPayload;
import com.demo.vietqr.dto.TransactionSyncResponse;
import com.demo.vietqr.security.CallbackTokenService;
import com.demo.vietqr.service.TransactionStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class VietQrCallbackController {

    private final CallbackTokenService callbackTokenService;
    private final TransactionStore transactionStore;

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

        String invalidReason = validate(payload);
        if (invalidReason != null) {
            log.warn("Payload transaction-sync không hợp lệ: {} — transactionid={}",
                    invalidReason, payload.getTransactionid());
            return ResponseEntity.status(400).body(
                    TransactionSyncResponse.failed("INVALID_PAYLOAD", invalidReason)
            );
        }

        // VietQR gọi lại cùng transactionid khi chưa nhận được phản hồi:
        // trả đúng reftransactionid đã cấp lần đầu, không ghi nhận thêm lần nữa.
        var existing = transactionStore.findByTransactionId(payload.getTransactionid());
        if (existing.isPresent()) {
            log.info("Giao dịch trùng — transactionid={} đã xử lý trước đó, reftransactionid={}",
                    payload.getTransactionid(), existing.get().getReftransactionid());
            return ResponseEntity.ok(
                    TransactionSyncResponse.success(existing.get().getReftransactionid())
            );
        }

        var stored = transactionStore.save(payload);

        log.info("=== NHẬN TRANSACTION SYNC TỪ VIETQR ===");
        log.info("transactionid    : {}", stored.getTransactionid());
        log.info("reftransactionid : {}", stored.getReftransactionid());
        log.info("bankaccount      : {}", stored.getBankaccount());
        log.info("amount           : {}", stored.getAmount());
        log.info("transType        : {}", stored.getTransType());
        log.info("content          : {}", stored.getContent());
        log.info("mã VQR           : {}", stored.getVqrCode());
        log.info("referencenumber  : {}", stored.getReferencenumber());
        log.info("========================================");

        if ("C".equalsIgnoreCase(stored.getTransType())) {
            log.info("✅ Ghi có {} VND vào tài khoản {} — mã VQR={}",
                    stored.getAmount(), stored.getBankaccount(), stored.getVqrCode());
        }

        return ResponseEntity.ok(TransactionSyncResponse.success(stored.getReftransactionid()));
    }

    /** Tra cứu các giao dịch đã nhận từ VietQR. Dùng chính Basic Auth của callback. */
    @GetMapping("/api/transactions")
    public ResponseEntity<?> listTransactions(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        if (authorization == null || !authorization.startsWith("Basic ")) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        }
        String decoded = new String(
                Base64.getDecoder().decode(authorization.substring("Basic ".length()).trim()),
                StandardCharsets.UTF_8);
        String[] parts = decoded.split(":", 2);
        if (parts.length != 2 || !callbackUsername.equals(parts[0]) || !callbackPassword.equals(parts[1])) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        }

        var all = transactionStore.findAll();
        return ResponseEntity.ok(Map.of("total", all.size(), "transactions", all));
    }

    /** Trả về mô tả lỗi nếu payload không hợp lệ, null nếu hợp lệ. */
    private String validate(TransactionSyncPayload p) {
        if (isBlank(p.getTransactionid())) return "Thiếu transactionid";
        if (isBlank(p.getBankaccount())) return "Thiếu bankaccount";
        if (isBlank(p.getContent())) return "Thiếu content";
        if (isBlank(p.getReferencenumber())) return "Thiếu referencenumber";
        if (p.getTransactiontime() == null) return "Thiếu transactiontime";
        if (p.getAmount() == null || p.getAmount() <= 0) return "amount phải lớn hơn 0";
        if (!"C".equalsIgnoreCase(p.getTransType()) && !"D".equalsIgnoreCase(p.getTransType())) {
            return "transType phải là 'C' hoặc 'D'";
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /** Trả lỗi đúng định dạng VietQR khi body gửi sang không parse được. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> handleUnreadableBody(HttpMessageNotReadableException e) {
        log.warn("Body transaction-sync không đọc được: {}", e.getMessage());
        return ResponseEntity.status(400).body(
                TransactionSyncResponse.failed("MALFORMED_BODY", "Body không hợp lệ hoặc thiếu")
        );
    }
}