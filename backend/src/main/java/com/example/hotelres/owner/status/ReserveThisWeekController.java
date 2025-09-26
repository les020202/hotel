package com.example.hotelres.owner.status;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/owner/hotels")
public class ReserveThisWeekController {

    private final ReserveThisWeekService service;

    // GET /api/owner/hotels/{hotelId}/bookings/weekly-count
    @GetMapping("/{hotelId}/bookings/weekly-count")
    public Map<String, Long> weeklyCount(@PathVariable long hotelId) {
        long count = service.getWeeklyCount(hotelId);
        return Map.of("weeklyCount", count);
    }
}
