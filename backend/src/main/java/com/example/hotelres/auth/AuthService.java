package com.example.hotelres.auth;

import com.example.hotelres.auth.dto.SignupRequest;
import com.example.hotelres.user.User;
import com.example.hotelres.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.hotelres.user.coupon.UserCouponService;

@Service @RequiredArgsConstructor
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final UserCouponService userCouponService;

    @Transactional
    public User signup(SignupRequest req) {
        if (users.existsByLoginId(req.getLoginId())) throw new IllegalArgumentException("DUP_LOGIN_ID");
        if (users.existsByEmail(req.getEmail()))     throw new IllegalArgumentException("DUP_EMAIL");

        User u = new User();
        u.setLoginId(req.getLoginId());
        u.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        u.setName(req.getName());
        u.setEmail(req.getEmail());
        u.setPhone(req.getPhone());
        u.setGender(req.getGender());
        u.setBirthDate(req.getBirthDate());

        // ✅ 먼저 저장해서 PK 확보
        u = users.save(u);
        users.flush();                 // PK 즉시 확정 (안전)

        // ✅ 확보한 PK로 발급 (SecurityContext 의존 X)
        userCouponService.grantWelcomeCouponIfNeeded(u.getId());

        return u;
    }

     // 이메일 기반 비밀번호 재설정
    public boolean resetPassword(String email, String newPassword) {
        return users.findByEmail(email).map(user -> {
            user.setPasswordHash(passwordEncoder.encode(newPassword));
            users.save(user);
            return true;
        }).orElse(false);
    }

}
