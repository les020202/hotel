// src/main/java/com/example/hotelres/owner/BookingController.java
package com.example.hotelres.owner;

import com.example.hotelres.user.UserRepository;
import com.example.hotelres.user.User.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final UserRepository userRepository;

    public record CancelRequest(String reason) {}

    @PostMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public void cancel(@PathVariable Long id,
                       @RequestBody(required = false) CancelRequest body,
                       @AuthenticationPrincipal(expression = "username") String loginId,
                       @AuthenticationPrincipal User springUser // role 확인용
    ) {
        Long actorId = userRepository.findIdByLoginId(loginId)
                .orElseThrow(() -> new IllegalStateException("사용자 ID 조회 실패"));

        // 스프링 시큐리티 권한을 Role enum으로 매핑 (예: "ROLE_ADMIN")
        Role role =
                springUser.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority())) ? Role.ROLE_ADMIN :
                        springUser.getAuthorities().stream().anyMatch(a -> "ROLE_OWNER".equals(a.getAuthority())) ? Role.ROLE_OWNER :
                                Role.ROLE_USER;

        String reason = body == null ? null : body.reason();
        bookingService.cancelBooking(id, actorId, role, reason);
    }
}
