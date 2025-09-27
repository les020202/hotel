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

        // ▼ 취소 메타
        String    canceledAt,   // "YYYY-MM-DDTHH:mm:ss"
        String    canceledBy,   // USER / OWNER / ADMIN
        String    cancelReason,

        // ▼ 대표 투숙객
        String    guestName,
        String    guestPhone
) {}
