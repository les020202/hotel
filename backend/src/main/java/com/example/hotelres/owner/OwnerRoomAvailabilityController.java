// src/main/java/com/example/hotelres/owner/OwnerRoomAvailabilityController.java
package com.example.hotelres.owner;

import com.example.hotelres.owner.dto.AvailableRoomDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.time.LocalDate;
import java.util.List;

@RestController @RequestMapping("/api/owner/hotels/{hotelId}/rooms/available")
@RequiredArgsConstructor
public class OwnerRoomAvailabilityController {
    private final OwnerAssignmentService service;

    @GetMapping
    public List<AvailableRoomDto> available(@AuthenticationPrincipal Object p,
                                            @PathVariable Long hotelId,
                                            @RequestParam LocalDate from,
                                            @RequestParam LocalDate to,
                                            @RequestParam int minCapacity,
                                            @RequestParam String typeCode,
                                            @RequestParam(defaultValue="true") boolean upgrade) {
        String loginId = OwnerSecurityUtil.resolveLoginId(p);
        return service.findAvailableRooms(loginId, hotelId, from, to, minCapacity, typeCode, upgrade);
    }
}
