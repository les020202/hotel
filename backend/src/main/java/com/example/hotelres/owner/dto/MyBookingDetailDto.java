// src/main/java/com/example/hotelres/owner/dto/MyBookingDetailDto.java
package com.example.hotelres.owner.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MyBookingDetailDto {
    private Long   bookingId;
    private String status;

    private Long   hotelId;
    private String hotelName;
    private String roomTypeName;

    private String checkIn;   // ISO string
    private String checkOut;
    private Integer nights;
    private Integer guests;

    private Integer totalAmount;
    private String  currency;

    // ✅ 취소 메타
    private String  canceledAt;   // 2025-09-25T20:08:11
    private String  canceledBy;   // USER/OWNER/ADMIN
    private String  cancelReason;
}
