// path: backend/src/main/java/com/example/hotelres/reservation/ReservationController.java
package com.example.hotelres.reservation;

import com.example.hotelres.reservation.dto.HoldDtos.CreateHoldReq;
import com.example.hotelres.reservation.dto.HoldDtos.HoldRes;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reservations")
@Slf4j
public class ReservationController {

    private final ReservationService reservationService;

    /** (기존) 예약 홀드 생성 */
    @PostMapping("/hold")
    public ResponseEntity<HoldRes> create(@Valid @RequestBody CreateHoldReq req) {
        HoldRes out = reservationService.createHold(req);
        log.info("[HOLD][CREATE] userId={} holdCode={} total={}", req.getUserId(), out.getHoldCode(), out.getTotalAmount());
        return ResponseEntity.ok(out);
    }

    /** ✅ 리프라이스: 쿠폰 적용/해제 후 총액 갱신 */
    @PutMapping("/hold/{holdCode}/reprice")
    public ResponseEntity<HoldRes> reprice(
            @PathVariable String holdCode,
            @RequestBody RepriceReq req
    ) {
        HoldRes out = reservationService.repriceHold(
                holdCode,
                req.getCouponCode(),
                req.getUserId()
        );
        log.info("[HOLD][REPRICE] holdCode={} userId={} total={}", holdCode, req.getUserId(), out.getTotalAmount());
        return ResponseEntity.ok(out);
    }

    @Data
    public static class RepriceReq {
        private String couponCode; // null 또는 빈문자열이면 쿠폰 해제
        private Long userId;       // 옵션(검증용)
    }

    /** ✅ 특정 홀드 단건 조회 (디버깅/동기화용) */
    @GetMapping("/hold/{holdCode}")
    public ResponseEntity<HoldRes> getHold(@PathVariable String holdCode) {
        HoldRes out = reservationService.getHoldByCode(holdCode);
        log.info("[HOLD][GET] holdCode={} total={}", holdCode, out.getTotalAmount());
        return ResponseEntity.ok(out);
    }

    /** ✅ 수동 취소 (프런트 취소 버튼이 호출) */
    @DeleteMapping("/hold/{holdCode}")
    public ResponseEntity<Void> cancelHold(@PathVariable String holdCode) {
        reservationService.cancelHoldByCode(holdCode); // 멱등 처리
        log.info("[HOLD][CANCEL] holdCode={}", holdCode);
        return ResponseEntity.noContent().build();
    }

    /** ✅ 만료된 홀드 일괄 정리 (운영 전용) */
    @PostMapping("/holds/release-expired")
    public ResponseEntity<Integer> releaseExpired() {
        int released = reservationService.releaseExpiredHolds();
        log.info("[HOLD][RELEASE-EXPIRED] released={}", released);
        return ResponseEntity.ok(released);
    }

}