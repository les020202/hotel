// src/main/java/com/example/hotelres/owner/sales/dto/SalesSeriesResponse.java
package com.example.hotelres.owner.sales.dto;

import java.time.LocalDate;
import java.util.List;

public record SalesSeriesResponse(
    String mode,            // day | week | month
    LocalDate start,        // 집계 시작
    LocalDate end,          // 집계 끝(포함/프론트 표시에만, 쿼리는 보통 [start, end) 권장)
    List<SalesPoint> items  // 시계열 데이터
) {}
