// src/main/java/com/example/hotelres/owner/BookingService.java
package com.example.hotelres.owner;

import com.example.hotelres.user.User.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.*; // ★ 추가: LocalDate, LocalTime, ZoneId 등
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BookingItemRepository bookingItemRepository;
    private final BookingDayRepository bookingDayRepository;

    @Transactional
    public void cancelBooking(Long bookingId,
                              Long actorUserId,
                              Role actorRole,
                              String reason) {

        // 1) 예약
        BookingEntity b = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "예약이 존재하지 않습니다."));

        // 2) 권한
        switch (actorRole) {
            case ROLE_USER -> {
                if (!b.getUserId().equals(actorUserId)) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 예약만 취소할 수 있습니다.");
                }
            }
            case ROLE_OWNER -> {
                boolean ok = bookingRepository.existsByIdAndHotelId(b.getId(), b.getHotelId());
                if (!ok) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "해당 호텔 예약만 취소할 수 있습니다.");
            }
            case ROLE_ADMIN -> { /* 모두 허용 */ }
            default -> throw new ResponseStatusException(HttpStatus.FORBIDDEN, "권한이 없습니다.");
        }

        // 3) 이미 취소면 멱등
        if (b.getStatus() == BookingStatus.CANCELLED) return;

        // 4) ★ 취소 마감(컷오프) 검증
        //    - 시스템 시간대: Asia/Seoul (원하면 시스템 기본으로 바꿔도 됨)
        ZoneId zone = ZoneId.of("Asia/Seoul");
        LocalDateTime now = LocalDateTime.now(zone);

        LocalDate checkIn = b.getCheckIn();
        // USER: 전날 23:59:59.999999999
        LocalDateTime userDeadline   = LocalDateTime.of(checkIn.minusDays(1), LocalTime.MAX);
        // OWNER/ADMIN: 당일 23:59:59.999999999
        LocalDateTime staffDeadline  = LocalDateTime.of(checkIn,           LocalTime.MAX);

        LocalDateTime deadline = switch (actorRole) {
            case ROLE_USER  -> userDeadline;
            case ROLE_OWNER, ROLE_ADMIN -> staffDeadline;
        };

        if (now.isAfter(deadline)) {
            String msg = (actorRole == Role.ROLE_USER)
                    ? "체크인 전날까지 취소할 수 있습니다."
                    : "체크인 당일까지 취소할 수 있습니다.";
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
        }

        // 5) 재고 복구
        List<BookingItemEntity> items = bookingItemRepository.findByBooking_Id(b.getId());
        if (items != null) {
            for (var it : items) {
                int rooms = Math.max(1, it.getQuantity() == null ? 1 : it.getQuantity());
                bookingDayRepository.restoreInventory(
                        b.getHotelId(),
                        it.getRoomTypeId(),
                        b.getCheckIn(),
                        b.getCheckOut(),   // check-out은 배타
                        rooms
                );
            }
        }

        // 6) 상태/메타 업데이트
        b.setStatus(BookingStatus.CANCELLED);
        b.setCanceledAt(LocalDateTime.now(zone));
        b.setCancelReason(reason);
        b.setCanceledBy(
                actorRole == Role.ROLE_ADMIN ? "ADMIN" :
                        actorRole == Role.ROLE_OWNER ? "OWNER" : "USER"
        );

        bookingRepository.save(b);
    }
}
