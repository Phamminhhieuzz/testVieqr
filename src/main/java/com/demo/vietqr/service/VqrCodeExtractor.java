package com.demo.vietqr.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Mã đơn VietQR nhúng trong nội dung chuyển khoản, ví dụ "VQR471bacff47 Thanh toan don 001".
 * Đây là điểm nối duy nhất giữa đơn hàng và giao dịch, vì VietQR gửi orderId rỗng trong callback.
 */
public final class VqrCodeExtractor {

    private static final Pattern VQR_CODE = Pattern.compile("\\bVQR[A-Za-z0-9]+\\b");

    private VqrCodeExtractor() {
    }

    public static String extract(String content) {
        if (content == null) {
            return null;
        }
        Matcher matcher = VQR_CODE.matcher(content);
        return matcher.find() ? matcher.group() : null;
    }
}
