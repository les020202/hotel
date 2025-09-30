package com.example.hotelres.settlement;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor      // 기본 생성자
@AllArgsConstructor     // 모든 필드 생성자
public class WeeklySettlementDTO {
    private Long hotelId;     // 호텔 ID
    private Long sumSettled;  // 지난주 정산 금액
}
