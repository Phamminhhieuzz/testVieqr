package com.demo.vietqr.service;

import com.demo.vietqr.dto.TransactionSyncPayload;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Kho giao dịch nhận từ VietQR, lưu trong bộ nhớ.
 * Khoá theo transactionid của VietQR để chống xử lý trùng khi họ gọi lại.
 */
@Component
public class TransactionStore {

    private static final int MAX_TRANSACTIONS = 500;

    /** Mã đơn VietQR nhúng trong nội dung chuyển khoản, ví dụ "VQR471bacff47 Thanh toan don 001". */
    private static final Pattern VQR_CODE = Pattern.compile("\\bVQR[A-Za-z0-9]+\\b");

    private final Map<String, StoredTransaction> byTransactionId =
            new LinkedHashMap<>(16, 0.75f, false) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, StoredTransaction> eldest) {
                    return size() > MAX_TRANSACTIONS;
                }
            };

    public synchronized Optional<StoredTransaction> findByTransactionId(String transactionId) {
        return Optional.ofNullable(byTransactionId.get(transactionId));
    }

    public synchronized StoredTransaction save(TransactionSyncPayload payload) {
        StoredTransaction stored = new StoredTransaction(
                UUID.randomUUID().toString(),
                payload,
                extractVqrCode(payload.getContent()),
                Instant.now().toString()
        );
        byTransactionId.put(payload.getTransactionid(), stored);
        return stored;
    }

    public synchronized List<StoredTransaction> findAll() {
        return new ArrayList<>(byTransactionId.values());
    }

    private String extractVqrCode(String content) {
        if (content == null) {
            return null;
        }
        Matcher matcher = VQR_CODE.matcher(content);
        return matcher.find() ? matcher.group() : null;
    }

    @Getter
    public static class StoredTransaction {
        private final String reftransactionid;
        private final String transactionid;
        private final String bankaccount;
        private final Long amount;
        private final String transType;
        private final String content;
        private final String vqrCode;
        private final String referencenumber;
        private final String orderId;
        private final Long transactiontime;
        private final String receivedAt;

        StoredTransaction(String reftransactionid, TransactionSyncPayload p,
                          String vqrCode, String receivedAt) {
            this.reftransactionid = reftransactionid;
            this.transactionid = p.getTransactionid();
            this.bankaccount = p.getBankaccount();
            this.amount = p.getAmount();
            this.transType = p.getTransType();
            this.content = p.getContent();
            this.vqrCode = vqrCode;
            this.referencenumber = p.getReferencenumber();
            this.orderId = p.getOrderId();
            this.transactiontime = p.getTransactiontime();
            this.receivedAt = receivedAt;
        }
    }
}
