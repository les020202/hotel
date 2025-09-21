package com.example.hotelres.owner.dto;

public record AvailableRoomDto(
        Long roomId, String roomNo, Integer floor,
        String typeCode, Integer capacity, String housekeeping
) {}
