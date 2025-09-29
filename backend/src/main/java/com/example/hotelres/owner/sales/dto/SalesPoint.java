// src/main/java/com/example/hotelres/owner/sales/dto/SalesPoint.java
package com.example.hotelres.owner.sales.dto;

public record SalesPoint(
    String label,   // x축 라벨 (날짜/주/월)
    long amount     // 매출(원) - GMV 등
) {}
