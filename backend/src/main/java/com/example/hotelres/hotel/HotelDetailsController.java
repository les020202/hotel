// src/main/java/com/example/hotelres/hotel/HotelDetailsController.java
package com.example.hotelres.hotel;

import com.example.hotelres.hotel.dto.HotelDetailsDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.constraints.Min;
import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/hotels")
@Slf4j
public class HotelDetailsController {

    private final HotelDetailsService service;
    private final HotelDetailsRepository hotelDetailsRepository; // ✅ 존재 체크용 (필요시 주입)

    // ✅ checkIn, checkOut, guests 가 모두 있어야 매핑
    @GetMapping(value = "/{id}", params = {"checkIn","checkOut","guests"})
    public HotelDetailsDto get(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam(defaultValue = "1") @Min(1) Integer guests
    ) {
        log.info("[HotelDetails] id={}, checkIn={}, checkOut={}, guests={}", id, checkIn, checkOut, guests);

        if (!checkOut.isAfter(checkIn)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "checkOut must be after checkIn");
        }

        // 호텔 존재 여부 가드 (노트북 DB엔 있고 PC DB엔 없을 때 500 대신 404로)
        if (!hotelDetailsRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "HOTEL_NOT_FOUND");
        }

        try {
            return service.getDetails(id, checkIn, checkOut, guests);
        } catch (ResponseStatusException e) {
            // 서비스에서 의미 있는 상태코드로 던진 건 그대로 전달
            log.error("[HotelDetails] service error: {}", e.getReason(), e);
            throw e;
        } catch (Exception e) {
            // 원인 로깅 후 500으로 래핑
            log.error("[HotelDetails] FAILED id={}, ci={}, co={}, guests={}", id, checkIn, checkOut, guests, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "DETAILS_BUILD_FAILED");
        }
    }
}
