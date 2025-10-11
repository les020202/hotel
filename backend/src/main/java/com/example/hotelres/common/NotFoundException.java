package com.example.hotelres.common;  
// 공통 예외 정의 패키지

public class NotFoundException extends RuntimeException {
  // RuntimeException(실행 시 예외)을 상속한 사용자 정의 예외 클래스

  public NotFoundException(String msg) { 
    super(msg); 
    // 부모 생성자에 메시지를 넘겨 예외 객체 생성
  }
}
