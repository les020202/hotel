// src/main/java/com/example/hotelres/admin/dto/RefundRateRow.java
package com.example.hotelres.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundRateRow {
    private Long   hotelId;
    private String hotelName;
    private String region;

    private int    bookings;    // 기간 내 예약건수
    private int    refunds;     // 기간 내 환불건수
    private double refundRate;  // refunds / bookings (0~1)
}
