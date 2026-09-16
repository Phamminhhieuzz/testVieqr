package com.demo.vietqr.debug;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@RestController
@RequestMapping("/api/debug")
@RequiredArgsConstructor
public class DebugController {

    private final RequestRecorder recorder;

    @Value("${vietqr-callback.username}")
    private String callbackUsername;

    @Value("${vietqr-callback.password}")
    private String callbackPassword;

    @GetMapping("/requests")
    public ResponseEntity<?> listRequests(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        if (!isAuthorized(authorization)) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        }
        return ResponseEntity.ok(recorder.getAll());
    }

    @DeleteMapping("/requests")
    public ResponseEntity<?> clearRequests(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        if (!isAuthorized(authorization)) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        }
        recorder.clear();
        return ResponseEntity.ok(Map.of("message", "cleared"));
    }

    private boolean isAuthorized(String authorization) {
        if (authorization == null || !authorization.startsWith("Basic ")) {
            return false;
        }
        try {
            String decoded = new String(
                    Base64.getDecoder().decode(authorization.substring("Basic ".length()).trim()),
                    StandardCharsets.UTF_8);
            String[] parts = decoded.split(":", 2);
            return parts.length == 2
                    && callbackUsername.equals(parts[0])
                    && callbackPassword.equals(parts[1]);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
