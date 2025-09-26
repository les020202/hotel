// src/main/java/com/example/hotelres/reservation/dto/MyBookingSummary.java
package com.example.hotelres.reservation.dto;

import java.time.LocalDate;

public record MyBookingSummary(
        Long      bookingId,
        String    status,
        String    hotelName,
        String    roomTypeName,
        LocalDate checkIn,
        LocalDate checkOut,
        Integer   nights,
        Integer   guests,
        Integer   totalAmount,
        String    currency,
        String    receiptUrl,

        // ▼ 추가: 취소 메타
        String    canceledAt,   // ISO-like 문자열 "YYYY-MM-DDTHH:mm:ss" (프론트에서 replace로 표시)
        String    canceledBy,   // USER / OWNER / ADMIN
        String    cancelReason
) {}
