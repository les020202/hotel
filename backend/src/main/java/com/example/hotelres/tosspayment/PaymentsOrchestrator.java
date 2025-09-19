package com.example.hotelres.tosspayment;

import com.example.common.error.ApiException;
import com.example.hotelres.payment.Payment;
import com.example.hotelres.payment.PaymentRepository;
import com.example.hotelres.payment.PaymentStatus;
import com.example.hotelres.reservation.Booking;
import com.example.hotelres.reservation.BookingRepository;
import com.example.hotelres.reservation.BookingService;
import com.example.hotelres.tosspayment.dto.PaymentConfirmResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentsOrchestrator {

    private final BookingService bookingService;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final TossPaymentService tossPaymentService;

    /**
     * 결제 승인 → (없으면) 예약 생성 → Payment 저장 → 예약 확정 → DTO 응답
     * - orderId는 voucher_no로 사용
     */
    @Transactional
    public PaymentConfirmResponse confirmToss(String paymentKey, String orderId, int amount, String holdCode) {
        if (orderId == null || orderId.isBlank()) {
            throw new ApiException(400, "MISSING_ORDER_ID", "orderId가 없습니다.");
        }

        // 0) 멱등: 결제 성공 기록이 이미 있으면, booking도 함께 확인 후 즉시 응답
        Payment already = paymentRepository.findByOrderId(orderId).orElse(null);
        if (already != null && already.getStatus() == PaymentStatus.SUCCEEDED) {
            Booking b = bookingRepository.findByVoucherNo(orderId)
                    .orElseThrow(() -> new ApiException(500, "BOOKING_MISSING", "기존 결제는 있으나 예약 정보가 없습니다."));
            return buildResponse(already, b);
        }

        // 1) Toss 결제 승인
        Map<String, Object> toss = tossPaymentService.confirm(paymentKey, orderId, amount);

        // 2) 예약 조회 or 생성(결제 시점 생성 플로우)
        Booking booking = bookingRepository.findByVoucherNo(orderId).orElse(null);
        if (booking == null) {
            if (holdCode == null || holdCode.isBlank()) {
                throw new ApiException(400, "MISSING_HOLD_CODE", "holdCode가 없습니다.");
            }
            booking = bookingService.createFromHold(holdCode, orderId, amount);
        } else {
            // 기존 예약이 있다면 금액 검증
            if (booking.getTotalAmount() != amount) {
                throw new ApiException(409, "AMOUNT_MISMATCH", "결제 금액이 예약 금액과 다릅니다.");
            }
        }

        // 3) Payment 저장/갱신
        Payment p = (already != null) ? already : new Payment();
        p.setOrderId(orderId);
        p.setPaymentKey((String) toss.get("paymentKey"));
        p.setAmount(((Number) toss.get("totalAmount")).intValue());
        p.setStatus(PaymentStatus.SUCCEEDED);

        String method = (String) toss.getOrDefault("method", "");
        Map<?, ?> easy = (Map<?, ?>) toss.get("easyPay");
        String methodLabel = (easy != null && easy.get("provider") != null)
                ? method + " · " + easy.get("provider")
                : method;
        p.setMethodLabel(methodLabel);

        Map<?, ?> receipt = (Map<?, ?>) toss.get("receipt");
        if (receipt != null) p.setReceiptUrl((String) receipt.get("url"));

        String approvedAt = (String) toss.get("approvedAt");
        if (approvedAt != null) p.setApprovedAt(OffsetDateTime.parse(approvedAt));

        p.setRawPayload(toss.toString()); // 감사/분석용 원문 보관
        paymentRepository.save(p);

        // 4) 예약 확정
        bookingService.confirm(booking);

        // 5) 응답
        return buildResponse(p, booking);
    }

    private PaymentConfirmResponse buildResponse(Payment p, Booking b) {
        var payDto = PaymentConfirmResponse.PaymentDto.builder()
                .amount(p.getAmount())
                .orderId(p.getOrderId())
                .methodLabel(p.getMethodLabel())
                .approvedAtDisplay(p.getApprovedAt() != null ? p.getApprovedAt().toString() : null)
                .receiptUrl(p.getReceiptUrl())
                .build();

        var bookingDto = PaymentConfirmResponse.BookingDto.builder()
                .id(b.getId())
                .checkIn(b.getCheckIn() != null ? b.getCheckIn().toString() : null)
                .checkOut(b.getCheckOut() != null ? b.getCheckOut().toString() : null)
                .nights(b.getNights())
                .guests(b.getGuests())
                .policy("체크인 3일 전까지 전액 환불") // 실제 정책 있으면 교체
                .build();

        return PaymentConfirmResponse.builder()
                .payment(payDto)
                .booking(bookingDto)
                .build();
    }
}
