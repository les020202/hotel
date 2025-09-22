package com.example.hotelres.owner;

import com.example.hotelres.owner.dto.InventoryBulkCommand;
import com.example.hotelres.owner.dto.InventoryDayDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/owner/hotels/{hotelId}/inventory")
@RequiredArgsConstructor
public class OwnerInventoryController {

    private final OwnerInventoryService service;

    // 조회: /api/owner/hotels/{hotelId}/inventory?roomTypeId=10&from=2025-10-01&to=2025-10-31
    @GetMapping
    public List<InventoryDayDto> getInventory(@AuthenticationPrincipal Object principal,
                                              @PathVariable Long hotelId,
                                              @RequestParam Long roomTypeId,
                                              @RequestParam LocalDate from,
                                              @RequestParam LocalDate to) {
        String loginId = resolveLoginId(principal);
        return service.getInventory(loginId, hotelId, roomTypeId, from, to);
    }

    // 일괄수정: PUT 본문에 InventoryBulkCommand
    @PutMapping("/bulk")
    public ChangedResult bulkUpdate(@AuthenticationPrincipal Object principal,
                                    @PathVariable Long hotelId,
                                    @RequestBody InventoryBulkCommand cmd) {
        String loginId = resolveLoginId(principal);
        int changed = service.bulkUpdate(loginId, hotelId, cmd);
        return new ChangedResult(changed);
    }

    private String resolveLoginId(Object principal) {
        if (principal instanceof UserDetails u) return u.getUsername();
        if (principal instanceof java.util.Map<?,?> m) {
            Object v = m.get("loginId");
            if (v instanceof String s && !s.isBlank()) return s;
            Object sub = m.get("sub");
            if (sub instanceof String s2 && !s2.isBlank()) return s2;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null ? auth.getName() : null);
    }

    public record ChangedResult(int changed) {}
}
