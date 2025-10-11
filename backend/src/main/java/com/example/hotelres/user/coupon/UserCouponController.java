package com.example.hotelres.user.coupon;

import com.example.hotelres.common.CurrentUser;
import com.example.hotelres.user.coupon.dto.UserCouponDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user/coupon")
@RequiredArgsConstructor
public class UserCouponController {

    private final UserCouponService userCouponService;

    @GetMapping
    public List<UserCouponDto> list(@RequestParam(defaultValue = "true") boolean all) {
        Long userId = CurrentUser.requireId(); // 미인증이면 401로 처리
        return userCouponService.getMyCoupons(userId, all);
    }
}
