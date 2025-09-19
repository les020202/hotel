package com.example.hotelres.main.controller;

import lombok.RequiredArgsConstructor;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.hotelres.main.model.Hotel;
import com.example.hotelres.main.service.HotelPriceService;
import com.example.hotelres.main.service.HotelQueryService;

@RestController
@RequestMapping("/api/hotels")
@RequiredArgsConstructor
public class HotelController {

    private final HotelQueryService hotelQueryService;
    private final HotelPriceService hotelPriceService;

    @GetMapping("/search")
    public ResponseEntity<Page<Hotel>> search(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "region", required = false) String region,
            @RequestParam(name = "regionExact", defaultValue = "false") boolean regionExact,
            @RequestParam(name = "gradeMin", required = false) Integer gradeMin,
            @RequestParam(name = "hasHomepage", required = false) Boolean hasHomepage,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "12") int size,
            @RequestParam(name = "sort", defaultValue = "id,asc") String sort
    ) {
        Sort s = toSort(sort);
        Pageable pageable = PageRequest.of(page, size, s);
        var result = hotelQueryService.search(q, region, regionExact, gradeMin, hasHomepage, pageable);
        return ResponseEntity.ok(result);
    }

    // ✅ checkIn/checkOut/guests 가 없을 때만 매칭
    @GetMapping(value = "/{id}", params = {"!checkIn","!checkOut","!guests"})
    public ResponseEntity<Hotel> getOne(@PathVariable("id") Long id) {
        return ResponseEntity.ok(hotelQueryService.getOne(id));
    }

    @GetMapping("/min-price")
    public Map<Long, Integer> minPrice(@RequestParam List<Long> ids) {
        return hotelPriceService.minPrices(new HashSet<>(ids));
    }

    private Sort toSort(String sort) {
        try {
            String[] t = sort.split(",");
            String prop = t[0].trim();
            Sort.Direction dir = (t.length > 1 && "desc".equalsIgnoreCase(t[1].trim()))
                    ? Sort.Direction.DESC : Sort.Direction.ASC;
            return Sort.by(dir, prop);
        } catch (Exception e) {
            return Sort.by(Sort.Direction.ASC, "id");
        }
    }
}
