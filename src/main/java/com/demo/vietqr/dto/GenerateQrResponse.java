package com.demo.vietqr.dto;

import lombok.Data;

@Data
public class GenerateQrResponse {
    private String bankCode;
    private String bankName;
    private String bankAccount;
    private String userBankName;
    private String amount;
    private String content;

    /** Chuỗi raw QR — dùng để render QR image trên frontend */
    private String qrCode;

    /** UUID của ảnh QR (dùng với VietQR image API nếu cần) */
    private String imgId;

    /** 1 = QR đã tồn tại từ trước, 0 = mới tạo */
    private int existing;

    private String transactionId;
    private String transactionRefId;

    /** Link trang hiển thị QR — có thể nhúng vào iframe hoặc redirect */
    private String qrLink;

    private String terminalCode;
    private String subTerminalCode;
    private String serviceCode;
    private String orderId;
    private String vaAccount;
}
