// path: backend/src/main/java/com/example/hotelres/coupon/CouponController.java
package com.example.hotelres.reservation;

import com.example.hotelres.admin.coupon.Coupon;
import com.example.hotelres.admin.coupon.CouponRepository;
import com.example.hotelres.reservation.CouponIssuanceRepository;
import com.example.hotelres.reservation.dto.AvailableCouponDto;
import com.example.hotelres.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * CouponController
 * - 쿠폰 API를 처리하는 REST 컨트롤러
 * - 기본 prefix: /api/coupons
 */
@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponRepository coupons;                 // 쿠폰 엔티티 조회
    private final CouponIssuanceRepository couponIssuances; // 발급 쿠폰 조회
    private final UserRepository userRepository;            // 사용자 조회

    /**
     * 전체/사용가능 쿠폰 목록 조회
     * GET /api/coupons
     *   - ?all=false (default): 오늘 사용 가능한 쿠폰만
     *   - ?all=true           : 만료 포함 전체 조회
     */
    @GetMapping
    public List<Coupon> list(@RequestParam(name = "all", defaultValue = "false") boolean all) {
        return coupons.findForList(!all);
    }

    /**
     * 현재 로그인한 사용자가 발급받은 쿠폰 중 사용 가능한 목록 조회
     * GET /api/coupons/available
     */
    @GetMapping("/available")
    public List<AvailableCouponDto> getAvailable() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        // principal에서 loginId 추출
        Object principal = auth.getPrincipal();
        String loginId = (principal instanceof org.springframework.security.core.userdetails.UserDetails ud)
                ? ud.getUsername()
                : auth.getName();

        var user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        Long userId = user.getId();
        var now = LocalDateTime.now();
        var today = LocalDate.now();

        return couponIssuances.findAvailableForUser(userId, today, now).stream()
                .map(ci -> {
                    LocalDateTime expiresAt = ci.getExpiresAt();
                    if (expiresAt == null && ci.getCoupon().getValidTo() != null) {
                        expiresAt = ci.getCoupon().getValidTo().atTime(23, 59, 59);
                    }
                    return new AvailableCouponDto(
                            ci.getCoupon().getCode(),
                            ci.getCoupon().getTitle(),
                            ci.getCoupon().getAmount(),
                            expiresAt
                    );
                })
                .toList();
    }
}
