package com.example.hotelres.owner;

import java.time.LocalDate;

public record OwnerBookingDto(
        Long id, LocalDate checkIn, LocalDate checkOut,
        int nights, int guests, int totalAmount, String status, String voucherNo
) {}
