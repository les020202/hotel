// src/main/java/com/example/hotelres/common/GlobalExceptionHandler.java
package com.example.hotelres.common;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.hotelres.settlement.CutoffNotReachedException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice // 전체 REST 컨트롤러 전역 예외 처리
public class GlobalExceptionHandler {

    // ===== 1) 입력값 검증 실패 (Bean Validation) =====
    // 프론트에서 필드별 에러 메시지 맵을 그대로 쓰기 좋도록 field->message 형태로 반환
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> errors.put(e.getField(), e.getDefaultMessage()));
        return ResponseEntity.badRequest().body(errors); // 400
    }

    // ===== 2) 데이터 무결성 위반(중복 키 등) =====
    // 제약조건명으로 상세 메시지 분기
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDuplicate(DataIntegrityViolationException e) {
        String msg = e.getMostSpecificCause() != null ? e.getMostSpecificCause().getMessage() : "";
        String friendly;
        if (msg.contains("uk_users_login")) {
            friendly = "이미 사용 중인 아이디입니다.";
        } else if (msg.contains("uk_users_email")) {
            friendly = "이미 등록된 이메일입니다.";
        } else {
            friendly = "중복 데이터 오류";
        }
        return ResponseEntity.status(HttpStatus.CONFLICT) // 409
                .body(Map.of("error", friendly));
    }

    // ===== 3) 잘못된 요청 파라미터/상태 =====
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegal(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST) // 400
                .body(Map.of("error", e.getMessage()));
    }

    // ===== 4) 그 외 모든 예외(안전망) =====
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneric(Exception e) {
        e.printStackTrace(); // 로그 확인용
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR) // 500
                .body(Map.of("error", "서버 오류: " + e.getClass().getSimpleName()));
    }
   

        @ExceptionHandler(CutoffNotReachedException.class)
        @ResponseStatus(HttpStatus.CONFLICT)
        public Map<String, Object> handleCutoffException(CutoffNotReachedException ex) {
            Map<String, Object> body = new HashMap<>();
            body.put("message", ex.getMessage());  // ✅ 메시지만 내려주기
            return body;
        }
    }


