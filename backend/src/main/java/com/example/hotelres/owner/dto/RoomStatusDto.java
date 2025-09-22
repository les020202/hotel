package com.example.hotelres.owner.dto;

import com.example.hotelres.owner.RoomStatus;
import com.example.hotelres.owner.HousekeepingStatus;

public record RoomStatusDto(
        Long id,
        String roomNo,                 // ← Integer roomNumber → String roomNo 로 변경
        Integer floor,
        Long roomTypeId,
        String roomTypeCode,
        String roomTypeName,
        Integer capacity,
        RoomStatus status,             // enum 그대로
        HousekeepingStatus housekeeping, // hkStatus → housekeeping 로 변경 (이름은 상관없지만 의미 맞춤)
        boolean occupied
) {}
