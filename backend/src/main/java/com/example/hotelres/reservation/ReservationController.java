// path: src/main/java/com/example/hotelres/reservation/ReservationController.java
package com.example.hotelres.reservation;

import com.example.hotelres.reservation.dto.HoldDtos.CreateHoldReq;
import com.example.hotelres.reservation.dto.HoldDtos.HoldRes;
import jakarta.validation.Valid;
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
     return ResponseEntity.ok(reservationService.createHold(req));
 }

 /** ✅ 수동 취소 (프런트 취소 버튼이 호출) */
 @DeleteMapping("/hold/{holdCode}")
 public ResponseEntity<Void> cancelHold(@PathVariable String holdCode) {
     reservationService.cancelHoldByCode(holdCode); // 멱등 처리
     return ResponseEntity.noContent().build();
 }

 /** ✅ 만료된 홀드 일괄 정리 (운영 전용) */
 @PostMapping("/holds/release-expired")
 public ResponseEntity<Integer> releaseExpired() {
     return ResponseEntity.ok(reservationService.releaseExpiredHolds());
 }
}
