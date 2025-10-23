package com.example.hotelres.exception;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
public class RequestIdFilter implements Filter {

    private static final String KEY = "requestId";

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        String id = UUID.randomUUID().toString();
        MDC.put(KEY, id);
        try {
            if (res instanceof HttpServletResponse r) {
                r.setHeader("X-Request-Id", id);
            }
            chain.doFilter(req, res);
        } finally {
            MDC.remove(KEY);
        }
    }
}
