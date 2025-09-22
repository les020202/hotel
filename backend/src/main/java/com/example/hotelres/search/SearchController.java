package com.example.hotelres.search;

import com.example.hotelres.common.dto.PagedResponse;
import com.example.hotelres.search.dto.HotelCardDto;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService service;

    @GetMapping("/hotels")
    public PagedResponse<HotelCardDto> hotels(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam(required = false) String region,
            @RequestParam(defaultValue = "1") Integer guests,   // ✅ 추가
            @RequestParam(defaultValue = "5") Integer limit,
            @RequestParam(defaultValue = "0") Integer offset
    ) {
        if (region != null && region.isBlank()) region = null;
        if (!checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("checkOut must be after checkIn");
        }
        // ✅ guests 서비스로 전달
        return service.search(checkIn, checkOut, region, guests, limit, offset);
    }
}
