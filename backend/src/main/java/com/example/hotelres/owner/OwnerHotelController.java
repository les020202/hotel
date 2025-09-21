package com.example.hotelres.owner;

import com.example.hotelres.owner.dto.OwnerHotelView;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/owner/hotels")
@RequiredArgsConstructor
public class OwnerHotelController {

    private final HotelOwnerRepository repo;

    /** 내 호텔 목록 */
    @GetMapping
    public List<OwnerHotelView> myHotels(@AuthenticationPrincipal Object principal) {
        String loginId = resolveLoginId(principal);
        return repo.findHotelsByOwnerLogin(loginId);
    }

    /**
     * 다양한 Principal 타입에서 loginId를 뽑아낸다.
     * - UserDetails: username 사용
     * - Map claims(커스텀 JWT 필터가 Map을 principal로 넣은 경우): "loginId" 우선, 없으면 "sub"
     * - 그 외: Authentication.getName()
     */
    private String resolveLoginId(Object principal) {
        if (principal instanceof UserDetails u) {
            return u.getUsername();
        }
        if (principal instanceof Map<?, ?> m) {
            Object v = m.get("loginId");
            if (v instanceof String s && !s.isBlank()) return s;
            Object sub = m.get("sub");
            if (sub instanceof String s2 && !s2.isBlank()) return s2;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null ? auth.getName() : null);
    }
}
