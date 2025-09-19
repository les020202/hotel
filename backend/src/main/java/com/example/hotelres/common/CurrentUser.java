package com.example.hotelres.common;  
// 공통 기능(헬퍼/유틸)들이 모여 있는 패키지

import com.example.hotelres.user.User;
import com.example.hotelres.user.UserRepository;
// User 엔티티와 DB 접근용 JPA Repository

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
// Spring Security Authentication, 컴포넌트 스캔용 어노테이션

@Component
@RequiredArgsConstructor
// 스프링 빈으로 등록되는 컴포넌트
// final 필드를 위한 생성자 자동 생성
public class CurrentUser {
  private final UserRepository users; // User 엔티티 접근용 JPA Repo

  // 현재 인증된 사용자(Authentication) 객체에서 User 엔티티 조회
  public User get(Authentication auth) {
    String loginId = auth.getName(); // SecurityContext에서 로그인 아이디 가져오기
    return users.findByLoginId(loginId)
        .orElseThrow(() -> new RuntimeException("user not found"));
        // loginId로 DB에서 User 조회, 없으면 예외 발생
  }
}
