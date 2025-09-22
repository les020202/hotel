// path: backend/src/main/java/com/example/hotelres/reservation/CouponController.java
package com.example.hotelres.reservation;

import com.example.hotelres.reservation.dto.AvailableCouponDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class CouponController {

    private final CouponIssuanceRepository couponIssuanceRepository;

    @GetMapping("/api/coupons/available")
    public List<AvailableCouponDto> getAvailable(@RequestParam Long userId) {
        var now   = LocalDateTime.now();
        var today = LocalDate.now();

        return couponIssuanceRepository.findAvailableForUser(userId, today, now).stream()
            .map(ci -> new AvailableCouponDto(
                    ci.getCoupon().getCode(),
                    ci.getCoupon().getTitle(),
                    ci.getCoupon().getAmount(),
                    // 발급 만료 없으면 쿠폰 valid_to(자정)를 표시용으로 사용
                    ci.getExpiresAt() != null
                        ? ci.getExpiresAt()
                        : (ci.getCoupon().getValidTo() != null
                            ? ci.getCoupon().getValidTo().atTime(23,59,59)
                            : null)
            ))
            .toList();
    }
}
