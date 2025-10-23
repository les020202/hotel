package com.example.hotelres.common;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import io.github.bucket4j.*;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Order(10) // 프로젝트 상황에 맞게 조정 (보통 SecurityFilterChain 이후 동작이면 OK)
public class RateLimitFilter implements Filter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    // 정책: 1분 32회, 초기 버스트 30회 (원하면 바꿔 사용)
    private Bucket newBucket() {
        Bandwidth limit = Bandwidth.classic(32, Refill.greedy(60, Duration.ofMinutes(1)))
                .withInitialTokens(30);
        return Bucket.builder().addLimit(limit).build();
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request  = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String uri = request.getRequestURI();

        // 정적 파일/헬스 제외 (프로젝트 정적 경로에 맞춰 보완)
        if (uri.startsWith("/assets") || uri.startsWith("/favicon")
                || uri.startsWith("/health") || uri.startsWith("/static")
                || uri.matches("^/.*\\.(js|css|png|jpg|jpeg|gif|svg|ico|woff2?)$")) {
            chain.doFilter(req, res);
            return;
        }

        // 키: 로그인 사용자는 userId, 아니면 IP
        String key = buildKey(request);

        Bucket bucket = buckets.computeIfAbsent(key, k -> newBucket());
        if (bucket.tryConsume(1)) {
            chain.doFilter(req, res);
        } else {
            write429(response);
        }
    }

    private String buildKey(HttpServletRequest request) {
        // (선택) JWT 필터에서 userId를 attribute로 심어둔 경우 사용
        Object uidAttr = request.getAttribute("userId");
        if (uidAttr != null) return "U:" + uidAttr;

        // (선택) SecurityContext에서 principal 사용 가능하다면 꺼내서 사용해도 됨
        // String principal = request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : null;
        // if (principal != null) return "U:" + principal;

        String ip = getClientIp(request);
        return "IP:" + ip;
    }

    private String getClientIp(HttpServletRequest req) {
        String f = req.getHeader("X-Forwarded-For");
        if (f != null && !f.isBlank()) return f.split(",")[0].trim();
        String r = req.getHeader("X-Real-IP");
        return (r != null && !r.isBlank()) ? r : req.getRemoteAddr();
    }

    private void write429(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setHeader("Retry-After", "30"); // 힌트(초)
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("""
        {"code":"RATE_LIMIT","message":"요청이 너무 많습니다. 잠시 후에 다시 시도해주세요."}
        """);
    }
}
