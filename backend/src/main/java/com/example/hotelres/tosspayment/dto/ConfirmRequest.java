package com.example.hotelres.tosspayment.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ConfirmRequest {
    private String paymentKey;
    private String orderId;   // = bookings.voucher_no 로 사용할 값
    private int amount;
    private String holdCode;  // 결제 직전 홀드 코드(BookingHold 조회용)
}
