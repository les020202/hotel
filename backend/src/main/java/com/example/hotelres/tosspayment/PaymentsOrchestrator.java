package com.example.hotelres.tosspayment;

import com.example.hotelres.auth.EmailService;
import com.example.hotelres.common.ApiException;
import com.example.hotelres.owner.BookingEntity;
import com.example.hotelres.owner.BookingItemEntity;
import com.example.hotelres.owner.BookingItemRepository;
import com.example.hotelres.owner.BookingRepository;
import com.example.hotelres.owner.BookingStatus;
import com.example.hotelres.payment.*;
import com.example.hotelres.reservation.*;
import com.example.hotelres.reservation.MyBookingQueryRepository; // ✅ 추가
import com.example.hotelres.tosspayment.dto.TossConfirmResponse;
import com.example.hotelres.tosspayment.dto.PaymentConfirmResponse;
import com.example.hotelres.user.UserRepository;
import com.example.hotelres.user.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;

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

    private final EmailService emailService;
    private final UserRepository userRepository;
    private final MyBookingQueryRepository myBookingQueryRepository; // ✅ 추가

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public PaymentConfirmResponse confirmToss(String paymentKey, String orderId, long amount, String holdCode) {
        // 멱등
        var existing = paymentRepository.findByProviderRef(paymentKey);
        if (existing.isPresent()) {
            var p = existing.get();
            String receiptUrl0 = null;
            try {
                var node = objectMapper.readTree(Optional.ofNullable(p.getRawPayload()).orElse("{}"));
                if (node.has("receipt") && node.get("receipt").has("url")) {
                    receiptUrl0 = node.get("receipt").get("url").asText(null);
                }
            } catch (Exception ignore) {}
            return new PaymentConfirmResponse(p.getBookingId(), p.getId(), receiptUrl0);
        }

        log.info(">>> ConfirmToss req: paymentKey={}, orderId={}, reqAmount={}, holdCode={}", paymentKey, orderId, amount, holdCode);

        // 1) 홀드 조회
        BookingHold hold = bookingHoldRepository.findByHoldCode(holdCode);
        if (hold == null) throw new ApiException("유효하지 않은 holdCode");

        Integer expectedAmount = hold.getTotalAmount();
        if (expectedAmount == null) throw new ApiException("홀드 총액이 비어 있습니다.");
        if (hold.getExpiresAt() != null && hold.getExpiresAt().isBefore(java.time.LocalDateTime.now())) {
            throw new ApiException("홀드가 만료되었습니다.");
        }

        // 2) Toss 승인
        TossConfirmResponse res = tossPaymentService.confirm(paymentKey, orderId, expectedAmount);
        if (!Objects.equals(expectedAmount, res.totalAmount())) {
            throw new ApiException("결제 금액 불일치(hold=" + expectedAmount + ", paid=" + res.totalAmount() + ")");
        }

        // 3) 예약 확정
        int nights = 1;
        if (hold.getCheckIn() != null && hold.getCheckOut() != null) {
            long d = ChronoUnit.DAYS.between(hold.getCheckIn(), hold.getCheckOut());
            nights = (int) Math.max(1, d);
        }

        BookingEntity booking = new BookingEntity();
        booking.setUserId(hold.getUserId());
        booking.setHotelId(hold.getHotelId());
        booking.setCheckIn(hold.getCheckIn());
        booking.setCheckOut(hold.getCheckOut());
        booking.setNights(nights);
        booking.setGuests(hold.getGuests());
        booking.setTotalAmount(res.totalAmount());
        booking.setCurrency(hold.getCurrency());
        booking.setVoucherNo(orderId);
        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);

        // 4) 라인아이템
        BookingItemEntity item = BookingItemEntity.builder()
                .booking(booking)
                .roomTypeId(hold.getRoomTypeId())
                .ratePlanId(hold.getRatePlanId())
                .quantity(nights)
                .priceTotal(hold.getTotalAmount())
                .build();
        bookingItemRepository.save(item);

        // 5) 결제 저장
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

        // 6) 쿠폰 사용 처리
        if (hold.getCouponCode() != null && !hold.getCouponCode().isBlank()) {
            var now = java.time.LocalDateTime.now();
            var today = now.toLocalDate();
            couponIssuanceRepository
                    .findAvailableByUserAndCode(hold.getUserId(), hold.getCouponCode(), today, now)
                    .ifPresent(ci -> {
                        ci.setStatus(CouponIssuanceStatus.USED);
                        ci.setUsedBookingId(booking.getId());
                        couponIssuanceRepository.save(ci);
                    });
        }

        // 7) 홀드 삭제
        bookingHoldRepository.delete(hold);

        // 8) 영수증 URL (프론트 응답용 — 메일에는 사용 안 함)
        String receiptUrl = null;
        if (res.receipt() != null) {
            Object urlObj = res.receipt().get("url");
            if (urlObj != null) receiptUrl = String.valueOf(urlObj);
        }

        // 9) ✅ 메일 발송 데이터: 쿼리 레포에서 호텔/객실명 한 번에 가져오기
        try {
            String toEmail = null;
            String customerName = null;

            if (hold.getUserId() != null) {
                Optional<User> uopt = userRepository.findById(hold.getUserId());
                if (uopt.isPresent()) {
                    User u = uopt.get();
                    toEmail = u.getEmail();
                    customerName = u.getName();
                }
            }

            if (toEmail != null && !toEmail.isBlank()) {
                // 단건 요약 가져오기 (hotelName, roomTypeName 포함)
                var rowOpt = myBookingQueryRepository.findMyBooking(hold.getUserId(), booking.getId());
                String hotelName = rowOpt.map(MyBookingQueryRepository.MyBookingRow::getHotelName).orElse("");
                String roomTypeName = rowOpt.map(MyBookingQueryRepository.MyBookingRow::getRoomTypeName).orElse("");

                emailService.sendBookingConfirmation(new EmailService.BookingMailPayload(
                        toEmail,
                        customerName,
                        booking.getId(),
                        hotelName,
                        roomTypeName,
                        booking.getCheckIn(),
                        booking.getCheckOut(),
                        booking.getGuests(),
                        String.format("%,d원", res.totalAmount())
                ));
            } else {
                log.info("예약확인 메일 스킵: 사용자 이메일을 찾지 못함 (userId={})", hold.getUserId());
            }
        } catch (Exception mailEx) {
            log.warn("예약확인 메일 전송 실패: {}", mailEx.toString());
        }

        // 10) 응답
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
