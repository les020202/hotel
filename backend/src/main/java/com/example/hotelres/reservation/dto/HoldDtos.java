// path: src/main/java/com/example/hotelres/reservation/dto/HoldDtos.java
package com.example.hotelres.reservation.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class HoldDtos {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CreateHoldReq {
        @NotNull private Long userId;
        @NotNull private Long hotelId;
        @NotNull private Long roomTypeId;
        @NotNull private Long ratePlanId;
        @NotNull private LocalDate checkIn;   // [checkIn, checkOut)
        @NotNull private LocalDate checkOut;  // checkout 당일 제외
        @Min(1) private int guests = 1;

        private String couponCode;  // 옵션
        private String guestName;   // 옵션(프론트 표시용)
        private String phone;       // 옵션(프론트 표시용)
    }

    @Getter @AllArgsConstructor
    public static class HoldRes {
        private final String holdCode;
        private final LocalDateTime expiresAt;
        private final int totalAmount;
    }
}
