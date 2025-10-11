package com.example.hotelres.admin.dto;

import lombok.*;
import java.util.List;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class TrendDto {
    private List<String>  labels;
    private List<Long>    revenue;   // 서비스에서 .revenue(...) 호출
    private List<Integer> bookings;
}
