-- =====================================================================
-- VietQR Demo - Schema MySQL
-- Import: mysql -u root -p < vietqr_schema.sql
--    hoac: MySQL Workbench > Server > Data Import > Import from Self-Contained File
-- =====================================================================

CREATE DATABASE IF NOT EXISTS vietqr_demo
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE vietqr_demo;

-- ---------------------------------------------------------------------
-- Giao dich nhan tu callback transaction-sync cua VietQR
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS transaction_sync;

CREATE TABLE transaction_sync (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    -- Ma giao dich do HE THONG CUA MINH sinh ra, tra ve cho VietQR
    reftransactionid    VARCHAR(36)     NOT NULL,

    -- Ma giao dich do VIETQR gui sang. UNIQUE = chong xu ly trung khi VietQR goi lai
    transactionid       VARCHAR(100)    NOT NULL,

    bankaccount         VARCHAR(50)     NOT NULL,
    amount              BIGINT          NOT NULL,
    trans_type          VARCHAR(1)      NOT NULL COMMENT 'C = ghi co, D = ghi no',
    content             VARCHAR(500)    NOT NULL COMMENT 'Noi dung chuyen khoan',

    -- Ma VQR trich tu content, vi VietQR gui orderId rong
    vqr_code            VARCHAR(50)     NULL,

    referencenumber     VARCHAR(200)    NOT NULL,
    order_id            VARCHAR(100)    NULL,

    transaction_time    BIGINT          NOT NULL COMMENT 'Epoch millis do VietQR gui',
    transaction_time_at DATETIME(3)
        GENERATED ALWAYS AS (FROM_UNIXTIME(transaction_time / 1000)) STORED
        COMMENT 'Ban doc duoc cua transaction_time',

    terminal_code       VARCHAR(50)     NULL,
    sub_terminal_code   VARCHAR(50)     NULL,
    service_code        VARCHAR(50)     NULL,
    url_link            VARCHAR(500)    NULL,
    sign                VARCHAR(500)    NULL,

    received_at         TIMESTAMP(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

    PRIMARY KEY (id),
    CONSTRAINT ck_trans_type CHECK (trans_type IN ('C','D')),
    UNIQUE KEY uk_transactionid (transactionid),
    UNIQUE KEY uk_reftransactionid (reftransactionid),
    KEY idx_vqr_code (vqr_code),
    KEY idx_order_id (order_id),
    KEY idx_referencenumber (referencenumber),
    KEY idx_received_at (received_at)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Giao dich nhan tu callback VietQR';

-- ---------------------------------------------------------------------
-- Don hang tao ra khi goi API sinh ma QR
-- Doi chieu voi giao dich qua vqr_code nam trong noi dung chuyen khoan
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS qr_order;

CREATE TABLE qr_order (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    order_id            VARCHAR(100)    NOT NULL COMMENT 'Ma don hang cua minh',

    -- Ma VQR trich tu noi dung QR vua tao. Day la khoa de doi chieu voi giao dich
    vqr_code            VARCHAR(50)     NULL,

    bank_account        VARCHAR(50)     NOT NULL,
    bank_code           VARCHAR(20)     NULL,
    amount              BIGINT          NOT NULL,
    content             VARCHAR(500)    NULL COMMENT 'Noi dung chuyen khoan in tren QR',
    qr_code             VARCHAR(1000)   NULL COMMENT 'Chuoi raw QR tra ve tu VietQR',

    status              VARCHAR(10)     NOT NULL DEFAULT 'PENDING',

    -- Dien khi doi chieu duoc giao dich tu callback
    ref_transaction_id  VARCHAR(36)     NULL,
    paid_amount         BIGINT          NULL,

    created_at          TIMESTAMP(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    paid_at             TIMESTAMP(3)    NULL,

    PRIMARY KEY (id),
    CONSTRAINT ck_order_status CHECK (status IN ('PENDING','PAID')),
    UNIQUE KEY uk_order_id (order_id),
    KEY idx_qr_order_vqr_code (vqr_code),
    KEY idx_status (status),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Don hang gan voi ma QR da sinh';

-- ---------------------------------------------------------------------
-- Du lieu mau: dung payload that VietQR da gui sang, de kiem tra nhanh
-- ---------------------------------------------------------------------
INSERT INTO transaction_sync
    (reftransactionid, transactionid, bankaccount, amount, trans_type,
     content, vqr_code, referencenumber, order_id, transaction_time)
VALUES
    ('28a79b28-a67e-483a-95c9-c110a39b1a1a',
     '38a537ba-a0a9-4652-aa2d-cbcc319cceaa',
     '2501200566666', 50000, 'C',
     'VQR471bacff47 Thanh toan don 001', 'VQR471bacff47',
     'customer-phmminhhiu-user26685-4d645070', '', 1789527723000);

-- ---------------------------------------------------------------------
-- Cau lenh tra cuu thuong dung
-- ---------------------------------------------------------------------

-- Xem giao dich moi nhat
-- SELECT reftransactionid, transactionid, amount, trans_type, vqr_code,
--        content, transaction_time_at, received_at
-- FROM transaction_sync
-- ORDER BY received_at DESC
-- LIMIT 50;

-- Tim theo ma VQR (vi orderId VietQR gui rong)
-- SELECT * FROM transaction_sync WHERE vqr_code = 'VQR471bacff47';

-- Tong tien ghi co theo ngay
-- SELECT DATE(transaction_time_at) AS ngay,
--        COUNT(*) AS so_giao_dich,
--        SUM(amount) AS tong_tien
-- FROM transaction_sync
-- WHERE trans_type = 'C'
-- GROUP BY DATE(transaction_time_at)
-- ORDER BY ngay DESC;
