package com.example.hotelres.common;  // 공통 기능(헬퍼/유틸)

import com.example.hotelres.user.User;
import com.example.hotelres.user.UserRepository;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrentUser {

  private final UserRepository users;

  /** static 메서드(requireId)에서 Repo를 쓰기 위한 자기 참조 */
  private static volatile CurrentUser SELF;

  @PostConstruct
  void initSelf() { SELF = this; }

  /** 현재 인증된 사용자(Authentication)에서 User 엔티티 조회 (인스턴스용) */
  public User get(Authentication auth) {
    String loginId = auth.getName(); // username이 loginId인 환경
    return users.findByLoginId(loginId)
        .orElseThrow(() -> new RuntimeException("user not found"));
  }

  /**
   * 로그인한 사용자 ID 반환. 없으면 IllegalStateException("UNAUTHORIZED")
   * - principal(UserDetails/기타)의 username이 '숫자 id'면 그대로 사용
   * - 숫자가 아니면 'loginId'로 간주하고 DB 조회하여 id 반환
   */
  public static Long requireId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated()
        || "anonymousUser".equals(String.valueOf(auth.getPrincipal()))) {
      throw new IllegalStateException("UNAUTHORIZED");
    }

    Object principal = auth.getPrincipal();

    // 1) UserDetails 계열
    if (principal instanceof UserDetails ud) {
      String username = ud.getUsername(); // id 또는 loginId
      try {
        return Long.valueOf(username);     // 숫자 id인 경우
      } catch (NumberFormatException ignored) {
        ensureSelf();
        return SELF.users.findByLoginId(username)
            .map(User::getId)
            .orElseThrow(() -> new IllegalStateException("UNAUTHORIZED"));
      }
    }

    // 2) 그 외: Authentication#getName() 사용
    String name = auth.getName(); // id 또는 loginId
    try {
      return Long.valueOf(name);   // 숫자 id인 경우
    } catch (Exception ignored) {
      ensureSelf();
      return SELF.users.findByLoginId(name)
          .map(User::getId)
          .orElseThrow(() -> new IllegalStateException("UNAUTHORIZED"));
    }
  }

  private static void ensureSelf() {
    if (SELF == null) throw new IllegalStateException("UNAUTHORIZED");
  }
}
