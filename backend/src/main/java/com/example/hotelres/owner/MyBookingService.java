// src/main/java/com/example/hotelres/owner/MyBookingService.java
package com.example.hotelres.owner;

import com.example.hotelres.owner.dto.MyBookingDetailDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class MyBookingService {

    private final BookingRepository bookingRepository;
    private final BookingItemRepository bookingItemRepository;
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Transactional(readOnly = true)
    public MyBookingDetailDto getMyBookingDetail(Long bookingId, Long userId) {
        BookingEntity b = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("예약이 존재하지 않습니다."));
        if (!b.getUserId().equals(userId)) {
            throw new IllegalStateException("본인 예약만 조회할 수 있습니다.");
        }

        // 객실명 등은 기존 로직대로 조합 (예시로 첫 아이템만)
        var items = bookingItemRepository.findByBooking_Id(b.getId());
        String roomTypeName = (items != null && !items.isEmpty()) ? "객실" : "";

        return MyBookingDetailDto.builder()
                .bookingId(b.getId())
                .status(b.getStatus().name())
                .hotelId(b.getHotelId())
                .hotelName(null)     // 필요시 조인/조회로 채우기
                .roomTypeName(roomTypeName)
                .checkIn(b.getCheckIn()  != null ? b.getCheckIn().toString()  : null)
                .checkOut(b.getCheckOut() != null ? b.getCheckOut().toString() : null)
                .nights(b.getNights())
                .guests(b.getGuests())
                .totalAmount(b.getTotalAmount())
                .currency(b.getCurrency())

                // ✅ 취소 메타 매핑
                .canceledAt(b.getCanceledAt() != null ? b.getCanceledAt().format(ISO) : null)
                .canceledBy(b.getCanceledBy())
                .cancelReason(b.getCancelReason())
                .build();
    }
}
