package com.demo.vietqr.debug;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class RequestLoggingFilter extends OncePerRequestFilter {

    private final RequestRecorder recorder;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        ContentCachingRequestWrapper wrapped = new ContentCachingRequestWrapper(request);

        try {
            chain.doFilter(wrapped, response);
        } finally {
            Map<String, String> headers = new LinkedHashMap<>();
            for (String name : Collections.list(wrapped.getHeaderNames())) {
                headers.put(name, wrapped.getHeader(name));
            }

            String body = new String(wrapped.getContentAsByteArray(), StandardCharsets.UTF_8);

            log.info(">>> REQUEST {} {}{} | status={} | body={}",
                    wrapped.getMethod(),
                    wrapped.getRequestURI(),
                    wrapped.getQueryString() == null ? "" : "?" + wrapped.getQueryString(),
                    response.getStatus(),
                    body);

            recorder.record(wrapped.getMethod(), wrapped.getRequestURI(),
                    wrapped.getQueryString(), headers, body, response.getStatus());
        }
    }
}
