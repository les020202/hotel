// path: backend/src/main/java/com/example/hotelres/user/coupon/UserCouponDebugController.java
package com.example.hotelres.user.coupon;

import com.example.hotelres.common.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/debug/coupon")
@RequiredArgsConstructor
public class UserCouponDebugController {

    private final UserCouponService svc;

    @PostMapping("/welcome")
    public Map<String,Object> forceWelcome() {
        Long userId = CurrentUser.requireId();
        boolean ok = svc.grantWelcomeCouponIfNeeded(userId);
        return Map.of("granted", ok);
    }
}
