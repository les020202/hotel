// src/main/java/com/example/hotelres/owner/BookingController.java
package com.example.hotelres.owner;

import com.example.hotelres.user.UserRepository;
import com.example.hotelres.user.User.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 예약 취소 컨트롤러 (A안)
 * - 성공 시 본문 없이 204 No Content 반환
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final UserRepository userRepository;

    /* 요청 바디 */
    public record CancelRequest(String reason) {}

    @PostMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.NO_CONTENT) // ✅ 204 고정
    public void cancel(@PathVariable Long id,
                       @RequestBody(required = false) CancelRequest body,
                       @AuthenticationPrincipal(expression = "username") String loginId,
                       // 스프링 시큐리티 User (role 판별용)
                       @AuthenticationPrincipal org.springframework.security.core.userdetails.User springUser) {

        Long actorId = userRepository.findIdByLoginId(loginId)
                .orElseThrow(() -> new IllegalStateException("사용자 ID 조회 실패"));

        // 권한 → 우리 Role enum 매핑
        Role role =
                springUser.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority())) ? Role.ROLE_ADMIN :
                        springUser.getAuthorities().stream().anyMatch(a -> "ROLE_OWNER".equals(a.getAuthority())) ? Role.ROLE_OWNER :
                                Role.ROLE_USER;

        String reason = body == null ? null : body.reason();

        bookingService.cancelBooking(id, actorId, role, reason);
        // 본문 없음(204). 프론트는 후속 재조회로 상태/메타 갱신.
    }
}
