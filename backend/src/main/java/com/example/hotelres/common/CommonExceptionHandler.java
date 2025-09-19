package com.example.hotelres.common;  
// 공통 기능 패키지

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
// 예외 처리용 스프링 어노테이션 및 클래스들

import java.util.HashMap;
import java.util.Map;
// 에러 메시지를 담을 Map

@RestControllerAdvice
// 모든 REST 컨트롤러 전역에서 발생하는 예외를 처리하는 어노테이션
public class CommonExceptionHandler  {

  // ===== 입력값 검증 실패 예외 처리 =====
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
    Map<String, String> errors = new HashMap<>(); // 에러 메시지 담을 Map

    ex.getBindingResult().getFieldErrors()
      .forEach(e -> errors.put(e.getField(), e.getDefaultMessage()));
      // 유효성 검증 실패한 필드명과 메시지를 Map에 저장

    return ResponseEntity.badRequest().body(errors);
    // HTTP 400 응답과 함께 에러 메시지 반환
  }
}
