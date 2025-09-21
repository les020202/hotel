package com.example.hotelres.owner.dto;

import java.time.LocalDate;

public record ArrivalItemDto(
        Long bookingId, Long bookingItemId,
        String guestName, Integer guests,
        Long roomTypeId, String typeCode, String roomTypeName,
        LocalDate checkIn, LocalDate checkOut,
        int nights,
        boolean assigned
) {}
