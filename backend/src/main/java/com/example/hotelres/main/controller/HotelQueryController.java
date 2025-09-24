// backend/src/main/java/com/example/hotelres/main/controller/HotelQueryController.java
package com.example.hotelres.main.controller;

import com.example.hotelres.main.service.HotelQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class HotelQueryController {

    private final HotelQueryService hotelQueryService;

    /** (기존 유지) /api/hotels/min-price */
    @GetMapping("/hotels/min-price")
    public ResponseEntity<Map<Long, Integer>> getMinPrices(@RequestParam("ids") List<Long> ids) {
        if (ids == null || ids.isEmpty()) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(hotelQueryService.getMinPricesByHotelIds(ids));
    }

    /** (기존 유지) /api/hotels/recommended */
    @GetMapping("/hotels/recommended")
    public ResponseEntity<List<Map<String,Object>>> getRecommended(
            @RequestParam(name = "limit", defaultValue = "10") int limit) {
        return ResponseEntity.ok(hotelQueryService.getRecommended(limit));
    }

    /** ⬇️ 추가: 프론트 호환. /api/search/hotels?page=0&size=5
     *   - checkIn/checkOut 파라미터 없는 요청만 이 메서드가 처리
     *   - 카드 데이터를 Page-like 포맷으로 래핑
     */
    @GetMapping(value = "/search/hotels", params = {"!checkIn", "!checkOut"})
    public Map<String, Object> searchHotelsWithoutDates(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        var list = hotelQueryService.getRecommendedCards(size);
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("content", list);           // ← RandomHotels.vue가 그대로 사용
        resp.put("number", 0);
        resp.put("size", size);
        resp.put("totalElements", list.size());
        resp.put("totalPages", 1);
        return resp;
    }

    /** ⬇️ 추가: 프론트 호환. /api/search/min-prices?ids=1&ids=2 */
    @GetMapping(value = "/search/min-prices", params = "ids")
    public Map<Long, Integer> searchMinPricesByIds(@RequestParam("ids") List<Long> ids) {
        return hotelQueryService.getMinPricesByHotelIds(ids);
    }
    
    /** /api/search/min-prices?hotelIds=1&hotelIds=2 또는 hotelIds=1,2 */
    @GetMapping(value = "/search/min-prices", params = "hotelIds")
    public Map<Long, Integer> searchMinPricesByHotelIds(@RequestParam("hotelIds") List<Long> hotelIds) {
        return hotelQueryService.getMinPricesByHotelIds(hotelIds);
    }
}
