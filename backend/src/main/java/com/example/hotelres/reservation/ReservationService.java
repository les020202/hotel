// path: backend/src/main/java/com/example/hotelres/reservation/ReservationService.java
package com.example.hotelres.reservation;

import com.example.hotelres.common.ApiException;
import com.example.hotelres.reservation.dto.HoldDtos.CreateHoldReq;
import com.example.hotelres.reservation.dto.HoldDtos.HoldRes;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReservationService {

    /** 홀드 TTL(분) */
    private static final int HOLD_TTL_MINUTES = 15;

    private final BookingDayRepository bookingDayRepository;
    private final BookingHoldRepository bookingHoldRepository;
    private final CouponIssuanceRepository couponIssuanceRepository; // 쿠폰 지급 조회

    /** 홀드 생성 (선점) */
    @Transactional
    public HoldRes createHold(CreateHoldReq req) {
        if (!req.getCheckOut().isAfter(req.getCheckIn())) {
            throw new IllegalArgumentException("체크아웃은 체크인보다 뒤여야 합니다.");
        }

        final long nights = ChronoUnit.DAYS.between(req.getCheckIn(), req.getCheckOut());
        final int qty = 1; // 객실 단위
        final int guests = req.getGuests();

        // 1) 재고 범위 잠금
        List<BookingDay> days = bookingDayRepository.findForUpdate(
                req.getHotelId(), req.getRoomTypeId(), req.getCheckIn(), req.getCheckOut());

        if (days.size() != nights) {
            throw new SoldOutException("선택 구간에 판매 가능한 재고가 없습니다.");
        }

        // 2) 재고 검증
        for (BookingDay d : days) {
            if (!Boolean.TRUE.equals(d.getIsSellable()) || d.getRemainingQty() < qty) {
                throw new SoldOutException("재고 부족: " + d.getStayDate());
            }
        }

        // 3) 하드 홀드: booked += qty
        for (BookingDay d : days) {
            int booked = Objects.requireNonNullElse(d.getBooked(), 0);
            d.setBooked(booked + qty);
        }

        // 4) 금액 계산
        int subtotal = days.stream()
                .map(d -> Objects.requireNonNullElse(d.getPrice(), 0))
                .mapToInt(p -> p * qty)
                .sum();

        // 쿠폰 검증/할인
        int discount = 0;
        String couponCode = req.getCouponCode();
        boolean couponApplied = false;
        if (couponCode != null && !couponCode.isBlank()) {
            var now   = LocalDateTime.now();
            var today = now.toLocalDate();
            var opt   = couponIssuanceRepository
                    .findAvailableByUserAndCode(req.getUserId(), couponCode, today, now);
            if (opt.isPresent()) {
                int amount = java.util.Optional.ofNullable(opt.get().getCoupon().getAmount()).orElse(0);
                discount = Math.min(amount, subtotal);
                couponApplied = (discount > 0);
            }
        }
        int total = Math.max(0, subtotal - discount);

        // 5) 홀드 저장
        BookingHold hold = new BookingHold();
        hold.setUserId(req.getUserId());
        hold.setHotelId(req.getHotelId());
        hold.setRoomTypeId(req.getRoomTypeId());
        hold.setRatePlanId(req.getRatePlanId());
        hold.setCheckIn(req.getCheckIn());
        hold.setCheckOut(req.getCheckOut());
        hold.setGuests(guests);
        hold.setCouponCode(couponApplied ? couponCode : null); // ✅ 유효할 때만 저장
        hold.setRoomSubtotal(subtotal);
        hold.setDiscount(discount);
        hold.setTotalAmount(total);
        hold.setCurrency("KRW");
        hold.setExpiresAt(LocalDateTime.now().plusMinutes(HOLD_TTL_MINUTES));
        hold.setHoldCode(generateUniqueHoldCode());

        BookingHold saved = bookingHoldRepository.save(hold);
        return new HoldRes(saved.getHoldCode(), saved.getExpiresAt(), saved.getTotalAmount());
    }

    /** ✅ 홀드 리프라이스(Reprice) — 쿠폰 변경 시 총액 갱신 */
    @Transactional
    public HoldRes repriceHold(String holdCode, String couponCode, Long userId) {
        BookingHold hold = bookingHoldRepository.findByHoldCode(holdCode);
        if (hold == null) {
            throw new ApiException("유효하지 않은 holdCode");
        }
        // (옵션) 보안: 요청자 검증
        if (userId != null && !userId.equals(hold.getUserId())) {
            throw new ApiException("요청자와 홀드의 사용자 정보가 일치하지 않습니다.");
        }

        int subtotal = Objects.requireNonNullElse(hold.getRoomSubtotal(), 0);
        int discount = 0;

        if (couponCode != null && !couponCode.isBlank()) {
            var now   = LocalDateTime.now();
            var today = now.toLocalDate();
            var opt   = couponIssuanceRepository
                    .findAvailableByUserAndCode(hold.getUserId(), couponCode, today, now);
            if (opt.isEmpty()) {
                throw new ApiException("유효하지 않거나 사용 불가한 쿠폰");
            }
            int amount = java.util.Optional.ofNullable(opt.get().getCoupon().getAmount()).orElse(0);
            discount = Math.min(amount, subtotal);
            hold.setCouponCode(couponCode);
        } else {
            // 쿠폰 해제
            hold.setCouponCode(null);
        }

        int total = Math.max(0, subtotal - discount);
        hold.setDiscount(discount);
        hold.setTotalAmount(total);

        // ✅ reprice 시에도 TTL 갱신
        hold.setExpiresAt(LocalDateTime.now().plusMinutes(HOLD_TTL_MINUTES));

        bookingHoldRepository.saveAndFlush(hold);
        return new HoldRes(hold.getHoldCode(), hold.getExpiresAt(), hold.getTotalAmount());
    }

@Transactional
public HoldRes getHoldByCode(String holdCode) {
    BookingHold hold = bookingHoldRepository.findByHoldCode(holdCode);
    if (hold == null) {
        throw new ApiException("존재하지 않는 holdCode: " + holdCode);
    }
    return new HoldRes(
            hold.getHoldCode(),
            hold.getExpiresAt(),
            hold.getTotalAmount()
    );
}

    /** 만료된 홀드 일괄 해제 */
    @Transactional
    public int releaseExpiredHolds() {
        LocalDateTime now = LocalDateTime.now();
        List<BookingHold> expired = bookingHoldRepository.findAllByExpiresAtBefore(now);
        int released = 0;

        for (BookingHold hold : expired) {
            List<BookingDay> days = bookingDayRepository.findForUpdate(
                    hold.getHotelId(), hold.getRoomTypeId(), hold.getCheckIn(), hold.getCheckOut());

            final int qty = 1;
            for (BookingDay d : days) {
                int booked = Objects.requireNonNullElse(d.getBooked(), 0);
                d.setBooked(Math.max(0, booked - qty));
            }
            bookingHoldRepository.delete(hold);
            released++;
        }
        return released;
    }

    /** 특정 홀드 코드 수동 취소(멱등) */
    @Transactional
    public void cancelHoldByCode(String holdCode) {
        BookingHold hold = bookingHoldRepository.findByHoldCode(holdCode);
        if (hold == null) return;

        List<BookingDay> days = bookingDayRepository.findForUpdate(
                hold.getHotelId(), hold.getRoomTypeId(), hold.getCheckIn(), hold.getCheckOut());

        final int qty = 1;
        for (BookingDay d : days) {
            int booked = Objects.requireNonNullElse(d.getBooked(), 0);
            d.setBooked(Math.max(0, booked - qty));
        }
        bookingHoldRepository.delete(hold);
    }

    /** 중복 방지 홀드 코드 생성 — UNIQUE 제약과 병행 사용 */
    private String generateUniqueHoldCode() {
        String code;
        do {
            code = "HLD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (bookingHoldRepository.existsByHoldCode(code));
        return code;
    }
}
