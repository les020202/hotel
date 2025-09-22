package com.example.hotelres.owner;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OwnerGuard {

    private final HotelOwnerRepository hotelOwnerRepository;

    /** 현재 로그인한 사용자의 loginId 추출 (의존성 없이 범용) */
    public String currentLoginId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) throw new IllegalStateException("Unauthenticated");

        // 1) UserDetails 기반 (폼/세션 로그인 등)
        Object principal = auth.getPrincipal();
        if (principal instanceof UserDetails ud) {
            String u = ud.getUsername();
            if (u != null && !u.isBlank()) return u;
        }

        // 2) JWT를 Map 형태로 넣어둔 경우(커스텀 필터 등)
        if (principal instanceof Map<?,?> map) {
            String s = pickFirstNonBlank(map,
                    "sub", "login_id", "username", "preferred_username");
            if (s != null) return s;
        }

        // 3) 최후: Authentication 이름
        String name = auth.getName();
        if (name != null && !name.isBlank()) return name;

        // 4) principal문자열
        return String.valueOf(principal);
    }

    private static String pickFirstNonBlank(Map<?,?> map, String... keys) {
        for (String k : keys) {
            Object v = map.get(k);
            if (v != null) {
                String s = String.valueOf(v).trim();
                if (!s.isBlank()) return s;
            }
        }
        return null;
    }

    /** 내가 소유한 호텔 ID 목록 */
    public List<Long> myHotelIds() {
        String loginId = currentLoginId();
        return hotelOwnerRepository.findHotelIdsByOwnerLoginId(loginId);
    }

    /** 특정 호텔 소유 여부 */
    public boolean isOwnerOfHotel(Long hotelId) {
        String loginId = currentLoginId();
        return hotelOwnerRepository.existsByUserLoginIdAndHotelId(loginId, hotelId);
    }

    /** 특정 호텔 소유 여부 강제 검사(아니면 403) */
    public void assertOwnerOfHotel(Long hotelId) {
        if (!isOwnerOfHotel(hotelId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You do not own hotel " + hotelId
            );
        }
    }

    /** 여러 호텔 모두 소유 여부 강제 검사 */
    public void assertAllOwned(Iterable<Long> hotelIds) {
        String loginId = currentLoginId();
        for (Long hid : hotelIds) {
            if (!hotelOwnerRepository.existsByUserLoginIdAndHotelId(loginId, hid)) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "You do not own hotel " + hid
                );
            }
        }
    }
}
