// src/main/java/com/example/hotelres/reservation/MyBookingsController.java
package com.example.hotelres.reservation;

import com.example.hotelres.reservation.dto.MyBookingSummary;
import com.example.hotelres.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/my/bookings")
@RequiredArgsConstructor
public class MyBookingsController {

    private final MyBookingQueryRepository queryRepo; // 네이티브/프로젝션 레포
    private final UserRepository userRepository;      // loginId → userId

    private Long requireUserId(String loginId) {
        return userRepository.findIdByLoginId(loginId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "사용자를 찾을 수 없습니다."));
    }

    @GetMapping
    public Page<MyBookingSummary> list(
            @AuthenticationPrincipal(expression = "username") String loginId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Long userId = requireUserId(loginId);

        var rows = queryRepo.findMyBookings(userId, PageRequest.of(page, size));
        var mapped = rows.getContent().stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return new PageImpl<>(mapped, rows.getPageable(), rows.getTotalElements());
    }

    @GetMapping("/{id}")
    public MyBookingSummary one(
            @AuthenticationPrincipal(expression = "username") String loginId,
            @PathVariable Long id
    ) {
        Long userId = requireUserId(loginId);

        var row = queryRepo.findMyBooking(userId, id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "예약을 찾을 수 없습니다."));

        return toDto(row);
    }

    // ─────────────────────────────────────────────────────────
    // 프로젝션 → DTO 매핑
    // ─────────────────────────────────────────────────────────
    private MyBookingSummary toDto(MyBookingQueryRepository.MyBookingRow r) {
        LocalDate in  = toLocalDate(r.getCheckIn());
        LocalDate out = toLocalDate(r.getCheckOut());

        return new MyBookingSummary(
                r.getBookingId(),
                r.getStatus(),
                r.getHotelName(),
                r.getRoomTypeName(),
                in,
                out,
                r.getNights(),
                r.getGuests(),
                r.getTotalAmount(),
                r.getCurrency(),
                r.getReceiptUrl(),
                // ▼ 취소 메타 (레포에서 DATE_FORMAT으로 String으로 내려오게 해둠)
                r.getCanceledAt(),
                r.getCanceledBy(),
                r.getCancelReason()
        );
    }

    // 프로젝션의 날짜가 String("YYYY-MM-DD")이나 java.sql.Date로 오는 경우 대비
    private LocalDate toLocalDate(Object v) {
        if (v == null) return null;
        if (v instanceof LocalDate ld) return ld;
        if (v instanceof java.sql.Date sd) return sd.toLocalDate();
        return LocalDate.parse(String.valueOf(v)); // "YYYY-MM-DD" 가정
    }
}
