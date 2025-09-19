// path: src/main/java/com/example/hotelres/reservation/ReservationService.java
package com.example.hotelres.reservation;

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

/*
 * ================================================================
 * ReservationService
 * ---------------------------------------------------------------
 * 목적
 *  - 예약 "홀드(선점)" 생성/관리. 결제 직전, 선택한 날짜 범위를 잠시 고정.
 *
 * 동시성 제어(핵심)
 *  - BookingDayRepository.findForUpdate(...) 가 PESSIMISTIC_WRITE(SELECT ... FOR UPDATE)
 *    를 사용하여 [호텔, 룸타입, 기간] 범위를 잠금 → 동일 구간 동시 진입을 차단.
 *
 * 재고 검증
 *  - DB의 가상컬럼(Generated Column)인 remaining_qty, is_sellable 을 그대로 사용.
 *    (booking_day DDL: remaining_qty AS GREATEST(allotment-booked,0), is_sellable AS CASE ...)
 *  - 엔티티 BookingDay에 아래와 같이 "읽기 전용" 매핑되어 있어야 함:
 *      @Column(name="remaining_qty", insertable=false, updatable=false) private Integer remainingQty;
 *      @Column(name="is_sellable", insertable=false, updatable=false) private Boolean isSellable;
 *
 * 재고 차감 정책(하드 홀드)
 *  - 홀드 생성 시점에 booked += qty 로 "실제 수량"을 확보.
 *  - 만료/취소 시 반드시 booked 를 복구해야 하므로 release/cancel 메서드를 제공.
 *
 * 만료 처리
 *  - releaseExpiredHolds(): expires_at < now 인 홀드를 찾아 booked 복구 후 홀드 삭제.
 *  - 운영에선 @Scheduled 로 1~5분 주기 실행을 권장.
 *
 * 주의
 *  - days.size() != nights 이면(누락된 날짜가 있으면) 판매 불가로 간주.
 *  - currency 는 CHAR(3) 스키마 기준. 길이 제한을 엔티티에 두는 것을 추천.
 * ================================================================
 */

@Service
@RequiredArgsConstructor
public class ReservationService {

    /** 홀드 TTL(분) — 필요 시 설정값으로 분리 */
    private static final int HOLD_TTL_MINUTES = 15;

    private final BookingDayRepository bookingDayRepository;
    private final BookingHoldRepository bookingHoldRepository;

    /**
     * 홀드 생성 (선점)
     * 흐름:
     *  1) 기본 검증(기간/인원)
     *  2) 대상 구간 booking_day 범위를 비관적락으로 잠금
     *  3) DB 생성컬럼 사용해 재고 검증(isSellable, remainingQty)
     *  4) 하드 홀드(booked += qty)
     *  5) booking_holds 레코드 생성(만료시각 포함)
     */
    @Transactional
    public HoldRes createHold(CreateHoldReq req) {
        // 0) 파라미터 기본 검증
        if (!req.getCheckOut().isAfter(req.getCheckIn())) {
            throw new IllegalArgumentException("체크아웃은 체크인보다 뒤여야 합니다.");
        }

        final long nights = ChronoUnit.DAYS.between(req.getCheckIn(), req.getCheckOut());
        final int qty = 1; // 항상 객실 단위로만 차감
        final int guests = req.getGuests(); // 인원수는 별도로 저장

        // 1) 재고 범위를 잠그기 (비관적 락) — 같은 범위 동시 진입 차단
        List<BookingDay> days = bookingDayRepository.findForUpdate(
                req.getHotelId(), req.getRoomTypeId(), req.getCheckIn(), req.getCheckOut());

        // 날짜 누락(행 부재) 시 판매 불가로 처리
        if (days.size() != nights) {
            throw new SoldOutException("선택 구간에 판매 가능한 재고가 없습니다.");
        }

        // 2) 재고 검증 (DB 가상컬럼 활용: is_sellable, remaining_qty)
        for (BookingDay d : days) {
            if (!Boolean.TRUE.equals(d.getIsSellable()) || d.getRemainingQty() < qty) {
                throw new SoldOutException("재고 부족: " + d.getStayDate());
            }
        }

        // 3) 하드 홀드 정책: booked += qty (※ 만료/취소 시 복구 필요)
        for (BookingDay d : days) {
            int booked = Objects.requireNonNullElse(d.getBooked(), 0);
            d.setBooked(booked + qty);
            // JPA 영속상태이므로 @Transactional 커밋 시점에 UPDATE 반영됨
        }

        // 4) 금액 계산(박당 가격 * 수량)
        int subtotal = days.stream()
                .map(d -> Objects.requireNonNullElse(d.getPrice(), 0))
                .mapToInt(p -> p * qty)
                .sum();

        // 5) 홀드 행 생성
        var hold = new BookingHold();
        hold.setUserId(req.getUserId());
        hold.setHotelId(req.getHotelId());
        hold.setRoomTypeId(req.getRoomTypeId());
        hold.setRatePlanId(req.getRatePlanId());
        hold.setCheckIn(req.getCheckIn());
        hold.setCheckOut(req.getCheckOut());
        hold.setGuests(guests); // 인원 기록은 따로 저장
        hold.setCouponCode(req.getCouponCode());
        hold.setRoomSubtotal(subtotal);
        hold.setDiscount(0);
        hold.setTotalAmount(subtotal);
        hold.setCurrency("KRW"); // 스키마: CHAR(3)
        hold.setExpiresAt(LocalDateTime.now().plusMinutes(HOLD_TTL_MINUTES));
        hold.setHoldCode(generateUniqueHoldCode()); // UNIQUE 제약 + 중복 방지

        BookingHold saved = bookingHoldRepository.save(hold);

        return new HoldRes(saved.getHoldCode(), saved.getExpiresAt(), saved.getTotalAmount());
    }

    /**
     * 만료된 홀드 일괄 해제 (스케줄러/관리자 호출용)
     *  - expires_at < now 인 홀드를 조회
     *  - 동일 기간 booking_day 범위를 다시 잠그고 booked 를 복구
     *  - 홀드 레코드 삭제
     * 반환값: 정리된(삭제된) 홀드 개수
     */
    @Transactional
    public int releaseExpiredHolds() {
        LocalDateTime now = LocalDateTime.now();
        List<BookingHold> expired = bookingHoldRepository.findAllByExpiresAtBefore(now);
        int released = 0;

        for (BookingHold hold : expired) {
            // 범위를 다시 잠그고 안전하게 복구
            var days = bookingDayRepository.findForUpdate(
                    hold.getHotelId(), hold.getRoomTypeId(), hold.getCheckIn(), hold.getCheckOut());

            int qty = Math.max(1, hold.getGuests());
            for (BookingDay d : days) {
                int booked = Objects.requireNonNullElse(d.getBooked(), 0);
                d.setBooked(Math.max(0, booked - qty));
            }

            bookingHoldRepository.delete(hold);
            released++;
        }
        return released;
    }

    /**
     * 특정 홀드 코드 수동 취소(사용자 취소/백오피스 처리)
     *  - 홀드가 존재하면 동일 기간을 잠그고 booked 복구 후 홀드 삭제
     *  - 존재하지 않으면 무시 (idempotent)
     */
    @Transactional
    public void cancelHoldByCode(String holdCode) {
        BookingHold hold = bookingHoldRepository.findByHoldCode(holdCode);
        if (hold == null) return;

        var days = bookingDayRepository.findForUpdate(
                hold.getHotelId(), hold.getRoomTypeId(), hold.getCheckIn(), hold.getCheckOut());

        int qty = Math.max(1, hold.getGuests());
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
