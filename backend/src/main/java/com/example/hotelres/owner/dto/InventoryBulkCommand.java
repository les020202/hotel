package com.example.hotelres.owner.dto;

import java.time.LocalDate;
import java.util.Set;

public record InventoryBulkCommand(
        Long roomTypeId,
        LocalDate from,
        LocalDate to,
        Set<Integer> weekdays,   // 1=Mon ... 7=Sun (또는 비우면 전체)
        Integer allotment,       // null이면 변경 안 함
        Integer price,           // null이면 변경 안 함
        String status            // null이면 변경 안 함 (OPEN/CLOSED/SOLD_OUT)
) {}
