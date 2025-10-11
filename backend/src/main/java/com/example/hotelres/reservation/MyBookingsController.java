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

    private final MyBookingQueryRepository queryRepo;
    private final UserRepository userRepository;

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
    // 프로젝션 → DTO 매핑 (⚠️ 레코드 생성자 인자 순서 = 레코드 정의 순서!)
    // ─────────────────────────────────────────────────────────
    private MyBookingSummary toDto(MyBookingQueryRepository.MyBookingRow r) {
        LocalDate in  = toLocalDate(r.getCheckIn());
        LocalDate out = toLocalDate(r.getCheckOut());

        return new MyBookingSummary(
                r.getBookingId(),
                r.getStatus(),

                r.getHotelId(),       // ✅ 추가
                r.getHotelName(),
                r.getRoomTypeName(),

                in,
                out,
                r.getNights(),
                r.getGuests(),
                r.getTotalAmount(),
                r.getCurrency(),
                r.getReceiptUrl(),

                r.getCanceledAt(),
                r.getCanceledBy(),
                r.getCancelReason(),

                r.getGuestName(),
                r.getGuestPhone()
        );
    }

    private LocalDate toLocalDate(Object v) {
        if (v == null) return null;
        if (v instanceof LocalDate ld) return ld;
        if (v instanceof java.sql.Date sd) return sd.toLocalDate();
        return LocalDate.parse(String.valueOf(v)); // "YYYY-MM-DD"
    }
}
