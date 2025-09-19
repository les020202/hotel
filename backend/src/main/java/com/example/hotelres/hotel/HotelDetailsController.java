package com.example.hotelres.hotel;

import com.example.hotelres.hotel.dto.HotelDetailsDto;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.constraints.Min;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/hotels")
public class HotelDetailsController {

    private final HotelDetailsService service;

    // ✅ checkIn, checkOut, guests 파라미터가 모두 있을 때만 매칭
    @GetMapping(value = "/{id}", params = {"checkIn","checkOut","guests"})
    public HotelDetailsDto get(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam(defaultValue = "1") @Min(1) Integer guests
    ) {
        if (!checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("checkOut must be after checkIn");
        }
        return service.getDetails(id, checkIn, checkOut, guests);
    }
}
