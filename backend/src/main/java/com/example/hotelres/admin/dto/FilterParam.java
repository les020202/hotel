// src/main/java/com/example/hotelres/admin/dto/FilterParam.java
package com.example.hotelres.admin.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class FilterParam {
    private String region;     // 예: "서울특별시" (없으면 null/빈문자)
    private String hotel;      // 호텔명 또는 호텔ID 문자열
    private LocalDate from;    // YYYY-MM-DD
    private LocalDate to;      // YYYY-MM-DD
}
