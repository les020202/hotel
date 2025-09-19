// src/main/java/com/example/tosspayment/dto/PaymentConfirmResponse.java
package com.example.hotelres.tosspayment.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentConfirmResponse {

    private PaymentDto payment;   // ✅ 이 필드명이 있어야 .payment(...)가 생김
    private BookingDto booking;   // ✅ 이 필드명이 있어야 .booking(...)가 생김

    @Getter @Builder
    public static class PaymentDto {
        private int amount;
        private String orderId;
        private String methodLabel;
        private String approvedAtDisplay;
        private String receiptUrl;
    }

    @Getter @Builder
    public static class BookingDto {
        private Long id;
        private String checkIn;   // 문자열로 내려주면 편함(프론트 포맷 자유)
        private String checkOut;
        private int nights;
        private int guests;       // 지금 스키마는 guests 하나만 존재
        private String policy;    // 있으면 채우고 없으면 임시 문자열
    }
}
