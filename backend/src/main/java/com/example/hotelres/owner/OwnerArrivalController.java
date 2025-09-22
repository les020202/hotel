// src/main/java/com/example/hotelres/owner/OwnerArrivalController.java
package com.example.hotelres.owner;

import com.example.hotelres.owner.dto.ArrivalItemDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController @RequestMapping("/api/owner/hotels/{hotelId}/arrivals")
@RequiredArgsConstructor
public class OwnerArrivalController {
    private final HotelOwnerGuard guard;
    private final OwnerArrivalRepository repo;

    @GetMapping
    public List<ArrivalItemDto> arrivals(@AuthenticationPrincipal Object p,
                                         @PathVariable Long hotelId,
                                         @RequestParam LocalDate date) {
        String loginId = OwnerSecurityUtil.resolveLoginId(p);
        guard.checkAccess(loginId, hotelId);
        return repo.findArrivals(hotelId, date);
    }
}
