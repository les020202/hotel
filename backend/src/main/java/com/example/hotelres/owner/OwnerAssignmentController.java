// src/main/java/com/example/hotelres/owner/OwnerAssignmentController.java
package com.example.hotelres.owner;

import com.example.hotelres.owner.dto.AssignRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.Map;

@RestController @RequestMapping("/api/owner/hotels/{hotelId}/assignments")
@RequiredArgsConstructor
public class OwnerAssignmentController {
    private final OwnerAssignmentService service;

    @PostMapping
    public Map<String,Object> assign(@AuthenticationPrincipal Object p,
                                     @PathVariable Long hotelId,
                                     @RequestBody AssignRequest req) {
        String loginId = OwnerSecurityUtil.resolveLoginId(p);
        int n = service.assign(loginId, hotelId, req.bookingItemId(), req.roomId());
        return Map.of("assignedNights", n);
    }

    @DeleteMapping("/by-item/{bookingItemId}")
    public Map<String,Object> unassign(@AuthenticationPrincipal Object p,
                                       @PathVariable Long hotelId,
                                       @PathVariable Long bookingItemId) {
        String loginId = OwnerSecurityUtil.resolveLoginId(p);
        int n = service.unassignAll(loginId, hotelId, bookingItemId);
        return Map.of("releasedNights", n);
    }
}
