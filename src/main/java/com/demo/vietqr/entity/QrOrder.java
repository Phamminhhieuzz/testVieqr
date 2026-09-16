package com.demo.vietqr.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "qr_order")
@Getter
@Setter
@NoArgsConstructor
public class QrOrder {

    public enum Status { PENDING, PAID }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, unique = true, length = 100)
    private String orderId;

    /** Mã VQR trích từ nội dung QR — khoá để đối chiếu với giao dịch nhận về. */
    @Column(name = "vqr_code", length = 50)
    private String vqrCode;

    @Column(name = "bank_account", nullable = false, length = 50)
    private String bankAccount;

    @Column(name = "bank_code", length = 20)
    private String bankCode;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Column(name = "content", length = 500)
    private String content;

    @Column(name = "qr_code", length = 1000)
    private String qrCode;

    // Hibernate 6 mac dinh anh xa enum sang kieu ENUM cua MySQL - ep dung VARCHAR cho khop schema
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 10)
    private Status status = Status.PENDING;

    @Column(name = "ref_transaction_id", length = 36)
    private String refTransactionId;

    @Column(name = "paid_amount")
    private Long paidAmount;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "paid_at")
    private Instant paidAt;
}
