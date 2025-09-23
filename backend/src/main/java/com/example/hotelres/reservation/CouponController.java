// path: backend/src/main/java/com/example/hotelres/reservation/CouponController.java
package com.example.hotelres.reservation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.hotelres.coupon.Coupon;
import com.example.hotelres.reservation.dto.AvailableCouponDto;
import com.example.hotelres.user.UserRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class CouponController {

    private final CouponIssuanceRepository couponIssuanceRepository;
    private final UserRepository userRepository;
    private final CouponRepository coupons; // 쿠폰 데이터를 조회/처리하는 리포지토리 의존성
    
    @GetMapping("/api/coupons") // HTTP GET 요청을 처리하는 메서드
    public List<Coupon> list(@RequestParam(name="all", defaultValue = "false") boolean all) {
        // 요청 파라미터 all을 boolean으로 받음 (기본값 false)
        // all=false → !all == true  → 오늘 사용 가능한 것만 조회
        // all=true  → !all == false → 만료 포함 전체 조회
        return coupons.findForList(!all ? true : false);
    }
    
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