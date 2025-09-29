package com.example.hotelres.owner.occupancy;

public record RoomTypeOccupancyRow(
    Long roomTypeId,
    String roomTypeName,
    Long bookedNights,
    Long allotmentNights,
    Double occupancyRate // 0~100
) {}
