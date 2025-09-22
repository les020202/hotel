package com.example.hotelres.hotel;

import com.example.hotelres.hotel.dto.HotelDetailsDto;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.constraints.Min; // ✅ 추가

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/hotels")
public class HotelDetailsController {

    private final HotelDetailsService service;

    @GetMapping("/{id}")
    public HotelDetailsDto get(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam(defaultValue = "1") @Min(1) Integer guests   // ✅ 추가
    ) {
        if (!checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("checkOut must be after checkIn");
        }
        return service.getDetails(id, checkIn, checkOut, guests);       // ✅ guests 전달
    }
}
