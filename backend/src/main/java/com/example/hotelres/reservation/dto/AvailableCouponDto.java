// path: backend/src/main/java/com/example/hotelres/reservation/dto/AvailableCouponDto.java
package com.example.hotelres.reservation.dto;

import java.time.LocalDateTime;

public record AvailableCouponDto(
        String code,
        String title,
        Integer amount,
        LocalDateTime expiresAt // null 가능
) {}
