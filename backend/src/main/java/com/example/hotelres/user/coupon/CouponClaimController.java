package com.example.hotelres.user.coupon;

import com.example.hotelres.common.CurrentUser;
import com.example.hotelres.user.coupon.dto.ClaimCouponRequest;
import com.example.hotelres.user.coupon.dto.ClaimCouponResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponClaimController {

    private final CouponClaimService couponClaimService;

    @PostMapping("/claim")
    public ClaimCouponResponse claim(@Valid @RequestBody ClaimCouponRequest req) {
        Long userId = CurrentUser.requireId(); // 미인증이면 401
        return couponClaimService.claim(userId, req.code());
    }
}
