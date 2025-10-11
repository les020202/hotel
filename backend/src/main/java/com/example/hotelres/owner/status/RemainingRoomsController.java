package com.example.hotelres.owner.status;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/owner/hotels")
public class RemainingRoomsController {

    private final RemainingRoomsService service;

    // 프론트에서 호출: GET /api/owner/hotels/{hotelId}/inventory/today
    @GetMapping("/{hotelId}/inventory/today")
    public Map<String, Long> getTodayRemaining(@PathVariable long hotelId) {
        long total = service.getTodayRemaining(hotelId);
        return Map.of("totalRemainingQty", total);
    }
}
