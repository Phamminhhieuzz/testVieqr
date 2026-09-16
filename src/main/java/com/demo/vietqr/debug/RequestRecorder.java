package com.demo.vietqr.debug;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;

/**
 * Lưu tạm N request gần nhất trong bộ nhớ để chẩn đoán callback từ VietQR.
 */
@Component
public class RequestRecorder {

    private static final int MAX_ENTRIES = 50;

    private final Deque<Map<String, Object>> entries = new ArrayDeque<>();

    public synchronized void record(String method, String uri, String query,
                                    Map<String, String> headers, String body, int status) {
        if (entries.size() >= MAX_ENTRIES) {
            entries.removeFirst();
        }
        entries.addLast(Map.of(
                "time", Instant.now().toString(),
                "method", method,
                "uri", uri,
                "query", query == null ? "" : query,
                "headers", headers,
                "body", body == null ? "" : body,
                "status", status
        ));
    }

    public synchronized List<Map<String, Object>> getAll() {
        return new ArrayList<>(entries);
    }

    public synchronized void clear() {
        entries.clear();
    }
}
