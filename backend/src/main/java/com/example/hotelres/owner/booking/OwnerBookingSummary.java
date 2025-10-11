// src/main/java/com/example/hotelres/owner/booking/OwnerBookingSummary.java
package com.example.hotelres.owner.booking;

import java.time.LocalDate;

public record OwnerBookingSummary(
        Long bookingId,
        String status,
        String userLoginId,
        String userName,
        String roomTypeName,
        LocalDate checkIn,
        Integer nights,
        Integer guests,
        Integer totalAmount,
        String currency
) {}
