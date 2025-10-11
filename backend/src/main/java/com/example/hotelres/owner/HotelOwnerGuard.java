package com.example.hotelres.owner;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HotelOwnerGuard {

    private final HotelOwnerRepository repo;

    /** (loginId, hotelId) 매핑 없으면 403 */
    public void checkAccess(String loginId, Long hotelId) {
        boolean ok = repo.existsByUserLoginIdAndHotelId(loginId, hotelId);
        if (!ok) throw new AccessDeniedException("Not your hotel");
    }
}
