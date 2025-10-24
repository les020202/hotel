package com.example.hotelres.exception;

import com.example.hotelres.settlement.CutoffNotReachedException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

/**
 * 에러 일원화 동작 확인을 위한 테스트 전용 컨트롤러
 * - 실제 기능과 무관. 필요 시 @Profile("dev")로 한정해서 사용해도 됨.
 */
// @Profile("dev")
@RestController
@RequestMapping("/api/test")
public class TestErrorController {

    /** 400 Bad Request */
    @GetMapping("/bad-request")
    public void badRequest() {
        throw new IllegalArgumentException("테스트 400 Bad Request");
    }

    /** 401 Unauthorized */
    @GetMapping("/unauthorized")
    public void unauthorized() {
        // GlobalExceptionHandler에서 AuthenticationException → 401로 표준화
        throw new BadCredentialsException("테스트 401 Unauthorized");
    }

    /** 403 Forbidden */
    @GetMapping("/forbidden")
    public void forbidden() {
        throw new AccessDeniedException("테스트 403 Forbidden");
    }

    /** 404 Not Found */
    @GetMapping("/not-found")
    public void notFound() throws NoHandlerFoundException {
        // spring.mvc.throw-exception-if-no-handler-found=true 설정 필요
        throw new NoHandlerFoundException(HttpMethod.GET.name(), "/api/test/not-found", new HttpHeaders());
    }

    /** 405 Method Not Allowed */
    // 이 엔드포인트는 GET만 허용. 콘솔에서 POST로 호출하면 405가 발생.
    @GetMapping("/method-not-allowed")
    public String onlyGet() { return "ok"; }

    /** 409 Conflict (도메인 예시: 정산 컷오프 미도달) */
    @GetMapping("/conflict")
    public void conflict() {
        throw new CutoffNotReachedException("테스트 409 Conflict: 컷오프 미도달");
    }

    /** 413 Payload Too Large */
    @PostMapping("/upload-too-large")
    public void tooLarge() {
        throw new MaxUploadSizeExceededException(5 * 1024 * 1024L);
    }

    /** 415 Unsupported Media Type */
    @PostMapping("/unsupported-type")
    public void unsupported() throws HttpMediaTypeNotSupportedException {
        throw new HttpMediaTypeNotSupportedException("application/xml");
    }

    /** 429 Too Many Requests */
    @GetMapping("/too-many-requests")
    public void tooMany() {
        throw new RateLimitExceededException("테스트 429 Too Many Requests");
    }

    /** 500 Internal Server Error */
    @GetMapping("/internal-error")
    public void internalError() {
        throw new RuntimeException("테스트 500 Internal Server Error");
    }

    /** 502 Bad Gateway */
    @GetMapping("/bad-gateway")
    public void badGateway() {
        throw new DownstreamBadGatewayException("테스트 502 Bad Gateway");
    }

    /** 503 Service Unavailable */
    @GetMapping("/service-unavailable")
    public void serviceUnavailable() {
        throw new ServiceUnavailableException("테스트 503 Service Unavailable");
    }

    /** 504 Gateway Timeout */
    @GetMapping("/gateway-timeout")
    public void gatewayTimeout() {
        throw new GatewayTimeoutException("테스트 504 Gateway Timeout");
    }
}
