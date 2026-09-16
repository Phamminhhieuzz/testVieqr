package com.demo.vietqr.controller;

import com.demo.vietqr.dto.GenerateQrRequest;
import com.demo.vietqr.dto.GenerateQrResponse;
import com.demo.vietqr.dto.TransactionSyncPayload;
import com.demo.vietqr.entity.QrOrder;
import com.demo.vietqr.repository.QrOrderRepository;
import com.demo.vietqr.service.TransactionStore;
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
    private final QrOrderRepository qrOrderRepository;
    private final TransactionStore transactionStore;

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
     * Tra cứu đơn đã thanh toán chưa.
     * GET /api/qr/order/{orderId}
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<?> getOrder(@PathVariable String orderId) {
        return qrOrderRepository.findByOrderId(orderId)
                .<ResponseEntity<?>>map(order -> ResponseEntity.ok(Map.of(
                        "orderId", order.getOrderId(),
                        "vqrCode", order.getVqrCode() == null ? "" : order.getVqrCode(),
                        "amount", order.getAmount(),
                        "status", order.getStatus().name(),
                        "paidAmount", order.getPaidAmount() == null ? 0 : order.getPaidAmount(),
                        "refTransactionId", order.getRefTransactionId() == null ? "" : order.getRefTransactionId(),
                        "createdAt", String.valueOf(order.getCreatedAt()),
                        "paidAt", String.valueOf(order.getPaidAt())
                )))
                .orElseGet(() -> ResponseEntity.status(404).body(Map.of(
                        "status", "NOT_FOUND",
                        "message", "Không tìm thấy đơn " + orderId
                )));
    }

    /**
     * Giả lập thanh toán cho đơn hàng (mô phỏng callback ngân hàng VietQR).
     * POST /api/qr/order/{orderId}/simulate-pay
     */
    @PostMapping("/order/{orderId}/simulate-pay")
    public ResponseEntity<?> simulatePay(@PathVariable String orderId) {
        var opt = qrOrderRepository.findByOrderId(orderId);
        if (opt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of(
                    "status", "FAILED",
                    "message", "Không tìm thấy đơn " + orderId
            ));
        }
        var order = opt.get();
        if (order.getStatus() == QrOrder.Status.PAID) {
            return ResponseEntity.ok(Map.of(
                    "status", "ALREADY_PAID",
                    "message", "Đơn đã thanh toán trước đó",
                    "orderId", orderId
            ));
        }

        TransactionSyncPayload payload = new TransactionSyncPayload();
        payload.setTransactionid("SIM-" + System.currentTimeMillis());
        payload.setBankaccount(order.getBankAccount() != null ? order.getBankAccount() : "2501200566666");
        payload.setAmount(order.getAmount());
        payload.setTransType("C");
        payload.setContent(order.getContent() != null ? order.getContent() : (order.getVqrCode() + " Thanh toan don"));
        payload.setReferencenumber("SIM-REF-" + System.currentTimeMillis());
        payload.setTransactiontime(System.currentTimeMillis());
        payload.setOrderId(order.getOrderId());

        var saved = transactionStore.save(payload);
        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Đã giả lập callback thành công",
                "refTransactionId", saved.getReftransactionid(),
                "orderId", orderId,
                "amount", order.getAmount()
        ));
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
