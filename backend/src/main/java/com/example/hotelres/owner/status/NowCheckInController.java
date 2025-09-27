package com.example.hotelres.owner.status;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/owner/hotels")
public class NowCheckInController {

    private final NowCheckInService service;

    // GET /api/owner/hotels/{hotelId}/checkins/now-count
    @GetMapping("/{hotelId}/checkins/now-count")
    public Map<String, Long> nowCheckInCount(@PathVariable long hotelId) {
        long cnt = service.getTodayCheckedInCount(hotelId);
        return Map.of("nowCheckInCount", cnt);
    }
}
