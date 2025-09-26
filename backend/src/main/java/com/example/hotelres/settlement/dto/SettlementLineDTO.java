// src/main/java/com/example/hotelres/settlement/dto/SettlementLineDTO.java
package com.example.hotelres.settlement.dto;

import lombok.*;
import java.time.LocalDate; // (더이상 안써도 됨, 쓰면 제거해도 무방)

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SettlementLineDTO {
    private long hotelId;
    private String hotelName;
    private long roomTypeId;
    private String roomTypeName;

    private long bookingCount;       // DISTINCT booking 수
    private long gross;              // 총액 합
    private long discount;           // 할인 합(균등배분)
    private long fee;                // 수수료 합(15%)
    private long net;                // 순지급 합
}
