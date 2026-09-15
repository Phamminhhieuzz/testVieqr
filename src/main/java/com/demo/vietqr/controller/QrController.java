package com.demo.vietqr.controller;

import com.demo.vietqr.dto.GenerateQrRequest;
import com.demo.vietqr.dto.GenerateQrResponse;
import com.demo.vietqr.service.VietQrService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/qr")
@RequiredArgsConstructor
public class QrController {

    private final VietQrService vietQrService;

    /**
     * Tạo mã QR thanh toán VietQR.
     *
     * POST /api/qr/generate
     *
     * Body mẫu (QR động):
     * {
     *   "bankCode": "MB",
     *   "bankAccount": "0123456789",
     *   "userBankName": "NGUYEN VAN A",
     *   "content": "Thanh toan don 001",
     *   "qrType": 0,
     *   "amount": 50000,
     *   "orderId": "ORDER001",
     *   "transType": "C"
     * }
     */
    @PostMapping("/generate")
    public ResponseEntity<?> generateQr(@Valid @RequestBody GenerateQrRequest request) {
        try {
            GenerateQrResponse response = vietQrService.generateQr(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            // Lỗi validate business rule
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "FAILED",
                    "message", e.getMessage()
            ));
        } catch (RuntimeException e) {
            // Lỗi từ VietQR hoặc network
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "FAILED",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Health check — kiểm tra app còn sống không.
     * GET /api/qr/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}
