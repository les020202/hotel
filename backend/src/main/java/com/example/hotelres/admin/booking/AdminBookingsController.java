// src/main/java/com/example/hotelres/admin/booking/AdminBookingsController.java
package com.example.hotelres.admin.booking;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/bookings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')") // SecurityConfig에서 /api/admin/** 보호 중이면 유지
public class AdminBookingsController {

    private final AdminBookingQueryRepository repo;

    @GetMapping
    public Page<AdminBookingSummary> list(
            @RequestParam(required = false) String status,               // 예약 상태
            @RequestParam(required = false) Long hotelId,                // 호텔 ID
            @RequestParam(required = false) String hotelName,            // ★ 호텔 이름(부분검색)
            @RequestParam(required = false) String loginId,              // 고객 로그인ID(부분검색)
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from, // 기간 시작(체크인 기준)
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,   // 기간 끝
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        // 레포지토리 시그니처: findAdminBookings(status, hotelId, hotelName, loginId, from, to, pageable)
        return repo.findAdminBookings(status, hotelId, hotelName, loginId, from, to, PageRequest.of(page, size));
    }
}
