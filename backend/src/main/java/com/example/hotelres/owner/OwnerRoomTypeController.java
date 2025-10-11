package com.example.hotelres.owner;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/owner/hotels/{hotelId}/room-types")
@RequiredArgsConstructor
public class OwnerRoomTypeController {

    private final HotelOwnerGuard guard;
    private final RoomTypeRepository repo;

    @GetMapping
    public List<RoomTypeLite> list(@AuthenticationPrincipal Object principal,
                                   @PathVariable Long hotelId) {
        String loginId = resolveLoginId(principal);
        guard.checkAccess(loginId, hotelId);
        return repo.findLiteDtos(hotelId);
    }

    private String resolveLoginId(Object principal) {
        if (principal instanceof UserDetails u) return u.getUsername();
        if (principal instanceof Map<?,?> m) {
            Object v = m.get("loginId"); if (v instanceof String s && !s.isBlank()) return s;
            Object sub = m.get("sub");   if (sub instanceof String s2 && !s2.isBlank()) return s2;
        }
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return (a!=null ? a.getName() : null);
    }
}
