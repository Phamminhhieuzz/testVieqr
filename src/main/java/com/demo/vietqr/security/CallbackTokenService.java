package com.demo.vietqr.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component
public class CallbackTokenService {

    @Value("${vietqr-callback.secret}")
    private String secret;

    @Value("${vietqr-callback.expiration-seconds}")
    private long expirationSeconds;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }

    public String generateToken(String subject) {
        return Jwts.builder()
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationSeconds * 1000))
                .signWith(getKey(), Jwts.SIG.HS256)
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