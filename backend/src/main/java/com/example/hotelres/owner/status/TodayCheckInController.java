package com.example.hotelres.owner.status;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/owner/hotels")
public class TodayCheckInController {

    private final TodayCheckInService service;

    // 프론트: GET /api/owner/hotels/{hotelId}/bookings/today-checkin-count
    @GetMapping("/{hotelId}/bookings/today-checkin-count")
    public Map<String, Long> todayCheckInCount(@PathVariable long hotelId) {
        long count = service.getTodayCount(hotelId);
        return Map.of("todayCheckInCount", count);
    }
}
