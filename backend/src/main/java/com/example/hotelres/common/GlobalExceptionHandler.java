// src/main/java/com/example/hotelres/common/GlobalExceptionHandler.java
package com.example.hotelres.common;

import com.example.hotelres.settlement.CutoffNotReachedException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;

import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 전역 REST 예외 처리기 (기존 클래스 확장 버전)
 * - 표준 에러 바디 + X-Error-Id 헤더 제공
 * - 기존 동작(검증/무결성/IllegalArgument/Generic) 유지 + 주요 HTTP 에러 추가
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /* ----------------------- 공통 유틸 ----------------------- */

    private HttpHeaders headers(String errorId) {
        HttpHeaders h = new HttpHeaders();
        h.setCacheControl(CacheControl.noStore());
        if (errorId != null && !errorId.isBlank()) h.add("X-Error-Id", errorId);
        return h;
    }

    private ResponseEntity<Object> build(int status, String code, String message, Throwable ex) {
        String errorId = UUID.randomUUID().toString();
        // 내부 로그에는 상세 정보 남김 (사용자 응답엔 노출하지 않음)
        if (ex != null) {
            log.error("[{}] {} - {}", errorId, code, ex.toString(), ex);
        } else {
            log.error("[{}] {} - (no exception detail)", errorId, code);
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status);
        body.put("code", code);
        body.put("message", message);
        body.put("errorId", errorId);
        return new ResponseEntity<>(body, headers(errorId), HttpStatus.valueOf(status));
    }

    /* ----------------------- 400 계열 ----------------------- */
    
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Object> handleStaticNotFound(NoResourceFoundException ex) {
        return build(404, "NOT_FOUND", "요청하신 리소스를 찾을 수 없습니다.", ex);
    }

    // (A) Bean Validation - 필드별 에러 맵 반환 (기존 동작 유지 + errorId/헤더 추가)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidation(MethodArgumentNotValidException ex) {
        String errorId = UUID.randomUUID().toString();
        // 필드 에러 맵
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        e -> e.getField(),
                        e -> e.getDefaultMessage(),
                        (a, b) -> a, // 중복 키는 첫 값 유지
                        LinkedHashMap::new
                ));
        // 내부 로그
        log.warn("[{}] INVALID_REQUEST - BeanValidation {}", errorId, fieldErrors, ex);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", 400);
        body.put("code", "INVALID_REQUEST");
        body.put("message", "요청 형식이 잘못되었습니다.");
        body.put("errorId", errorId);
        body.put("errors", fieldErrors); // 기존 UX를 위해 필드맵 유지

        return new ResponseEntity<>(body, headers(errorId), HttpStatus.BAD_REQUEST);
    }

    // (B) 잘못된 요청 바디/파라미터/타입
    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            ConstraintViolationException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<Object> handleBadRequest(Exception ex) {
        return build(400, "INVALID_REQUEST", "요청 형식이 잘못되었습니다.", ex);
    }

    // (C) 클라이언트 로직상 잘못된 상태/파라미터
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleIllegal(IllegalArgumentException ex) {
        return build(400, "INVALID_ARGUMENT", ex.getMessage() != null ? ex.getMessage() : "잘못된 요청입니다.", ex);
    }

    /* ----------------------- 401/403 ----------------------- */

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Object> handleUnauthorized(AuthenticationException ex) {
        return build(401, "UNAUTHORIZED", "로그인이 필요합니다.", ex);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> handleForbidden(AccessDeniedException ex) {
        return build(403, "FORBIDDEN", "접근 권한이 없습니다.", ex);
    }

    /* ----------------------- 404/405 ----------------------- */

    // application.yml 에서 spring.mvc.throw-exception-if-no-handler-found=true 필요
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Object> handleNotFound(NoHandlerFoundException ex) {
        return build(404, "NOT_FOUND", "요청하신 리소스를 찾을 수 없습니다.", ex);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Object> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        return build(405, "METHOD_NOT_ALLOWED", "허용되지 않은 요청입니다.", ex);
    }

    /* ----------------------- 429 & 게이트웨이 계열 ----------------------- */

    // 429: 필요 시 이 예외를 던져 사용 (내부/컨트롤러에서)
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Object> handleTooManyRequests(RateLimitExceededException ex) {
        return build(429, "TOO_MANY_REQUESTS", ex.getMessage() != null ? ex.getMessage() : "요청이 너무 많습니다. 잠시 후 다시 시도해주세요.", ex);
    }

    // ResponseStatusException 으로 들어오는 4xx/5xx 중 일부를 보정
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Object> handleResponseStatus(ResponseStatusException ex) {
        int status = ex.getStatusCode().value();
        String code;
        String msg = Optional.ofNullable(ex.getReason()).orElse("요청 처리 중 오류가 발생했습니다.");
        if (status == 429) code = "TOO_MANY_REQUESTS";
        else if (status == 502) code = "BAD_GATEWAY";
        else if (status == 503) code = "SERVICE_UNAVAILABLE";
        else if (status == 504) code = "GATEWAY_TIMEOUT";
        else if (status >= 400 && status < 500) code = "CLIENT_ERROR";
        else code = "SERVER_ERROR";
        return build(status, code, msg, ex);
    }

    @ExceptionHandler(DownstreamBadGatewayException.class)
    public ResponseEntity<Object> handleBadGateway(DownstreamBadGatewayException ex) {
        return build(502, "BAD_GATEWAY", ex.getMessage() != null ? ex.getMessage() : "외부 서비스 응답에 오류가 있습니다.", ex);
    }


    @ExceptionHandler(GatewayTimeoutException.class)
    public ResponseEntity<Object> handleGatewayTimeout(GatewayTimeoutException ex) {
        return build(504, "GATEWAY_TIMEOUT", ex.getMessage() != null ? ex.getMessage() : "외부 서비스 응답이 지연되고 있습니다.", ex);
    }

    /* ----------------------- 도메인 예외(기존 + 보강) ----------------------- */

    // 정산 컷오프 미도달: 409 유지 (기존 로직 보강: 표준 바디 + X-Error-Id)
    @ExceptionHandler(CutoffNotReachedException.class)
    public ResponseEntity<Object> handleCutoffException(CutoffNotReachedException ex) {
        String msg = Optional.ofNullable(ex.getMessage()).orElse("정산 가능 시점이 아닙니다.");
        return build(409, "CONFLICT_CUTOFF_NOT_REACHED", msg, ex);
    }

    // 데이터 무결성(중복 키 등) – 기존 동작 유지하되 메시지 보강
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleDuplicate(DataIntegrityViolationException e) {
        String msg = e.getMostSpecificCause() != null ? e.getMostSpecificCause().getMessage() : "";
        String friendly;
        if (msg.contains("uk_users_login")) {
            friendly = "이미 사용 중인 아이디입니다.";
        } else if (msg.contains("uk_users_email")) {
            friendly = "이미 등록된 이메일입니다.";
        } else {
            friendly = "중복 데이터 오류";
        }
        return build(409, "DATA_INTEGRITY_VIOLATION", friendly, e);
    }

    /* ----------------------- 안전망 ----------------------- */

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGeneric(Exception e) {
        // 최종 안전망: 500
        return build(500, "INTERNAL_SERVER_ERROR", "서버 오류가 발생했습니다.", e);
    }

    /* ----------------------- 커스텀 예외 (내부 정적 클래스로 제공) ----------------------- */

    // 필요 시 컨트롤러/서비스에서 아래 예외를 던져 429/502/503/504 매핑 사용
    public static class RateLimitExceededException extends RuntimeException {
        public RateLimitExceededException() { super(); }
        public RateLimitExceededException(String msg) { super(msg); }
    }
    public static class DownstreamBadGatewayException extends RuntimeException {
        public DownstreamBadGatewayException() { super(); }
        public DownstreamBadGatewayException(String msg) { super(msg); }
    }
    public static class ServiceUnavailableException extends RuntimeException {
        public ServiceUnavailableException() { super(); }
        public ServiceUnavailableException(String msg) { super(msg); }
    }
    public static class GatewayTimeoutException extends RuntimeException {
        public GatewayTimeoutException() { super(); }
        public GatewayTimeoutException(String msg) { super(msg); }
    }
}
