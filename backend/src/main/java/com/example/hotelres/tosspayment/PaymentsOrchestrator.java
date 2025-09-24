package com.example.hotelres.tosspayment;

import com.example.hotelres.common.ApiException;
import com.example.hotelres.owner.BookingEntity;
import com.example.hotelres.owner.BookingItemEntity;
import com.example.hotelres.owner.BookingItemRepository;
import com.example.hotelres.owner.BookingRepository;
import com.example.hotelres.owner.BookingStatus;
import com.example.hotelres.payment.*;
import com.example.hotelres.reservation.*;
import com.example.hotelres.tosspayment.dto.TossConfirmResponse;
import com.example.hotelres.tosspayment.dto.PaymentConfirmResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentsOrchestrator {

    private final TossPaymentService tossPaymentService;
    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final BookingHoldRepository bookingHoldRepository;
    private final BookingItemRepository bookingItemRepository;
    private final CouponIssuanceRepository couponIssuanceRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * @param paymentKey Toss에서 콜백으로 준 키
     * @param orderId    우리가 만든 주문번호
     * @param amount     (무시해도 됨) 프론트가 보내는 금액 — 홀드 금액으로 덮어쓴다
     * @param holdCode   결제 대상 홀드 코드
     */
    @Transactional
    public PaymentConfirmResponse confirmToss(String paymentKey, String orderId, long amount, String holdCode) {
        // 멱등 처리
        var existing = paymentRepository.findByProviderRef(paymentKey);
        if (existing.isPresent()) {
            var p = existing.get();
            return new PaymentConfirmResponse(p.getBookingId(), p.getId(), null);
        }
        log.info(">>> ConfirmToss req: paymentKey={}, orderId={}, reqAmount={}, holdCode={}", paymentKey, orderId, amount, holdCode);
        // 1) 홀드 선조회 & 서버 기준 금액 확보
        BookingHold hold = bookingHoldRepository.findByHoldCode(holdCode);
        
        log.info(">>> Hold from DB: totalAmount={}, discount={}, roomSubtotal={}", hold.getTotalAmount(), hold.getDiscount(), hold.getRoomSubtotal());
        if (hold == null) throw new ApiException("유효하지 않은 holdCode");
        Integer expectedAmount = hold.getTotalAmount();
        if (expectedAmount == null) throw new ApiException("홀드 총액이 비어 있습니다.");

        // (선택) 만료 체크
        if (hold.getExpiresAt() != null && hold.getExpiresAt().isBefore(java.time.LocalDateTime.now())) {
            throw new ApiException("홀드가 만료되었습니다.");
        }

        // 2) Toss 승인 — 반드시 '홀드 금액'으로 승인
        TossConfirmResponse res = tossPaymentService.confirm(paymentKey, orderId, expectedAmount);
        log.info(">>> Toss response: totalAmount={}", res.totalAmount());

        // 3) 금액 일치 검증 (PG 승인금액 vs 서버 홀드금액)
        if (!Objects.equals(expectedAmount, res.totalAmount())) {
            throw new ApiException("결제 금액 불일치(hold=" + expectedAmount + ", paid=" + res.totalAmount() + ")");
        }

     // 4) 예약 확정
        int nights = 1;
        if (hold.getCheckIn() != null && hold.getCheckOut() != null) {
            long d = ChronoUnit.DAYS.between(hold.getCheckIn(), hold.getCheckOut());
            nights = (int) Math.max(1, d);   // 최소 1
        }

        BookingEntity booking = new BookingEntity();
        booking.setUserId(hold.getUserId());
        booking.setHotelId(hold.getHotelId());
        booking.setCheckIn(hold.getCheckIn());
        booking.setCheckOut(hold.getCheckOut());
        booking.setNights(nights);                 // ✅ 기존: between(...) 바로 대입 → nights 사용
        booking.setGuests(hold.getGuests());
        booking.setTotalAmount(res.totalAmount());
        booking.setCurrency(hold.getCurrency());
        booking.setVoucherNo(orderId);
        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);

        // 5) 라인아이템
        BookingItemEntity item = BookingItemEntity.builder()
                .booking(booking)
                .roomTypeId(hold.getRoomTypeId())
                .ratePlanId(hold.getRatePlanId())
                .quantity(nights)                        // ✅ 반드시 채우기! (NULL 금지)
                .priceTotal(hold.getTotalAmount())       // 필요하면 roomSubtotal()로 교체
                .build();
        bookingItemRepository.save(item);
        // 6) 결제 저장
        Payment p = new Payment();
        p.setBookingId(booking.getId());
        p.setUserId(hold.getUserId());
        p.setProvider("toss");
        p.setMethod(mapMethod(res));
        p.setAmount(res.totalAmount());
        p.setCurrency(res.currency());
        p.setStatus(PaymentStatus.SUCCEEDED);
        p.setProviderRef(res.paymentKey());
        if (res.approvedAt() != null) {
            p.setApprovedAt(OffsetDateTime.parse(res.approvedAt()).toLocalDateTime());
        }
        try {
            p.setRawPayload(objectMapper.writeValueAsString(res));
        } catch (Exception ignore) {
            p.setRawPayload("{}");
        }
        paymentRepository.save(p);

        // 7) 쿠폰 사용 처리 (멱등)
        if (hold.getCouponCode() != null && !hold.getCouponCode().isBlank()) {
            var now = java.time.LocalDateTime.now();
            var today = now.toLocalDate();
            couponIssuanceRepository
                    .findAvailableByUserAndCode(hold.getUserId(), hold.getCouponCode(), today, now)
                    .ifPresent(ci -> {
                        ci.setStatus(CouponIssuanceStatus.USED); // ✅ enum 이름 주의
                        ci.setUsedBookingId(booking.getId());
                        couponIssuanceRepository.save(ci);
                    });
        }

        // 8) 홀드 삭제
        bookingHoldRepository.delete(hold);

        String receiptUrl = res.receipt() == null ? null : String.valueOf(res.receipt().get("url"));
        return new PaymentConfirmResponse(booking.getId(), p.getId(), receiptUrl);
    }

    private String mapMethod(TossConfirmResponse r) {
        if (r.card() != null) return "CARD";
        if (r.virtualAccount() != null) return "VBANK";
        if (r.transfer() != null) return "TRANSFER";
        if ("간편결제".equals(r.method()) || r.easyPay() != null) return "EASYPAY";
        return "OTHER";
    }
}
