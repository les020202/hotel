// src/main/java/com/example/hotelres/settlement/dto/HotelSummaryDTO.java
package com.example.hotelres.settlement.dto;

public record HotelSummaryDTO(
    long hotelId,
    String hotelName,
    long bookingCount,
    long grossSum,
    long discountSum,
    long feeSum,
    long netSum
) {}

