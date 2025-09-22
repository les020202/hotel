// path: backend/src/main/java/com/example/hotelres/reservation/CouponController.java
package com.example.hotelres.reservation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.hotelres.reservation.dto.AvailableCouponDto;
import com.example.hotelres.user.UserRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class CouponController {

    private final CouponIssuanceRepository couponIssuanceRepository;
    private final UserRepository userRepository;
    @GetMapping("/api/coupons/available")
    public List<AvailableCouponDto> getAvailable() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        // principal이 UserDetails라면 캐스팅
        Object principal = auth.getPrincipal();
        String loginId;

        if (principal instanceof org.springframework.security.core.userdetails.UserDetails userDetails) {
            loginId = userDetails.getUsername(); // = loginId
        } else {
            loginId = auth.getName(); // fallback
        }

        var u = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        Long userId = u.getId();

        var now   = LocalDateTime.now();
        var today = LocalDate.now();

        return couponIssuanceRepository.findAvailableForUser(userId, today, now).stream()
            .map(ci -> new AvailableCouponDto(
                ci.getCoupon().getCode(),
                ci.getCoupon().getTitle(),
                ci.getCoupon().getAmount(),
                ci.getExpiresAt() != null
                    ? ci.getExpiresAt()
                    : (ci.getCoupon().getValidTo() != null
                        ? ci.getCoupon().getValidTo().atTime(23,59,59)
                        : null)
            ))
            .toList();
    }
}