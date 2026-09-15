package com.demo.vietqr.service;

import com.demo.vietqr.dto.TokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class VietQrTokenService {

    private final RestTemplate restTemplate;

    @Value("${vietqr.username}")
    private String username;

    @Value("${vietqr.password}")
    private String password;

    @Value("${vietqr.base-url}")
    private String baseUrl;

    private final AtomicReference<String> cachedToken = new AtomicReference<>();
    private volatile Instant tokenExpireAt = Instant.EPOCH;

    private static final int REFRESH_BUFFER_SECONDS = 30;

    public String getValidToken() {
        if (isTokenExpired()) {
            synchronized (this) {
                if (isTokenExpired()) {
                    refreshToken();
                }
            }
        }
        return cachedToken.get();
    }

    private boolean isTokenExpired() {
        return Instant.now().isAfter(tokenExpireAt.minusSeconds(REFRESH_BUFFER_SECONDS));
    }

    private void refreshToken() {
        log.info("Token het han hoac chua co - dang lay token moi tu VietQR...");

        String credentials = username + ":" + password;
        String basicAuth = Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Basic " + basicAuth);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<TokenResponse> response = restTemplate.exchange(
                    baseUrl + "/token_generate",
                    HttpMethod.POST,
                    request,
                    TokenResponse.class
            );

            TokenResponse body = response.getBody();
            if (body == null || body.getAccessToken() == null) {
                throw new IllegalStateException("VietQR tra ve token rong");
            }

            cachedToken.set(body.getAccessToken());

            long rawExpiry = body.getExpiresIn();
            long durationSeconds;
            if (rawExpiry <= 0) {
                durationSeconds = 300L;
                log.warn("expires_in={} khong hop le, dung mac dinh 300s", rawExpiry);
            } else if (rawExpiry <= 86400L) {
                durationSeconds = rawExpiry;
            } else {
                long now = Instant.now().getEpochSecond();
                durationSeconds = rawExpiry - now;
                if (durationSeconds <= 0 || durationSeconds > 86400L) {
                    durationSeconds = 300L;
                }
                log.warn("expires_in={} trong nhu Unix timestamp, tinh duration={}s", rawExpiry, durationSeconds);
            }

            tokenExpireAt = Instant.now().plusSeconds(durationSeconds);
            log.info("Lay token thanh cong, het han sau {}s", durationSeconds);

        } catch (Exception e) {
            log.error("Loi khi lay token tu VietQR: {}", e.getMessage());
            throw new RuntimeException("Khong the lay token VietQR: " + e.getMessage(), e);
        }
    }
}