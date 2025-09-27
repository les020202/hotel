// src/main/java/com/example/hotelres/owner/BookingController.java
package com.example.hotelres.owner;

import com.example.hotelres.user.UserRepository;
import com.example.hotelres.user.User.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    @ResponseStatus(HttpStatus.NO_CONTENT) // 204
    public void cancel(@PathVariable Long id,
                       @RequestBody(required = false) CancelRequest body,
                       @AuthenticationPrincipal(expression = "username") String loginId,
                       Authentication authentication) {

        Long actorId = userRepository.findIdByLoginId(loginId)
                .orElseThrow(() -> new IllegalStateException("사용자 ID 조회 실패"));

        // 권한 매핑 (ADMIN > OWNER > USER 우선순위)
        Role role = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority())) ? Role.ROLE_ADMIN :
                authentication.getAuthorities().stream()
                        .anyMatch(a -> "ROLE_OWNER".equals(a.getAuthority())) ? Role.ROLE_OWNER :
                        Role.ROLE_USER;

        String reason = body == null ? null : body.reason();

        bookingService.cancelBooking(id, actorId, role, reason);
        // 204 No Content 반환
    }
}
