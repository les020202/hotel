// src/main/java/com/example/hotelres/owner/booking/OwnerBookingsController.java
package com.example.hotelres.owner.booking;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/owner/hotels/{hotelId}/bookings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
public class OwnerBookingsController {

    private final OwnerBookingQueryRepository repo;

    @GetMapping
    public List<OwnerBookingSummary> list(@PathVariable Long hotelId,
                                          @AuthenticationPrincipal UserDetails me) {
        String ownerLoginId = me.getUsername(); // 토큰에서 로그인ID
        return repo.listForOwner(hotelId, ownerLoginId);
    }
}
