// src/main/java/com/example/hotelres/owner/BookingService.java
package com.example.hotelres.owner;

import com.example.hotelres.user.User.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BookingItemRepository bookingItemRepository;
    private final BookingDayRepository bookingDayRepository;

    /**
     * 예약 취소(소프트): USER/OWNER/ADMIN 모두 가능
     * - 재고 복구: booking_day.booked -= rooms
     * - 상태 플래그: CANCELLED, canceledAt/By/Reason 세팅
     * - 멱등: 이미 취소면 그냥 종료
     */
    @Transactional
    public void cancelBooking(Long bookingId,
                              Long actorUserId,
                              Role actorRole,
                              String reason) {

        // 1) 예약 가져오기
        BookingEntity b = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "예약이 존재하지 않습니다."));

        // 2) 권한 체크
        switch (actorRole) {
            case ROLE_USER -> {
                if (!b.getUserId().equals(actorUserId)) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 예약만 취소할 수 있습니다.");
                }
            }
            case ROLE_OWNER -> {
                // 자신 호텔의 예약인지 검증
                boolean ok = bookingRepository.existsByIdAndHotelId(b.getId(), b.getHotelId());
                if (!ok) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "해당 호텔 예약만 취소할 수 있습니다.");
                }
            }
            case ROLE_ADMIN -> { /* 모두 허용 */ }
            default -> throw new ResponseStatusException(HttpStatus.FORBIDDEN, "권한이 없습니다.");
        }

        // 3) 멱등: 이미 취소면 끝
        if (b.getStatus() == BookingStatus.CANCELLED) return;

        // 4) 재고 복구 (아이템 수량 사용)
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

        // 5) 상태/메타 업데이트(소프트 취소)
        b.setStatus(BookingStatus.CANCELLED); // 상태명 CANCELLED 확인 완료
        b.setCanceledAt(LocalDateTime.now());
        b.setCancelReason(reason);
        b.setCanceledBy(
                actorRole == Role.ROLE_ADMIN ? "ADMIN" :
                        actorRole == Role.ROLE_OWNER ? "OWNER" : "USER"
        );

        bookingRepository.save(b);
        // (옵션) 결제 환불/취소 연동 지점
    }
}
