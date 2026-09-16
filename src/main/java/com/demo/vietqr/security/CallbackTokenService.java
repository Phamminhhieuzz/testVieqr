package com.demo.vietqr.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

@Slf4j
@Component
public class CallbackTokenService {

    @Value("${vietqr-callback.secret}")
    private String secret;

    @Value("${vietqr-callback.expiration-seconds}")
    private long expirationSeconds;

    /**
     * HS512 (theo tài liệu VietQR) bắt buộc khoá >= 512 bit.
     * Băm secret qua SHA-512 để luôn ra đúng 64 byte, bất kể secret cấu hình dài bao nhiêu.
     */
    private SecretKey getKey() {
        try {
            byte[] keyBytes = MessageDigest.getInstance("SHA-512")
                    .digest(secret.getBytes(StandardCharsets.UTF_8));
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("JVM thiếu thuật toán SHA-512", e);
        }
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }

    public String generateToken(String subject) {
        return Jwts.builder()
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationSeconds * 1000))
                .signWith(getKey(), Jwts.SIG.HS512)
                .compact();
    }

    public boolean isValid(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims != null;
        } catch (Exception e) {
            log.warn("Callback token không hợp lệ: {}", e.getMessage());
            return false;
        }
    }
}