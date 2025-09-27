// src/main/java/com/example/hotelres/reservation/dto/MyBookingSummary.java
package com.example.hotelres.reservation.dto;

import java.time.LocalDate;

public record MyBookingSummary(
        Long bookingId,
        String status,
        String hotelName,
        String roomTypeName,
        LocalDate checkIn,
        LocalDate checkOut,
        Integer nights,
        Integer guests,
        Integer totalAmount,
        String currency,
        String receiptUrl,
        String guestName,
        String guestPhone
) {}
