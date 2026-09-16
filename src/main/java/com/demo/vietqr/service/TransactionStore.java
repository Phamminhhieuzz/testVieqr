package com.demo.vietqr.service;

import com.demo.vietqr.dto.TransactionSyncPayload;
import com.demo.vietqr.entity.QrOrder;
import com.demo.vietqr.entity.TransactionSyncEntity;
import com.demo.vietqr.repository.QrOrderRepository;
import com.demo.vietqr.repository.TransactionSyncRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Lưu giao dịch nhận từ VietQR vào database và đối chiếu với đơn hàng.
 * Ràng buộc UNIQUE trên transactionid ở tầng DB là lớp chống trùng cuối cùng.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionStore {

    private final TransactionSyncRepository transactionRepository;
    private final QrOrderRepository qrOrderRepository;

    @Transactional(readOnly = true)
    public Optional<TransactionSyncEntity> findByTransactionId(String transactionId) {
        return transactionRepository.findByTransactionid(transactionId);
    }

    @Transactional
    public TransactionSyncEntity save(TransactionSyncPayload payload) {
        TransactionSyncEntity entity = new TransactionSyncEntity();
        entity.setReftransactionid(UUID.randomUUID().toString());
        entity.setTransactionid(payload.getTransactionid());
        entity.setBankaccount(payload.getBankaccount());
        entity.setAmount(payload.getAmount());
        entity.setTransType(payload.getTransType().toUpperCase());
        entity.setContent(payload.getContent());
        entity.setVqrCode(VqrCodeExtractor.extract(payload.getContent()));
        entity.setReferencenumber(payload.getReferencenumber());
        entity.setOrderId(payload.getOrderId());
        entity.setTransactionTime(payload.getTransactiontime());
        entity.setTerminalCode(payload.getTerminalCode());
        entity.setSubTerminalCode(payload.getSubTerminalCode());
        entity.setServiceCode(payload.getServiceCode());
        entity.setUrlLink(payload.getUrlLink());
        entity.setSign(payload.getSign());

        TransactionSyncEntity saved = transactionRepository.save(entity);
        matchOrder(saved);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<TransactionSyncEntity> findAll() {
        return transactionRepository.findTop50ByOrderByIdDesc();
    }

    /**
     * Đối chiếu giao dịch với đơn hàng qua mã VQR, chỉ áp dụng cho giao dịch ghi có.
     * Không tìm thấy đơn là chuyện bình thường (khách chuyển tiền ngoài luồng tạo QR).
     */
    private void matchOrder(TransactionSyncEntity transaction) {
        if (!"C".equalsIgnoreCase(transaction.getTransType())) {
            return;
        }

        Optional<QrOrder> found = Optional.empty();

        // 1. Khớp qua mã VQR (chuẩn VietQR)
        if (transaction.getVqrCode() != null && !transaction.getVqrCode().isBlank()) {
            found = qrOrderRepository.findByVqrCode(transaction.getVqrCode());
        }

        // 2. Khớp qua order_id nếu có
        if (found.isEmpty() && transaction.getOrderId() != null && !transaction.getOrderId().isBlank()) {
            found = qrOrderRepository.findByOrderId(transaction.getOrderId());
        }

        // 3. Khớp qua nội dung chuyển khoản chứa mã đơn hàng
        if (found.isEmpty() && transaction.getContent() != null) {
            String contentUpper = transaction.getContent().toUpperCase();
            List<QrOrder> pendingOrders = qrOrderRepository.findByStatus(QrOrder.Status.PENDING);
            for (QrOrder pending : pendingOrders) {
                if (pending.getOrderId() != null && contentUpper.contains(pending.getOrderId().toUpperCase())) {
                    found = Optional.of(pending);
                    break;
                }
            }
        }

        if (found.isEmpty()) {
            log.info("Không tìm thấy đơn khớp giao dịch {} (content='{}', vqr='{}') — giao dịch vẫn được lưu lại",
                    transaction.getTransactionid(), transaction.getContent(), transaction.getVqrCode());
            return;
        }

        QrOrder order = found.get();
        if (order.getStatus() == QrOrder.Status.PAID) {
            log.info("Đơn {} đã thanh toán trước đó, bỏ qua", order.getOrderId());
            return;
        }

        order.setStatus(QrOrder.Status.PAID);
        order.setRefTransactionId(transaction.getReftransactionid());
        order.setPaidAmount(transaction.getAmount());
        order.setPaidAt(Instant.now());
        qrOrderRepository.save(order);

        if (!order.getAmount().equals(transaction.getAmount())) {
            log.warn("⚠️ Đơn {} lệch tiền: cần thu {} nhưng nhận {}",
                    order.getOrderId(), order.getAmount(), transaction.getAmount());
        }
        log.info("✅ Đơn {} đã thanh toán — {} VND, mã VQR {}",
                order.getOrderId(), transaction.getAmount(), transaction.getVqrCode());
    }
}
