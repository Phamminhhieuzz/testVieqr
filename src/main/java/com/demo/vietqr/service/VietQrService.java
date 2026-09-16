package com.demo.vietqr.service;

import com.demo.vietqr.dto.GenerateQrRequest;
import com.demo.vietqr.dto.GenerateQrResponse;
import com.demo.vietqr.entity.QrOrder;
import com.demo.vietqr.repository.QrOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * Tạo mã QR thanh toán VietQR.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VietQrService {

    private final RestTemplate restTemplate;
    private final VietQrTokenService tokenService;
    private final QrOrderRepository qrOrderRepository;

    @Value("${vietqr.base-url}")
    private String baseUrl;

    public GenerateQrResponse generateQr(GenerateQrRequest qrRequest) {
        validateRequest(qrRequest);

        String token = tokenService.getValidToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        HttpEntity<GenerateQrRequest> request = new HttpEntity<>(qrRequest, headers);

        log.debug("Gọi VietQR generate QR — bankCode={}, qrType={}, amount={}",
                qrRequest.getBankCode(), qrRequest.getQrType(), qrRequest.getAmount());

        try {
            ResponseEntity<GenerateQrResponse> response = restTemplate.exchange(
                    baseUrl + "/qr/generate-customer",
                    HttpMethod.POST,
                    request,
                    GenerateQrResponse.class
            );

            GenerateQrResponse body = response.getBody();
            if (body == null) {
                throw new IllegalStateException("VietQR trả về response rỗng");
            }

            saveOrder(qrRequest, body);

            log.info("Tạo QR thành công — orderId={}, transactionRefId={}",
                    body.getOrderId(), body.getTransactionRefId());
            return body;

        } catch (HttpClientErrorException e) {
            log.error("VietQR trả lỗi HTTP {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Lỗi từ VietQR [" + e.getStatusCode() + "]: " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("Lỗi khi gọi VietQR generate QR: {}", e.getMessage());
            throw new RuntimeException("Gọi VietQR thất bại: " + e.getMessage(), e);
        }
    }

    /**
     * Ghi lại đơn hàng để sau này đối chiếu với giao dịch nhận từ callback.
     * Lỗi lưu đơn không được làm hỏng việc trả mã QR cho khách.
     */
    private void saveOrder(GenerateQrRequest request, GenerateQrResponse response) {
        String orderId = response.getOrderId() != null && !response.getOrderId().isBlank()
                ? response.getOrderId()
                : request.getOrderId();
        if (orderId == null || orderId.isBlank()) {
            log.warn("QR tạo ra không có orderId — bỏ qua việc lưu đơn");
            return;
        }

        try {
            QrOrder order = qrOrderRepository.findByOrderId(orderId).orElseGet(QrOrder::new);
            order.setOrderId(orderId);
            order.setVqrCode(VqrCodeExtractor.extract(response.getContent()));
            order.setBankAccount(response.getBankAccount() != null
                    ? response.getBankAccount() : request.getBankAccount());
            order.setBankCode(response.getBankCode() != null
                    ? response.getBankCode() : request.getBankCode());
            order.setAmount(request.getAmount());
            order.setContent(response.getContent());
            order.setQrCode(response.getQrCode());
            order.setStatus(QrOrder.Status.PENDING);
            order.setRefTransactionId(null);
            order.setPaidAmount(null);
            order.setPaidAt(null);
            qrOrderRepository.save(order);

            log.info("Đã lưu đơn {} — mã VQR {}, trạng thái PENDING", orderId, order.getVqrCode());
        } catch (Exception e) {
            log.error("Không lưu được đơn {}: {}", orderId, e.getMessage());
        }
    }

    /**
     * Validate business rules theo tài liệu VietQR.
     */
    private void validateRequest(GenerateQrRequest req) {
        int qrType = req.getQrType();

        if (qrType == 0) {
            // QR động: bắt buộc amount, orderId, transType
            if (req.getAmount() == null || req.getAmount() <= 0)
                throw new IllegalArgumentException("QR động (qrType=0): amount bắt buộc và phải > 0");
            if (req.getOrderId() == null || req.getOrderId().isBlank())
                throw new IllegalArgumentException("QR động (qrType=0): orderId bắt buộc");
            if (req.getTransType() == null || (!req.getTransType().equals("C") && !req.getTransType().equals("D")))
                throw new IllegalArgumentException("QR động (qrType=0): transType phải là 'C' hoặc 'D'");
        }

        if (qrType == 1) {
            // QR tĩnh: bắt buộc terminalCode
            if (req.getTerminalCode() == null || req.getTerminalCode().isBlank())
                throw new IllegalArgumentException("QR tĩnh (qrType=1): terminalCode bắt buộc");
        }

        if (qrType == 3) {
            // QR bán động: bắt buộc amount, terminalCode, serviceCode
            if (req.getAmount() == null || req.getAmount() <= 0)
                throw new IllegalArgumentException("QR bán động (qrType=3): amount bắt buộc và phải > 0");
            if (req.getTerminalCode() == null || req.getTerminalCode().isBlank())
                throw new IllegalArgumentException("QR bán động (qrType=3): terminalCode bắt buộc");
            if (req.getServiceCode() == null || req.getServiceCode().isBlank())
                throw new IllegalArgumentException("QR bán động (qrType=3): serviceCode bắt buộc");
        }
    }
}
