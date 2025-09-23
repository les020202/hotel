package com.example.hotelres.main.controller;

import com.example.hotelres.main.service.HotelQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/hotels")
public class HotelQueryController {

    private final HotelQueryService hotelQueryService;

    /** 호텔별 최저가: /api/hotels/min-price?ids=1&ids=2&ids=3 */
    @GetMapping("/min-price")
    public ResponseEntity<Map<Long, Integer>> getMinPrices(@RequestParam("ids") List<Long> ids) {
        if (ids == null || ids.isEmpty()) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(hotelQueryService.getMinPricesByHotelIds(ids));
    }

    /** 추천(최저가 기준 상위 N): /api/hotels/recommended?limit=10 */
    @GetMapping("/recommended")
    public ResponseEntity<List<Map<String,Object>>> getRecommended(
            @RequestParam(name = "limit", defaultValue = "10") int limit) {
        return ResponseEntity.ok(hotelQueryService.getRecommended(limit));
    }
}
