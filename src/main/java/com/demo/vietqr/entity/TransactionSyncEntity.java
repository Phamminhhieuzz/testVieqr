package com.demo.vietqr.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "transaction_sync")
@Getter
@Setter
@NoArgsConstructor
public class TransactionSyncEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reftransactionid", nullable = false, unique = true, length = 36)
    private String reftransactionid;

    /** Mã giao dịch VietQR gửi sang. UNIQUE ở DB để chống xử lý trùng. */
    @Column(name = "transactionid", nullable = false, unique = true, length = 100)
    private String transactionid;

    @Column(name = "bankaccount", nullable = false, length = 50)
    private String bankaccount;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Column(name = "trans_type", nullable = false, length = 1)
    private String transType;

    @Column(name = "content", nullable = false, length = 500)
    private String content;

    @Column(name = "vqr_code", length = 50)
    private String vqrCode;

    @Column(name = "referencenumber", nullable = false, length = 200)
    private String referencenumber;

    @Column(name = "order_id", length = 100)
    private String orderId;

    @Column(name = "transaction_time", nullable = false)
    private Long transactionTime;

    /** Cột STORED GENERATED trong DB — chỉ đọc, không ghi. */
    @Column(name = "transaction_time_at", insertable = false, updatable = false)
    private Instant transactionTimeAt;

    @Column(name = "terminal_code", length = 50)
    private String terminalCode;

    @Column(name = "sub_terminal_code", length = 50)
    private String subTerminalCode;

    @Column(name = "service_code", length = 50)
    private String serviceCode;

    @Column(name = "url_link", length = 500)
    private String urlLink;

    @Column(name = "sign", length = 500)
    private String sign;

    @Column(name = "received_at", insertable = false, updatable = false)
    private Instant receivedAt;
}
