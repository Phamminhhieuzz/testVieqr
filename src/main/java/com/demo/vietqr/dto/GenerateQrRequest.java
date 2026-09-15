package com.demo.vietqr.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL) // bỏ qua field null khi gửi lên VietQR
public class GenerateQrRequest {

    @NotBlank(message = "bankCode không được để trống")
    private String bankCode;

    @NotBlank(message = "bankAccount không được để trống")
    private String bankAccount;

    @NotBlank(message = "userBankName không được để trống")
    private String userBankName;

    @NotBlank(message = "content không được để trống")
    @Size(max = 23, message = "content tối đa 23 ký tự")
    private String content;

    /**
     * Loại QR:
     * 0 = QR động (dynamic)  — bắt buộc: amount, orderId, transType
     * 1 = QR tĩnh (static)   — bắt buộc: terminalCode
     * 3 = QR bán động        — bắt buộc: amount, terminalCode, serviceCode
     */
    @NotNull(message = "qrType không được để trống")
    private Integer qrType;

    // Bắt buộc khi qrType = 0 hoặc 3
    private Long amount;

    // Bắt buộc khi qrType = 0 (tối đa 13 ký tự)
    @Size(max = 13, message = "orderId tối đa 13 ký tự")
    private String orderId;

    // Bắt buộc khi qrType = 0; giá trị: "C" (ghi có) hoặc "D" (ghi nợ)
    private String transType;

    // Bắt buộc khi qrType = 1 hoặc 3
    private String terminalCode;

    // Bắt buộc khi qrType = 3
    private String serviceCode;

    private String subTerminalCode;
    private String urlLink;
    private String note;
}
