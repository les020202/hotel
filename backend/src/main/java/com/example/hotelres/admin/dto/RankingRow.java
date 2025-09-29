// src/main/java/com/example/hotelres/admin/dto/RankingRow.java
package com.example.hotelres.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RankingRow {
    private Long   hotelId;
    private String hotelName;
    private String region;

    // 매출 랭킹에선 예약건수/증감 모두 채우고,
    // 속도 랭킹(velocity)에서는 bookings=null 로 넘기는 구조
    private Integer bookings;  // nullable
    private Long   revenue;    // 기간 총매출
    private Double delta;      // 전기간 대비 증감율(-1.0 ~ +∞)
}
