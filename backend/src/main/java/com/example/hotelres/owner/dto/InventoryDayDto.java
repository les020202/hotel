package com.example.hotelres.owner.dto;

import java.time.LocalDate;

public record InventoryDayDto(
        LocalDate stayDate,
        int allotment,
        int booked,
        int price,
        String status,      // OPEN/CLOSED/SOLD_OUT
        int remainingQty,   // DB 생성컬럼 매핑 (read-only)
        boolean sellable    // DB 생성컬럼 매핑 (read-only)
) {}
