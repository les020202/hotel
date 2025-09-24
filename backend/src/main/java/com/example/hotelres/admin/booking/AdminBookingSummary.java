// src/main/java/com/example/hotelres/admin/booking/AdminBookingSummary.java
package com.example.hotelres.admin.booking;

import java.time.LocalDate;

public record AdminBookingSummary(
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
        Long userId,
        String userLoginId,
        String userName,
        String receiptUrl
) {}
